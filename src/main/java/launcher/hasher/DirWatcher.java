package launcher.hasher;

import com.sun.nio.file.ExtendedWatchEventModifier;
import com.sun.nio.file.SensitivityWatchEventModifier;
import launcher.LauncherAPI;
import launcher.helper.IOHelper;
import launcher.helper.JVMHelper;
import launcher.helper.LogHelper;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Objects;

public final class DirWatcher
        implements AutoCloseable,
        Runnable {
    private static final boolean FILE_TREE_SUPPORTED = JVMHelper.OS_TYPE == JVMHelper.OS.MUSTDIE;
    private static final WatchEvent.Modifier[] MODIFIERS = new WatchEvent.Modifier[]{SensitivityWatchEventModifier.HIGH};
    private static final WatchEvent.Modifier[] FILE_TREE_MODIFIERS = new WatchEvent.Modifier[]{ExtendedWatchEventModifier.FILE_TREE, SensitivityWatchEventModifier.HIGH};
    private static final WatchEvent.Kind[] KINDS = new WatchEvent.Kind[]{StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE};
    private final Path dir;
    private final HashedDir hdir;
    private final FileNameMatcher matcher;
    private final WatchService service;
    private final boolean digest;

    @LauncherAPI
    public DirWatcher(Path path, HashedDir hashedDir, FileNameMatcher fileNameMatcher, boolean bl) throws IOException {
        this.dir = Objects.requireNonNull(path, "dir");
        this.hdir = Objects.requireNonNull(hashedDir, "hdir");
        this.matcher = fileNameMatcher;
        this.digest = bl;
        this.service = path.getFileSystem().newWatchService();
        if (FILE_TREE_SUPPORTED) {
            path.register(this.service, KINDS, FILE_TREE_MODIFIERS);
            return;
        }
        IOHelper.walk(path, new RegisterFileVisitor(), true);
    }

    private static void handleError(Throwable throwable) {
        LogHelper.error(throwable);
        JVMHelper.halt0(195952353);
    }

    private static Deque toPath(Iterable<Path> iterable) {
        LinkedList<String> linkedList = new LinkedList<String>();
        for (Path path : iterable) {
            linkedList.add(path.toString());
        }
        return linkedList;
    }

    @Override
    @LauncherAPI
    public void close() throws IOException {
        this.service.close();
    }

    @Override
    @LauncherAPI
    public void run() {
        try {
            this.processLoop();
        }
        catch (InterruptedException | ClosedWatchServiceException exception) {
        }
        catch (Throwable throwable) {
            DirWatcher.handleError(throwable);
        }
    }

    private void processKey(WatchKey watchKey) throws IOException {
        Path path = (Path)watchKey.watchable();
        for (WatchEvent<?> watchEvent : watchKey.pollEvents()) {
            HashedEntry HashedEntry2;
            WatchEvent.Kind<?> kind = watchEvent.kind();
            if (kind.equals(StandardWatchEventKinds.OVERFLOW)) {
                if (Boolean.getBoolean("launcher.dirwatcher.ignoreOverflows")) continue;
                throw new IOException("Overflow");
            }
            Path path2 = path.resolve((Path)watchEvent.context());
            Deque deque = DirWatcher.toPath(this.dir.relativize(path2));
            if (this.matcher != null && !this.matcher.shouldVerify(deque) || kind.equals(StandardWatchEventKinds.ENTRY_MODIFY) && (HashedEntry2 = this.hdir.resolve(deque)) != null && (HashedEntry2.getType() != HashedEntry.Type.FILE || ((HashedFile)HashedEntry2).isSame(path2, this.digest))) continue;
            throw new SecurityException(String.format("Forbidden modification (%s, %d times): '%s'", kind, watchEvent.count(), path2));
        }
        watchKey.reset();
    }

    private void processLoop() throws IOException, InterruptedException {
        while (!Thread.interrupted()) {
            this.processKey(this.service.take());
        }
    }

    private final class RegisterFileVisitor
            extends SimpleFileVisitor<Path> {
        private final Deque<String> path = new LinkedList<String>();

        private RegisterFileVisitor() {
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            FileVisitResult result = super.postVisitDirectory(dir, exc);
            if (!DirWatcher.this.dir.equals(dir)) {
                this.path.removeLast();
            }
            return result;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            FileVisitResult result = super.preVisitDirectory(dir, attrs);
            if (DirWatcher.this.dir.equals(dir)) {
                dir.register(DirWatcher.this.service, KINDS, MODIFIERS);
                return result;
            }
            this.path.add(IOHelper.getFileName(dir));
            if (DirWatcher.this.matcher != null && !DirWatcher.this.matcher.shouldVerify(this.path)) {
                return FileVisitResult.SKIP_SUBTREE;
            }
            dir.register(DirWatcher.this.service, KINDS, MODIFIERS);
            return result;
        }
    }
}

