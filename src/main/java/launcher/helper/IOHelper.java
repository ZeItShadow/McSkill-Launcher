package launcher.helper;

import launcher.Launcher;
import launcher.LauncherAPI;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.util.Collections;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class IOHelper {
    @LauncherAPI
    public static final Charset UNICODE_CHARSET = StandardCharsets.UTF_8;
    @LauncherAPI
    public static final Charset ASCII_CHARSET = StandardCharsets.US_ASCII;
    @LauncherAPI
    public static final int SOCKET_TIMEOUT = VerifyHelper.verifyInt(Integer.parseInt(System.getProperty("launcher.socketTimeout", Integer.toString(10000))), VerifyHelper.POSITIVE, "launcher.socketTimeout can't be <= 0");
    @LauncherAPI
    public static final int HTTP_TIMEOUT = VerifyHelper.verifyInt(Integer.parseInt(System.getProperty("launcher.httpTimeout", Integer.toString(5000))), VerifyHelper.POSITIVE, "launcher.httpTimeout can't be <= 0");
    @LauncherAPI
    public static final int BUFFER_SIZE = VerifyHelper.verifyInt(Integer.parseInt(System.getProperty("launcher.bufferSize", Integer.toString(4096))), VerifyHelper.POSITIVE, "launcher.bufferSize can't be <= 0");
    @LauncherAPI
    public static final String CROSS_SEPARATOR = "/";
    @LauncherAPI
    public static final FileSystem FS = FileSystems.getDefault();
    @LauncherAPI
    public static final String PLATFORM_SEPARATOR = FS.getSeparator();
    private static final Pattern PLATFORM_SEPARATOR_PATTERN = Pattern.compile(PLATFORM_SEPARATOR, 16);
    @LauncherAPI
    public static final boolean POSIX = FS.supportedFileAttributeViews().contains("posix");
    @LauncherAPI
    public static final Path JVM_DIR = Paths.get(System.getProperty("java.home"), new String[0]);
    @LauncherAPI
    public static final Path HOME_DIR = Paths.get(System.getProperty("user.home"), new String[0]);
    @LauncherAPI
    public static final Path HOME_DIR_WIN = Paths.get(System.getProperty("user.home") + "\\AppData\\Roaming", new String[0]);
    @LauncherAPI
    public static final Path WORKING_DIR = Paths.get(System.getProperty("user.dir"), new String[0]);
    private static final OpenOption[] READ_OPTIONS = new OpenOption[]{StandardOpenOption.READ};
    private static final OpenOption[] WRITE_OPTIONS = new OpenOption[]{StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING};
    private static final OpenOption[] APPEND_OPTIONS = new OpenOption[]{StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND};
    private static final LinkOption[] LINK_OPTIONS  = {};
    private static final CopyOption[] COPY_OPTIONS = new CopyOption[]{StandardCopyOption.REPLACE_EXISTING};
    private static final Set<FileVisitOption> WALK_OPTIONS = Collections.singleton(FileVisitOption.FOLLOW_LINKS);
    private static final Pattern CROSS_SEPARATOR_PATTERN  = Pattern.compile(CROSS_SEPARATOR, 16);

    private IOHelper() {
    }

    @LauncherAPI
    public static void close(AutoCloseable autoCloseable) {
        try {
            autoCloseable.close();
        }
        catch (Throwable throwable) {
            LogHelper.error(throwable);
        }
    }

    @LauncherAPI
    public static void copy(Path path, Path path2) throws IOException  {
        IOHelper.createParentDirs(path2);
        Files.copy(path, path2, COPY_OPTIONS);
    }

    @LauncherAPI
    public static void createParentDirs(Path path) throws IOException  {
        Path path2 = path.getParent();
        if (path2 != null && !IOHelper.isDir(path2)) {
            Files.createDirectories(path2);
        }
    }

    @LauncherAPI
    public static String decode(byte[] byArray) {
        return new String(byArray, UNICODE_CHARSET);
    }

    @LauncherAPI
    public static String decodeASCII(byte[] byArray) {
        return new String(byArray, ASCII_CHARSET);
    }

    @LauncherAPI
    public static void deleteDir(Path path, boolean bl) throws IOException {
        IOHelper.walk(path, new DeleteDirVisitor(path, bl), true);
    }

    @LauncherAPI
    public static byte[] encode(String string) {
        return string.getBytes(UNICODE_CHARSET);
    }

    @LauncherAPI
    public static byte[] encodeASCII(String string) {
        return string.getBytes(ASCII_CHARSET);
    }

    @LauncherAPI
    public static boolean exists(Path path) {
        return Files.exists(path, LINK_OPTIONS);
    }

    @LauncherAPI
    public static Path getCodeSource(Class clazz) {
        return Paths.get(IOHelper.toURI(clazz.getProtectionDomain().getCodeSource().getLocation()));
    }

    @LauncherAPI
    public static String getFileName(Path path) {
        return path.getFileName().toString();
    }

    @LauncherAPI
    public static String getIP(SocketAddress socketAddress) {
        return ((InetSocketAddress)socketAddress).getAddress().getHostAddress();
    }

    @LauncherAPI
    public static byte[] getResourceBytes(String string) throws IOException {
        return IOHelper.read(IOHelper.getResourceURL(string));
    }

    @LauncherAPI
    public static URL getResourceURL(String string) throws NoSuchFileException {
        URL uRL = Launcher.class.getResource('/' + string);
        if (uRL == null) {
            throw new NoSuchFileException(string);
        }
        return uRL;
    }

    @LauncherAPI
    public static boolean hasExtension(Path path, String string) {
        return IOHelper.getFileName(path).endsWith('.' + string);
    }

    @LauncherAPI
    public static boolean isDir(Path path) {
        return Files.isDirectory(path, LINK_OPTIONS);
    }

    @LauncherAPI
    public static boolean isEmpty(Path path) throws IOException {
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(path);){
            boolean bl = !directoryStream.iterator().hasNext();
            return bl;
        }
    }

    @LauncherAPI
    public static boolean isFile(Path path) {
        return Files.isRegularFile(path, LINK_OPTIONS);
    }

    @LauncherAPI
    public static boolean isValidFileName(String string) {
        return !string.equals(".") && !string.equals("..") && string.chars().noneMatch(n -> n == 47 || n == 92) && IOHelper.isValidPath(string);
    }

    @LauncherAPI
    public static boolean isValidPath(String string) {
        try {
            IOHelper.toPath(string);
            return true;
        }
        catch (InvalidPathException invalidPathException) {
            return false;
        }
    }

    @LauncherAPI
    public static boolean isValidTextureBounds(int n, int n2, boolean bl) {
        return n % 64 == 0 && (n2 << 1 == n || !bl && n2 == n) && n <= 1024 || bl && n % 22 == 0 && n2 % 17 == 0 && n / 22 == n2 / 17;
    }

    @LauncherAPI
    public static void move(Path path, Path path2) throws IOException {
        IOHelper.createParentDirs(path2);
        Files.move(path, path2, COPY_OPTIONS);
    }

    @LauncherAPI
    public static byte[] newBuffer() {
        return new byte[BUFFER_SIZE];
    }

    @LauncherAPI
    public static ByteArrayOutputStream newByteArrayOutput() {
        return new ByteArrayOutputStream();
    }

    @LauncherAPI
    public static char[] newCharBuffer() {
        return new char[BUFFER_SIZE];
    }

    @LauncherAPI
    public static URLConnection newConnection(URL uRL) throws IOException {
        URLConnection uRLConnection = uRL.openConnection();
        if (uRLConnection instanceof HttpURLConnection) {
            uRLConnection.setReadTimeout(HTTP_TIMEOUT);
            uRLConnection.setConnectTimeout(HTTP_TIMEOUT);
            uRLConnection.addRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)");
        } else {
            uRLConnection.setUseCaches(false);
        }
        uRLConnection.setDoInput(true);
        uRLConnection.setDoOutput(false);
        return uRLConnection;
    }

    @LauncherAPI
    public static Deflater newDeflater() {
        Deflater deflater = new Deflater(-1, true);
        deflater.setStrategy(0);
        return deflater;
    }

    @LauncherAPI
    public static Inflater newInflater() throws IOException {
        return new Inflater(true);
    }

    @LauncherAPI
    public static InputStream newInput(URL uRL) throws IOException {
        return IOHelper.newConnection(uRL).getInputStream();
    }

    @LauncherAPI
    public static InputStream newInput(Path path) throws IOException {
        return Files.newInputStream(path, READ_OPTIONS);
    }

    @LauncherAPI
    public static OutputStream newOutput(Path path) throws IOException {
        return IOHelper.newOutput(path, false);
    }

    @LauncherAPI
    public static OutputStream newOutput(Path path, boolean bl) throws IOException {
        IOHelper.createParentDirs(path);
        return Files.newOutputStream(path, bl ? APPEND_OPTIONS  : WRITE_OPTIONS);
    }

    @LauncherAPI
    public static BufferedReader newReader(InputStream inputStream) {
        return IOHelper.newReader(inputStream, UNICODE_CHARSET);
    }

    @LauncherAPI
    public static BufferedReader newReader(InputStream inputStream, Charset charset) {
        return new BufferedReader(new InputStreamReader(inputStream, charset));
    }

    @LauncherAPI
    public static BufferedReader newReader(URL uRL) throws IOException {
        URLConnection uRLConnection = IOHelper.newConnection(uRL);
        String string = uRLConnection.getContentEncoding();
        return IOHelper.newReader(uRLConnection.getInputStream(), string == null ? UNICODE_CHARSET : Charset.forName(string));
    }

    @LauncherAPI
    public static BufferedReader newReader(Path path) throws IOException {
        return Files.newBufferedReader(path, UNICODE_CHARSET);
    }

    @LauncherAPI
    public static Socket newSocket() throws SocketException {
        Socket socket = new Socket();
        IOHelper.setSocketFlags(socket);
        return socket;
    }

    @LauncherAPI
    public static BufferedWriter newWriter(OutputStream outputStream) {
        return new BufferedWriter(new OutputStreamWriter(outputStream, UNICODE_CHARSET));
    }

    @LauncherAPI
    public static BufferedWriter newWriter(Path path) throws IOException {
        return IOHelper.newWriter(path, false);
    }

    @LauncherAPI
    public static BufferedWriter newWriter(Path path, boolean bl) throws IOException {
        IOHelper.createParentDirs(path);
        return Files.newBufferedWriter(path, UNICODE_CHARSET, bl ? APPEND_OPTIONS  : WRITE_OPTIONS);
    }

    @LauncherAPI
    public static BufferedWriter newWriter(FileDescriptor fileDescriptor) {
        return IOHelper.newWriter(new FileOutputStream(fileDescriptor));
    }

    @LauncherAPI
    public static ZipEntry newZipEntry(String string) {
        ZipEntry zipEntry = new ZipEntry(string);
        zipEntry.setTime(0L);
        return zipEntry;
    }

    @LauncherAPI
    public static ZipEntry newZipEntry(ZipEntry zipEntry) {
        return IOHelper.newZipEntry(zipEntry.getName());
    }

    @LauncherAPI
    public static ZipInputStream newZipInput(InputStream inputStream) {
        return new ZipInputStream(inputStream, UNICODE_CHARSET);
    }

    @LauncherAPI
    public static ZipInputStream newZipInput(URL uRL) throws IOException {
        return IOHelper.newZipInput(IOHelper.newInput(uRL));
    }

    @LauncherAPI
    public static ZipInputStream newZipInput(Path path) throws IOException {
        return IOHelper.newZipInput(IOHelper.newInput(path));
    }

    @LauncherAPI
    public static byte[] read(Path path) throws IOException {
        long l = IOHelper.readAttributes(path).size();
        if (l > Integer.MAX_VALUE) {
            throw new IOException("File too big");
        }
        byte[] byArray = new byte[(int)l];
        try (InputStream inputStream = IOHelper.newInput(path);){
            IOHelper.read(inputStream, byArray);
        }
        return byArray;
    }

    @LauncherAPI
    public static byte[] read(URL uRL) throws IOException {
        try (InputStream inputStream = IOHelper.newInput(uRL);){
            byte[] byArray = IOHelper.read(inputStream);
            return byArray;
        }
    }

    @LauncherAPI
    public static void read(InputStream inputStream, byte[] byArray) throws IOException {
        int n;
        for (int i = 0; i < byArray.length; i += n) {
            n = inputStream.read(byArray, i, byArray.length - i);
            if (n >= 0) continue;
            throw new EOFException(String.format("%d bytes remaining", byArray.length - i));
        }
    }

    @LauncherAPI
    public static byte[] read(InputStream inputStream) throws IOException {
        try (ByteArrayOutputStream byteArrayOutputStream = IOHelper.newByteArrayOutput();){
            IOHelper.transfer(inputStream, (OutputStream)byteArrayOutputStream);
            byte[] byArray = byteArrayOutputStream.toByteArray();
            return byArray;
        }
    }

    @LauncherAPI
    public static BasicFileAttributes readAttributes(Path path) throws IOException {
        return Files.readAttributes(path, BasicFileAttributes.class, LINK_OPTIONS);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @LauncherAPI
    public static BufferedImage readTexture(Object object, boolean bl) throws IOException  {
        ImageReader imageReader = ImageIO.getImageReadersByMIMEType("image/png").next();
        try {
            imageReader.setInput(ImageIO.createImageInputStream(object), false, false);
            int n = imageReader.getWidth(0);
            int n2 = imageReader.getHeight(0);
            if (!IOHelper.isValidTextureBounds(n, n2, bl)) {
                throw new IOException(String.format("Invalid texture bounds: %dx%d", n, n2));
            }
            BufferedImage bufferedImage = imageReader.read(0);
            return bufferedImage;
        }
        finally {
            imageReader.dispose();
        }
    }

    @LauncherAPI
    public static String request(URL uRL) throws IOException {
        return IOHelper.decode(IOHelper.read(uRL)).trim();
    }

    @LauncherAPI
    public static InetSocketAddress resolve(InetSocketAddress inetSocketAddress) {
        if (inetSocketAddress.isUnresolved()) {
            return new InetSocketAddress(inetSocketAddress.getHostString(), inetSocketAddress.getPort());
        }
        return inetSocketAddress;
    }

    @LauncherAPI
    public static Path resolveIncremental(Path path, String string, String string2) {
        Path path2;
        Path path3 = path.resolve(string + '.' + string2);
        if (!IOHelper.exists(path3)) {
            return path3;
        }
        int n = 1;
        while (IOHelper.exists(path2 = path.resolve(String.format("%s (%d).%s", string, n, string2)))) {
            ++n;
        }
        return path2;
    }

    @LauncherAPI
    public static Path resolveJavaBin(Path path) {
        Path path2;
        Path path3 = (path == null ? JVM_DIR : path).resolve("bin");
        if (!LogHelper.isDebugEnabled() && IOHelper.isFile(path2 = path3.resolve("javaw.exe"))) {
            return path2;
        }
        path2 = path3.resolve("java.exe");
        if (IOHelper.isFile(path2)) {
            return path2;
        }
        Path path4 = path3.resolve("java");
        if (IOHelper.isFile(path4)) {
            return path4;
        }
        throw new RuntimeException("Java binary wasn't found");
    }

    @LauncherAPI
    public static void setSocketFlags(Socket socket) throws SocketException {
        socket.setKeepAlive(false);
        socket.setTcpNoDelay(false);
        socket.setReuseAddress(true);
        socket.setSoTimeout(SOCKET_TIMEOUT);
        socket.setTrafficClass(28);
        socket.setPerformancePreferences(1, 0, 2);
    }

    @LauncherAPI
    public static Path toPath(String string) {
        return Paths.get(CROSS_SEPARATOR_PATTERN.matcher(string).replaceAll(Matcher.quoteReplacement(PLATFORM_SEPARATOR)), new String[0]);
    }

    @LauncherAPI
    public static String toString(Path path) {
        return PLATFORM_SEPARATOR_PATTERN.matcher(path.toString()).replaceAll(Matcher.quoteReplacement(CROSS_SEPARATOR));
    }

    @LauncherAPI
    public static URI toURI(URL uRL) {
        try {
            return uRL.toURI();
        }
        catch (URISyntaxException uRISyntaxException) {
            throw new IllegalArgumentException(uRISyntaxException);
        }
    }

    @LauncherAPI
    public static URL toURL(Path path) {
        try {
            return path.toUri().toURL();
        }
        catch (MalformedURLException malformedURLException) {
            throw new InternalError(malformedURLException);
        }
    }

    @LauncherAPI
    public static int transfer(InputStream inputStream, OutputStream outputStream) throws IOException {
        int n = 0;
        byte[] byArray = IOHelper.newBuffer();
        int n2 = inputStream.read(byArray);
        while (n2 >= 0) {
            outputStream.write(byArray, 0, n2);
            n += n2;
            n2 = inputStream.read(byArray);
        }
        return n;
    }

    @LauncherAPI
    public static void transfer(Path path, OutputStream outputStream) throws IOException {
        try (InputStream inputStream = IOHelper.newInput(path);){
            IOHelper.transfer(inputStream, outputStream);
        }
    }

    @LauncherAPI
    public static int transfer(InputStream inputStream, Path path) throws IOException {
        return IOHelper.transfer(inputStream, path, false);
    }

    @LauncherAPI
    public static int transfer(InputStream inputStream, Path path, boolean bl) throws IOException {
        try (OutputStream outputStream = IOHelper.newOutput(path, bl);){
            int n = IOHelper.transfer(inputStream, outputStream);
            return n;
        }
    }

    @LauncherAPI
    public static String urlDecode(String string) {
        try {
            return URLDecoder.decode(string, UNICODE_CHARSET.name());
        }
        catch (UnsupportedEncodingException unsupportedEncodingException) {
            throw new InternalError(unsupportedEncodingException);
        }
    }

    @LauncherAPI
    public static String urlEncode(String string) {
        try {
            return URLEncoder.encode(string, UNICODE_CHARSET.name());
        }
        catch (UnsupportedEncodingException unsupportedEncodingException) {
            throw new InternalError(unsupportedEncodingException);
        }
    }

    @LauncherAPI
    public static String verifyFileName(String string) {
        return VerifyHelper.verify(string, IOHelper::isValidFileName, String.format("Invalid file name: '%s'", string));
    }

    @LauncherAPI
    public static int verifyLength(int n, int n2) throws IOException {
        if (n < 0 || n2 < 0 && n != -n2 || n2 > 0 && n > n2) {
            throw new IOException("Illegal length: " + n);
        }
        return n;
    }

    @LauncherAPI
    public static BufferedImage verifyTexture(BufferedImage bufferedImage2, boolean bl) {
        return VerifyHelper.verify(bufferedImage2, bufferedImage -> IOHelper.isValidTextureBounds(bufferedImage.getWidth(), bufferedImage.getHeight(), bl), String.format("Invalid texture bounds: %dx%d", bufferedImage2.getWidth(), bufferedImage2.getHeight()));
    }

    @LauncherAPI
    public static String verifyURL(String string) {
        try {
            new URL(string).toURI();
            return string;
        }
        catch (MalformedURLException | URISyntaxException exception) {
            throw new IllegalArgumentException("Invalid URL", exception);
        }
    }

    @LauncherAPI
    public static URL convertToURL(String string) {
        try {
            return new URL(string);
        }
        catch (MalformedURLException malformedURLException) {
            throw new IllegalArgumentException("Invalid URL", malformedURLException);
        }
    }

    @LauncherAPI
    public static void walk(Path path, FileVisitor fileVisitor, boolean bl) throws IOException {
        Files.walkFileTree(path, WALK_OPTIONS, Integer.MAX_VALUE, bl ? fileVisitor : new SkipHiddenVisitor(fileVisitor));
    }

    @LauncherAPI
    public static void write(Path path, byte[] byArray) throws IOException {
        IOHelper.createParentDirs(path);
        Files.write(path, byArray, WRITE_OPTIONS);
    }

    @LauncherAPI
    public static OutputStream newBufferedOutStream(OutputStream outputStream) {
        return new BufferedOutputStream(outputStream);
    }

    @LauncherAPI
    public static InputStream newBufferedInputStream(InputStream inputStream) {
        return new BufferedInputStream(inputStream);
    }
    private static final class DeleteDirVisitor extends SimpleFileVisitor<Path> {
        private final Path dir;
        private final boolean self;

        private DeleteDirVisitor(Path dir, boolean self) {
            this.dir = dir;
            this.self = self;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            FileVisitResult result = super.postVisitDirectory(dir, exc);
            if (self || !this.dir.equals(dir)) {
                Files.delete(dir);
            }
            return result;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Files.delete(file);
            return super.visitFile(file, attrs);
        }
    }

    private static final class SkipHiddenVisitor implements FileVisitor<Path> {
        private final FileVisitor<Path> visitor;

        private SkipHiddenVisitor(FileVisitor<Path> fileVisitor) {
            this.visitor = fileVisitor;
        }

        /* renamed from: aux */
        public FileVisitResult postVisitDirectory(Path path, IOException iOException) throws IOException {
            return Files.isHidden(path) ? FileVisitResult.CONTINUE : this.visitor.postVisitDirectory(path, iOException);
        }

        /* renamed from: aux */
        public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
            return Files.isHidden(path) ? FileVisitResult.SKIP_SUBTREE : this.visitor.preVisitDirectory(path, basicFileAttributes);
        }

        /* renamed from: Aux */
        public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
            return Files.isHidden(path) ? FileVisitResult.CONTINUE : this.visitor.visitFile(path, basicFileAttributes);
        }

        /* renamed from: Aux */
        public FileVisitResult visitFileFailed(Path path, IOException iOException) throws IOException {
            return this.visitor.visitFileFailed(path, iOException);
        }
    }
}