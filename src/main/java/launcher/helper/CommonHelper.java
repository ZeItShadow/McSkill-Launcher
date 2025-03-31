package launcher.helper;

import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import launcher.LauncherAPI;

import javax.script.ScriptEngine;
import java.util.Locale;

public class CommonHelper {
    private static final String[] SCRIPT_ENGINE_ARGS = new String[]{"-strict", "--language=es6", "--optimistic-types=false"};

    private CommonHelper() {
    }

    @LauncherAPI
    public static String low(String string) {
        return string.toLowerCase(Locale.US);
    }

    @LauncherAPI
    public static ScriptEngine newScriptEngine() {
        return new NashornScriptEngineFactory().getScriptEngine(SCRIPT_ENGINE_ARGS);
    }

    @LauncherAPI
    public static Thread newThread(String string, boolean bl, Runnable runnable) {
        Thread thread = new Thread(runnable);
        thread.setDaemon(bl);
        if (string != null) {
            thread.setName(string);
        }
        return thread;
    }

    @LauncherAPI
    public static String replace(String string, String ... stringArray) {
        for (int i = 0; i < stringArray.length; i += 2) {
            string = string.replace('%' + stringArray[i] + '%', stringArray[i + 1]);
        }
        return string;
    }
}
