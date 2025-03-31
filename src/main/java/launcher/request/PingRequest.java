package launcher.request;

import launcher.Launcher;
import launcher.LauncherAPI;
import launcher.serialize.HInput;
import launcher.serialize.HOutput;

import java.io.IOException;

public final class PingRequest
        extends Request<Void> {
    @LauncherAPI
    public static final byte EXPECTED_BYTE = 85;

    @LauncherAPI
    public PingRequest(Launcher.Config config) {
        super(config);
    }

    @LauncherAPI
    public PingRequest() {
        this(null);
    }

    @Override
    public Type getType() {
        return Type.PING;
    }

    @Override
    protected Void requestDo(HInput hInput, HOutput hOutput) throws IOException {
        byte by = (byte)hInput.readUnsignedByte();
        if (by != 85) {
            throw new IOException("Illegal ping response: " + by);
        }
        return null;
    }
}
