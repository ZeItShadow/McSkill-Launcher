package launcher.request;

import launcher.Launcher;
import launcher.LauncherAPI;
import launcher.helper.IOHelper;
import launcher.serialize.HInput;
import launcher.serialize.HOutput;
import launcher.serialize.stream.EnumSerializer;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class Request<R> {
    @LauncherAPI
    protected final Launcher.Config config;
    private final AtomicBoolean started = new AtomicBoolean(false);

    @LauncherAPI
    protected Request(Launcher.Config config) {
        this.config = config == null ? Launcher.getConfig() : config;
    }

    @LauncherAPI
    protected Request() {
        this(null);
    }

    @LauncherAPI
    public static void requestError(String s) throws RequestException {
        throw new RequestException(s);
    }

    @LauncherAPI
    public abstract Type getType();

    @LauncherAPI
    protected abstract R requestDo(HInput var1, HOutput var2) throws Throwable;

    @LauncherAPI
    public R request() throws Throwable {
        if (!this.started.compareAndSet(false, true)) {
            throw new IllegalStateException("Request already started");
        }
        try (Socket socket = IOHelper.newSocket();){
            socket.connect(IOHelper.resolve(config.address));
            try (HInput input = new HInput(socket.getInputStream());
                 HOutput output = new HOutput(socket.getOutputStream())) {
                writeHandshake(input, output);
                return requestDo(input, output);
            }
        }
    }

    @LauncherAPI
    protected final void readError(HInput hInput) throws IOException {
        String string = hInput.readString(0);
        if (!string.isEmpty()) {
            Request.requestError(string);
        }
    }

    private void writeHandshake(HInput hInput, HOutput hOutput) throws IOException {
        hOutput.writeInt(1917264919);
        hOutput.writeBigInteger(this.config.publicKey.getModulus(), 257);
        EnumSerializer.write(hOutput, this.getType());
        hOutput.flush();
        if (!hInput.readBoolean()) {
            Request.requestError("Serverside not accepted this connection");
        }
    }

    @LauncherAPI
    public enum Type implements EnumSerializer.Itf
    {
        PING(0),
        LAUNCHER(1),
        UPDATE(2),
        UPDATE_LIST(3),
        AUTH(4),
        JOIN_SERVER(5),
        CHECK_SERVER(6),
        PROFILE_BY_USERNAME(7),
        PROFILE_BY_UUID(8),
        BATCH_PROFILE_BY_USERNAME(9),
        CUSTOM(255);

        private static final EnumSerializer<Type> SERIALIZER = new EnumSerializer<Type>(Type.class);
        private final int n;

        Type(int n) {
            this.n = n;
        }

        @LauncherAPI
        public static Type read(HInput hInput) throws IOException {
            return (Type)SERIALIZER.read(hInput);
        }

        @Override
        public int getNumber() {
            return this.n;
        }
    }
}