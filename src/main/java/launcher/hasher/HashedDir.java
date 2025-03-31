package launcher.hasher;

import launcher.LauncherAPI;
import launcher.helper.IOHelper;
import launcher.helper.JVMHelper;
import launcher.helper.VerifyHelper;
import launcher.serialize.HInput;
import launcher.serialize.HOutput;
import launcher.serialize.stream.EnumSerializer;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public final class HashedDir
        extends HashedEntry {
    private final Map<String, HashedEntry> map = new HashMap<String, HashedEntry>(32);

    @LauncherAPI
    public HashedDir() {
    }

    @LauncherAPI
    public HashedDir(Path path, FileNameMatcher fileNameMatcher, boolean bl, boolean bl2) throws IOException {
        IOHelper.walk(path, new HashFileVisitor(this, path, fileNameMatcher, bl, bl2), true);
    }

    @LauncherAPI
    public HashedDir(HInput hInput) throws IOException {
        int n = hInput.readLength(0);
        for (int i = 0; i < n; ++i) {
            HashedEntry hashedEntry;
            String string = IOHelper.verifyFileName(hInput.readString(255));
            Type cOm4 = Type.read(hInput);
            switch (cOm4) {
                case FILE: {
                    hashedEntry = new HashedFile(hInput);
                    break;
                }
                case DIR: {
                    hashedEntry = new HashedDir(hInput);
                    break;
                }
                default: {
                    throw new AssertionError((Object)("Unsupported hashed entry type: " + cOm4.name()));
                }
            }
            VerifyHelper.putIfAbsent(this.map, string, hashedEntry, String.format("Duplicate dir entry: '%s'", string));
        }
    }

    @Override
    public Type getType() {
        return Type.DIR;
    }

    @Override
    public long size() {
        return this.map.values().stream().mapToLong(HashedEntry::size).sum();
    }

    @Override
    public void write(HOutput hOutput) throws IOException {
        Set<Map.Entry<String, HashedEntry>> set = this.map.entrySet();
        hOutput.writeLength(set.size(), 0);
        for (Map.Entry<String, HashedEntry> entry : set) {
            hOutput.writeString(entry.getKey(), 255);
            HashedEntry hashedEntry = entry.getValue();
            EnumSerializer.write(hOutput, hashedEntry.getType());
            hashedEntry.write(hOutput);
        }
    }

    @LauncherAPI
    public Diff diff(HashedDir hashedDir, FileNameMatcher fileNameMatcher) {
        HashedDir hashedDir2 = this.sideDiff(hashedDir, fileNameMatcher, new LinkedList<String>(), true);
        HashedDir hashedDir3 = hashedDir.sideDiff(this, fileNameMatcher, new LinkedList<String>(), false);
        return new Diff(hashedDir2, hashedDir3);
    }

    @LauncherAPI
    public HashedEntry getEntry(String string) {
        return this.map.get(string);
    }

    @LauncherAPI
    public boolean isEmpty() {
        return this.map.isEmpty();
    }

    @LauncherAPI
    public Map<String, HashedEntry> map() {
        return Collections.unmodifiableMap(this.map);
    }

    @LauncherAPI
    public HashedEntry resolve(Iterable<String> iterable) {
        HashedEntry hashedEntry = this;
        for (String string : iterable) {
            if (hashedEntry instanceof HashedDir) {
                hashedEntry = ((HashedDir)hashedEntry).map.get(string);
                continue;
            }
            return null;
        }
        return hashedEntry;
    }

    private HashedDir sideDiff(HashedDir hashedDir, FileNameMatcher fileNameMatcher, Deque<String> deque, boolean bl) {
        HashedDir hashedDir2 = new HashedDir();
        for (Map.Entry<String, HashedEntry> entry : this.map.entrySet()) {
            String string = entry.getKey();
            HashedEntry hashedEntry = entry.getValue();
            deque.add(string);
            boolean bl2 = fileNameMatcher == null || fileNameMatcher.shouldUpdate(deque);
            Type cOm4 = hashedEntry.getType();
            HashedEntry hashedEntry2 = hashedDir.map.get(string);
            if (hashedEntry2 == null || hashedEntry2.getType() != cOm4) {
                if (bl2 || bl && hashedEntry2 == null) {
                    hashedDir2.map.put(string, hashedEntry);
                    if (!bl) {
                        hashedEntry.flag = true;
                    }
                }
                deque.removeLast();
                continue;
            }
            switch (cOm4) {
                case FILE: {
                    HashedFile hashedFile = (HashedFile)hashedEntry;
                    HashedFile hashedFile2 = (HashedFile)hashedEntry2;
                    if (!bl || !bl2 || hashedFile.isSame(hashedFile2)) break;
                    hashedDir2.map.put(string, hashedEntry);
                    break;
                }
                case DIR: {
                    HashedDir hashedDir3;
                    HashedDir hashedDir4 = (HashedDir)hashedEntry;
                    HashedDir hashedDir5 = (HashedDir)hashedEntry2;
                    if (!bl && !bl2 || (hashedDir3 = hashedDir4.sideDiff(hashedDir5, fileNameMatcher, deque, bl)).isEmpty()) break;
                    hashedDir2.map.put(string, hashedDir3);
                    break;
                }
                default: {
                    throw new AssertionError((Object)("Unsupported hashed entry type: " + cOm4.name()));
                }
            }
            deque.removeLast();
        }
        return hashedDir2;
    }

    static Map aux(HashedDir hashedDir) {
        return hashedDir.map;
    }

    private final class HashFileVisitor
            extends SimpleFileVisitor<Path> {
        private final Path dir;
        private final FileNameMatcher matcher;
        private final boolean allowSymlinks;
        private final boolean digest;
        private final Deque<String> path = new LinkedList<String>();
        private final Deque<HashedDir> stack = new LinkedList<HashedDir>();
        private HashedDir current = HashedDir.this;

        private HashFileVisitor(HashedDir hashedDir2, Path path, FileNameMatcher fileNameMatcher, boolean bl, boolean bl2) {
            this.current = hashedDir2;
            this.dir = path;
            this.matcher = fileNameMatcher;
            this.allowSymlinks = bl;
            this.digest = bl2;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path path, IOException iOException) throws IOException {
            FileVisitResult fileVisitResult = super.postVisitDirectory(path, iOException);
            if (this.dir.equals(path)) {
                return fileVisitResult;
            }
            HashedDir hashedDir = this.stack.removeLast();
            HashedDir.aux(hashedDir).put(this.path.removeLast(), this.current);
            this.current = hashedDir;
            return fileVisitResult;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
            FileVisitResult fileVisitResult = super.preVisitDirectory(path, basicFileAttributes);
            if (this.dir.equals(path)) {
                return fileVisitResult;
            }
            this.aux(path, basicFileAttributes);
            this.stack.add(this.current);
            this.current = new HashedDir();
            this.path.add(IOHelper.getFileName(path));
            return fileVisitResult;
        }

        @Override
        public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
            this.aux(path, basicFileAttributes);
            this.path.add(IOHelper.getFileName(path));
            boolean bl = this.digest && (this.matcher == null || this.matcher.shouldUpdate(this.path));
            HashedDir.aux(this.current).put(this.path.removeLast(), new HashedFile(path, basicFileAttributes.size(), bl));
            return super.visitFile(path, basicFileAttributes);
        }

        private void aux(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
            if (!this.allowSymlinks && (JVMHelper.OS_TYPE == JVMHelper.OS.MUSTDIE && !path.toRealPath(new LinkOption[0]).equals(path) || JVMHelper.OS_TYPE != JVMHelper.OS.MUSTDIE && basicFileAttributes.isSymbolicLink())) {
                throw new SecurityException("Symlinks are not allowed");
            }
        }
    }

    public static final class Diff {
        @LauncherAPI
        public final HashedDir mismatch;
        @LauncherAPI
        public final HashedDir extra;

        private Diff(HashedDir hashedDir, HashedDir hashedDir2) {
            this.mismatch = hashedDir;
            this.extra = hashedDir2;
        }

        @LauncherAPI
        public boolean isSame() {
            return this.mismatch.isEmpty() && this.extra.isEmpty();
        }
    }
}
