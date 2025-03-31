package launcher.request.update;

import launcher.Launcher;
import launcher.LauncherAPI;
import launcher.hasher.FileNameMatcher;
import launcher.hasher.HashedDir;
import launcher.hasher.HashedEntry;
import launcher.hasher.HashedFile;
import launcher.helper.IOHelper;
import launcher.helper.SecurityHelper;
import launcher.request.Request;
import launcher.serialize.HInput;
import launcher.serialize.HOutput;
import launcher.serialize.signed.SignedObjectHolder;
import launcher.serialize.stream.EnumSerializer;
import launcher.serialize.stream.StreamObject;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.security.MessageDigest;
import java.security.SignatureException;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.zip.InflaterInputStream;

public final class UpdateRequest
        extends Request {
    @LauncherAPI
    public static final int MAX_QUEUE_SIZE = 128;
    private final String dirName;
    private final Path dir;
    private final FileNameMatcher matcher;
    private final boolean digest;
    private volatile State.Callback stateCallback;
    private HashedDir localDir;
    private long totalDownloaded;
    private long totalSize;
    private Instant startTime;

    @LauncherAPI
    public UpdateRequest(Launcher.Config config, String string, Path path, FileNameMatcher fileNameMatcher, boolean bl) {
        super(config);
        this.dirName = IOHelper.verifyFileName(string);
        this.dir = Objects.requireNonNull(path, "dir");
        this.matcher = fileNameMatcher;
        this.digest = bl;
    }

    @LauncherAPI
    public UpdateRequest(String string, Path path, FileNameMatcher fileNameMatcher, boolean bl) {
        this(null, string, path, fileNameMatcher, bl);
    }

    private static void fillActionsQueue(Queue<Action> queue, HashedDir hashedDir) {
        for (Map.Entry<String, HashedEntry> entry : hashedDir.map().entrySet()) {
            String string = (String)entry.getKey();
            HashedEntry hashedEntry = (HashedEntry)entry.getValue();
            HashedEntry.Type cOm4 = hashedEntry.getType();
            switch (cOm4) {
                case DIR: {
                    queue.add(new Action(Action.Type.CD, string, hashedEntry));
                    UpdateRequest.fillActionsQueue(queue, (HashedDir)hashedEntry);
                    queue.add(Action.CD_BACK);
                   break;
                }
                case FILE: {
                    queue.add(new Action(Action.Type.GET, string, hashedEntry));
                    break;
                }
                default:
                    throw new AssertionError((Object)("Unsupported hashed entry type: " + cOm4.name()));
            }
        }
    }

    @Override
    public Type getType() {
        return Type.UPDATE;
    }

    @Override
    public SignedObjectHolder<HashedDir> request() throws Throwable {
        Files.createDirectories(this.dir);
        this.localDir = new HashedDir(this.dir, this.matcher, false, this.digest);
        return (SignedObjectHolder)super.request();
    }

    protected SignedObjectHolder requestDo(HInput hInput, HOutput hOutput) throws IOException, SignatureException {
        hOutput.writeString(this.dirName, 255);
        hOutput.flush();
        this.readError(hInput);
        SignedObjectHolder signedObjectHolder = new SignedObjectHolder(hInput, this.config.publicKey, HashedDir::new);
        HashedDir.Diff diff = ((HashedDir)signedObjectHolder.object).diff(this.localDir, this.matcher);
        this.totalSize  = diff.mismatch.size();
        boolean bl = hInput.readBoolean();
        LinkedList<Action> linkedList = new LinkedList<Action>();
        UpdateRequest.fillActionsQueue(linkedList, diff.mismatch);
        linkedList.add(Action.FINISH);
        InputStream inputStream = bl ? new InflaterInputStream(hInput.stream, IOHelper.newInflater(), IOHelper.BUFFER_SIZE) : hInput.stream;
        this.startTime = Instant.now();
        Path path = this.dir;
        Action[] actionArray = new Action[128];
        while (!linkedList.isEmpty()) {
            Action action;
            int n;
            int n2 = Math.min(linkedList.size(), 128);
            hOutput.writeLength(n2, 128);
            for (n = 0; n < n2; ++n) {
                actionArray[n] = action = (Action)linkedList.remove();
                action.write(hOutput);
            }
            hOutput.flush();
            block8: for (n = 0; n < n2; ++n) {
                action = actionArray[n];
                switch (action.type) {
                    case CD: {
                        path = path.resolve(action.name);
                        Files.createDirectories(path, new FileAttribute[0]);
                        continue block8;
                    }
                    case GET: {
                        Path path2 = path.resolve(action.name);
                        if (inputStream.read() != 255) {
                            throw new IOException("Serverside cached size mismath for file " + action.name);
                        }
                        this.downloadFile(path2, (HashedFile)action.entry, inputStream);
                        continue block8;
                    }
                    case CD_BACK: {
                        path = path.getParent();
                        continue block8;
                    }
                    case FINISH: {
                        continue block8;
                    }
                    default: {
                        throw new AssertionError((Object)String.format("Unsupported action type: '%s'", action.type.name()));
                    }
                }
            }
        }
        this.deleteExtraDir(this.dir, diff.extra, diff.extra.flag);
        return signedObjectHolder;
    }

    @LauncherAPI
    public void setStateCallback(State.Callback callback) {
        this.stateCallback = callback;
    }

    private void deleteExtraDir(Path path, HashedDir hashedDir, boolean bl) throws IOException {
        block4: for (Map.Entry entry : hashedDir.map().entrySet()) {
            String string = (String)entry.getKey();
            Path path2 = path.resolve(string);
            HashedEntry hashedEntry = (HashedEntry)entry.getValue();
            HashedEntry.Type cOm4 = hashedEntry.getType();
            switch (cOm4) {
                case FILE: {
                    this.updateState(IOHelper.toString(path2), 0L, 0L);
                    Files.delete(path2);
                    continue block4;
                }
                case DIR: {
                    this.deleteExtraDir(path2, (HashedDir)hashedEntry, bl || hashedEntry.flag);
                    continue block4;
                }
            }
            throw new AssertionError((Object)("Unsupported hashed entry type: " + cOm4.name()));
        }
        if (bl) {
            this.updateState(IOHelper.toString(path), 0L, 0L);
            Files.delete(path);
        }
    }

    private void downloadFile(Path path, HashedFile hashedFile, InputStream inputStream) throws IOException {
        String string = IOHelper.toString(this.dir.relativize(path));
        this.updateState(string, 0L, hashedFile.size);
        MessageDigest messageDigest = this.digest ? SecurityHelper.newDigest(SecurityHelper.DigestAlgorithm.MD5) : null;
        try (OutputStream  object = IOHelper.newBufferedOutStream(IOHelper.newOutput(path));){
            long l = 0L;
            byte[] byArray = IOHelper.newBuffer();
            while (l < hashedFile.size) {
                int n = (int)Math.min(hashedFile.size - l, (long)byArray.length);
                int n2 = inputStream.read(byArray, 0, n);
                if (n2 < 0) {
                    throw new EOFException(String.format("%d bytes remaining", hashedFile.size - l));
                }
                ((OutputStream)object).write(byArray, 0, n2);
                if (messageDigest != null) {
                    messageDigest.update(byArray, 0, n2);
                }
                this.totalDownloaded  += (long)n2;
                this.updateState(string, l += (long)n2, hashedFile.size);
            }
            ((OutputStream)object).flush();
        }
        if (messageDigest != null) {
            byte[] digestBytes = messageDigest.digest();
            if (!hashedFile.isSameDigest(digestBytes)) {
                throw new SecurityException(String.format("File digest mismatch: '%s'", string));
            }
        }

    }

    private void updateState(String string, long l, long l2) {
        if (this.stateCallback  != null) {
            this.stateCallback.call(new State(string, l, l2, this.totalDownloaded, this.totalSize, Duration.between(this.startTime, Instant.now())));
        }
    }

    public static final class Action
            extends StreamObject {
        public static final Action CD_BACK = new Action(Type.CD_BACK, null, null);
        public static final Action FINISH = new Action(Type.FINISH, null, null);
        public final Type type;
        public final String name;
        public final HashedEntry entry;

        public Action(Type type, String string, HashedEntry hashedEntry) {
            this.type = type;
            this.name = string;
            this.entry = hashedEntry;
        }

        public Action(HInput hInput) throws IOException {
            this.type = Type.read(hInput);
            this.name = this.type == Type.CD || this.type == Type.GET ? IOHelper.verifyFileName(hInput.readString(255)) : null;
            this.entry = null;
        }

        @Override
        public void write(HOutput hOutput) throws IOException {
            EnumSerializer.write(hOutput, this.type);
            if (this.type == Type.CD || this.type == Type.GET) {
                hOutput.writeString(this.name, 255);
            }
        }

        public static enum Type implements EnumSerializer.Itf
        {
            CD(1),
            CD_BACK(2),
            GET(3),
            FINISH(255);

            private static final EnumSerializer<Type> SERIALIZER;
            private final int n;

            private Type(int n2) {
                this.n = n2;
            }

            @Override
            public int getNumber() {
                return this.n;
            }

            public static Type read(HInput input) throws IOException {
                return SERIALIZER.read(input);
            }

            static {
                SERIALIZER = new EnumSerializer<Type>(Type.class);
            }
        }
    }

    public static final class State {
        @LauncherAPI
        public final long fileDownloaded;
        @LauncherAPI
        public final long fileSize;
        @LauncherAPI
        public final long totalDownloaded;
        @LauncherAPI
        public final long totalSize;
        @LauncherAPI
        public final String filePath;
        @LauncherAPI
        public final Duration duration;

        public State(String string, long l, long l2, long l3, long l4, Duration duration) {
            this.filePath = string;
            this.fileDownloaded = l;
            this.fileSize = l2;
            this.totalDownloaded = l3;
            this.totalSize = l4;
            this.duration = duration;
        }

        @LauncherAPI
        public double getBps() {
            long l = this.duration.getSeconds();
            return l == 0L ? -1.0 : (double)this.totalDownloaded / (double)l;
        }

        @LauncherAPI
        public Duration getEstimatedTime() {
            double d = this.getBps();
            return d <= 0.0 ? null : Duration.ofSeconds((long)((double)this.getTotalRemaining() / d));
        }

        @LauncherAPI
        public double getFileDownloadedKiB() {
            return (double)this.fileDownloaded / 1024.0;
        }

        @LauncherAPI
        public double getFileDownloadedMiB() {
            return this.getFileDownloadedKiB() / 1024.0;
        }

        @LauncherAPI
        public double getFileDownloadedPart() {
            return this.fileSize == 0L ? 0.0 : (double)this.fileDownloaded / (double)this.fileSize;
        }

        @LauncherAPI
        public long getFileRemaining() {
            return this.fileSize - this.fileDownloaded;
        }

        @LauncherAPI
        public double getFileRemainingKiB() {
            return (double)this.getFileRemaining() / 1024.0;
        }

        @LauncherAPI
        public double getFileRemainingMiB() {
            return this.getFileRemainingKiB() / 1024.0;
        }

        @LauncherAPI
        public double getFileSizeKiB() {
            return (double)this.fileSize / 1024.0;
        }

        @LauncherAPI
        public double getFileSizeMiB() {
            return this.getFileSizeKiB() / 1024.0;
        }

        @LauncherAPI
        public double getTotalDownloadedKiB() {
            return (double)this.totalDownloaded / 1024.0;
        }

        @LauncherAPI
        public double getTotalDownloadedMiB() {
            return this.getTotalDownloadedKiB() / 1024.0;
        }

        @LauncherAPI
        public double getTotalDownloadedPart() {
            return this.totalSize == 0L ? 0.0 : (double)this.totalDownloaded / (double)this.totalSize;
        }

        @LauncherAPI
        public long getTotalRemaining() {
            return this.totalSize - this.totalDownloaded;
        }

        @LauncherAPI
        public double getTotalRemainingKiB() {
            return (double)this.getTotalRemaining() / 1024.0;
        }

        @LauncherAPI
        public double getTotalRemainingMiB() {
            return this.getTotalRemainingKiB() / 1024.0;
        }

        @LauncherAPI
        public double getTotalSizeKiB() {
            return (double)this.totalSize / 1024.0;
        }

        @LauncherAPI
        public double getTotalSizeMiB() {
            return this.getTotalSizeKiB() / 1024.0;
        }
        @FunctionalInterface
        public interface Callback {
            void call(State state);
        }
    }
}
