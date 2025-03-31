package launcher.helper.js;

import javafx.application.Application;
import launcher.LauncherAPI;

import java.util.concurrent.atomic.AtomicReference;

@LauncherAPI
public abstract class JSApplication
        extends Application {
    private static final AtomicReference INSTANCE = new AtomicReference();

    public JSApplication() {
        INSTANCE.set(this);
    }

    public static JSApplication getInstance() {
        return (JSApplication)INSTANCE.get();
    }
}
