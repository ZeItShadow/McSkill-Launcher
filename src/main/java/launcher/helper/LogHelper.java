package launcher.helper;

import launcher.Launcher;
import launcher.LauncherAPI;

import java.io.*;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;
import org.fusesource.jansi.AnsiOutputStream;

public class LogHelper {
    @LauncherAPI
    public static final String DEBUG_PROPERTY = "launcher.debug";
    @LauncherAPI
    public static final String NO_JANSI_PROPERTY = "launcher.noJAnsi";
    @LauncherAPI
    public static final boolean JANSI;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss", Locale.US);
    private static final AtomicBoolean DEBUG_ENABLED  = new AtomicBoolean(Boolean.getBoolean(DEBUG_PROPERTY));
    private static final Set<Output> OUTPUTS = Collections.newSetFromMap(new ConcurrentHashMap<>(2));
    private static final Output STD_OUTPUT;

    static {
        boolean jansi;

        try {
            if (Boolean.getBoolean(NO_JANSI_PROPERTY)) {
                jansi = false;
            } else {
                Class.forName("org.fusesource.jansi.Ansi");
                AnsiConsole.systemInstall();
                jansi = true;
            }
        }
        catch (ClassNotFoundException classNotFoundException) {
            jansi = false;
        }
        JANSI = jansi;
        STD_OUTPUT  = System.out::println;
        addOutput(STD_OUTPUT);
        String string = System.getProperty("launcher.logFile");
        if (string != null) {
            try {
                LogHelper.addOutput(IOHelper.toPath(string));
            }
            catch (IOException iOException) {
                LogHelper.error(iOException);
            }
        }
    }
    private LogHelper() {
    }

    @LauncherAPI
    public static void addOutput(Output output) {
        OUTPUTS.add(Objects.requireNonNull(output, "output"));
    }

    @LauncherAPI
    public static void addOutput(Path path) throws IOException {
        if (JANSI) {
            LogHelper.addOutput((Output)new JAnsiOutput(IOHelper.newOutput(path, true)));
        } else {
            LogHelper.addOutput(IOHelper.newWriter(path, true));
        }
    }

    @LauncherAPI
    public static void addOutput(Writer writer) {
        LogHelper.addOutput((Output)new WriterOutput(writer));
    }

    @LauncherAPI
    public static void debug(String string) {
        if (LogHelper.isDebugEnabled()) {
            LogHelper.log(Level.DEBUG, string, false);
        }
    }

    @LauncherAPI
    public static void debug(String string, Object ... objectArray) {
        LogHelper.debug(String.format(string, objectArray));
    }

    @LauncherAPI
    public static void error(Throwable throwable) {
        LogHelper.error(LogHelper.isDebugEnabled() ? LogHelper.toString(throwable) : throwable.toString());
    }

    @LauncherAPI
    public static void error(String string) {
        LogHelper.log(Level.ERROR, string, false);
    }

    @LauncherAPI
    public static void error(String string, Object ... objectArray) {
        LogHelper.error(String.format(string, objectArray));
    }

    @LauncherAPI
    public static void info(String string) {
        LogHelper.log(Level.INFO, string, false);
    }

    @LauncherAPI
    public static void info(String string, Object ... objectArray) {
        LogHelper.info(String.format(string, objectArray));
    }

    @LauncherAPI
    public static boolean isDebugEnabled() {
        return true;
        //return DEBUG_ENABLED.get();
    }

    @LauncherAPI
    public static void setDebugEnabled(boolean bl) {
        DEBUG_ENABLED.set(bl);
    }

    @LauncherAPI
    public static void log(Level com62, String string, boolean bl) {
        String string2 = DATE_TIME_FORMATTER.format(LocalDateTime.now());
        LogHelper.println(JANSI ? LogHelper.ansiFormatLog(com62, string2, string, bl) : LogHelper.formatLog(com62, string, string2, bl));
    }

    @LauncherAPI
    public static void printVersion(String string) {
        LogHelper.println(JANSI ? LogHelper.ansiFormatVersion(string) : LogHelper.formatVersion(string));
    }

    @LauncherAPI
    public static synchronized void println(String string) {
        for (Output output : OUTPUTS) {
            output.println(string);
        }
    }

    @LauncherAPI
    public static boolean removeOutput(Output output) {
        return OUTPUTS.remove(output);
    }

    @LauncherAPI
    public static boolean removeStdOutput() {
        return LogHelper.removeOutput(STD_OUTPUT);
    }

    @LauncherAPI
    public static void subDebug(String string) {
        if (LogHelper.isDebugEnabled()) {
            LogHelper.log(Level.DEBUG, string, true);
        }
    }

    @LauncherAPI
    public static void subDebug(String string, Object ... objectArray) {
        LogHelper.subDebug(String.format(string, objectArray));
    }

    @LauncherAPI
    public static void subInfo(String string) {
        LogHelper.log(Level.INFO, string, true);
    }

    @LauncherAPI
    public static void subInfo(String string, Object ... objectArray) {
        LogHelper.subInfo(String.format(string, objectArray));
    }

    @LauncherAPI
    public static void subWarning(String string) {
        LogHelper.log(Level.WARNING, string, true);
    }

    @LauncherAPI
    public static void subWarning(String string, Object ... objectArray) {
        LogHelper.subWarning(String.format(string, objectArray));
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    @LauncherAPI
    public static String toString(Throwable throwable) {
        try (StringWriter stringWriter = new StringWriter();){
            try (PrintWriter object = new PrintWriter(stringWriter);){
                throwable.printStackTrace(object);
            }
            return stringWriter.toString();
        }
        catch (IOException iOException) {
            throw new AssertionError((Object)iOException);
        }
    }

    @LauncherAPI
    public static void warning(String string) {
        LogHelper.log(Level.WARNING, string, false);
    }

    @LauncherAPI
    public static void warning(String string, Object ... objectArray) {
        LogHelper.warning(String.format(string, objectArray));
    }

    private static String ansiFormatLog(Level com62, String string, String string2, boolean bl) {
        Ansi.Color color;
        boolean bl2 = com62 != Level.DEBUG;
        switch (com62) {
            case WARNING: {
                color = Ansi.Color.YELLOW;
                break;
            }
            case ERROR: {
                color = Ansi.Color.RED;
                break;
            }
            default: {
                color = Ansi.Color.WHITE;
            }
        }
        Ansi ansi = new Ansi();
        ansi.fg(Ansi.Color.WHITE).a(string);
        ansi.fgBright(Ansi.Color.WHITE).a(" [").bold();
        if (bl2) {
            ansi.fgBright(color);
        } else {
            ansi.fg(color);
        }
        ansi.a((Object)com62).boldOff().fgBright(Ansi.Color.WHITE).a("] ");
        if (bl2) {
            ansi.fgBright(color);
        } else {
            ansi.fg(color);
        }
        if (bl) {
            ansi.a(' ').a(Ansi.Attribute.ITALIC);
        }
        ansi.a(string2);
        return ansi.reset().toString();
    }

    private static String ansiFormatVersion(String string) {
        return new Ansi().bold().fgBright(Ansi.Color.CYAN).a(string).fgBright(Ansi.Color.WHITE).a(" v").fgBright(Ansi.Color.BLUE).a("1.7.5.2").fgBright(Ansi.Color.WHITE).a(" (build #").fgBright(Ansi.Color.RED).a(Launcher.BUILD).fgBright(Ansi.Color.WHITE).a(')').reset().toString();
    }

    private static String formatLog(Level com62, String string, String string2, boolean bl) {
        if (bl) {
            string = ' ' + string;
        }
        return string2 + " [" + com62.name + "] " + string;
    }

    private static String formatVersion(String string) {
        return String.format("%s v%s (build #%s)", string, "1.7.5.2", Launcher.BUILD);
    }

    @LauncherAPI
    @FunctionalInterface
    public interface Output {
        void println(String message);
    }

    @LauncherAPI
    public enum Level {
        DEBUG("DEBUG"),
        INFO("INFO"),
        WARNING("WARN"),
        ERROR("ERROR");

        public final String name;

        Level(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

    private static final class JAnsiOutput extends WriterOutput  {

        private JAnsiOutput(OutputStream outputStream) throws IOException{
            super(IOHelper.newWriter(new AnsiOutputStream(outputStream)));
        }
    }
    private static class WriterOutput implements Output, AutoCloseable {
        private final Writer writer;

        private WriterOutput(Writer writer) {
            this.writer = writer;
        }

        @Override
        public void close() throws IOException {
            writer.close();
        }

        @Override
        public void println(String message) {
            try {
                writer.write(message + System.lineSeparator());
                writer.flush();
            } catch (IOException ignored) {
                // Do nothing?
            }
        }
    }
}