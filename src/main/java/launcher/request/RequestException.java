package launcher.request;

import launcher.LauncherAPI;

import java.io.IOException;

public class RequestException extends IOException {
    private static final long serialVersionUID = 7558237657082664821L;

    @LauncherAPI
    public RequestException(String string) {
        super(string);
    }

    @LauncherAPI
    public RequestException(Throwable throwable) {
        super(throwable);
    }

    @LauncherAPI
    public RequestException(String string, Throwable throwable) {
        super(string, throwable);
    }

    @Override
    public String toString() {
        return this.getMessage();
    }
}
