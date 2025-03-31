package launcher.client;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import launcher.LauncherAPI;
import launcher.helper.IOHelper;
import launcher.helper.JVMHelper;
import launcher.helper.LogHelper;
import launcher.helper.VerifyHelper;
import launcher.serialize.HInput;
import launcher.serialize.HOutput;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.regex.Pattern;

public final class ServerPinger
{
    private static final String aux = "§1";
    private static final String Aux = "MC|PingHost";
    private static final Pattern LEGACY_PING_HOST_DELIMETER;
    private static final int PACKET_LENGTH = 65535;
    private final InetSocketAddress address;
    private final String version;
    private final Object cacheLock;
    private Result cache;
    private Throwable cacheError;
    private long cacheUntil;

    @LauncherAPI
    public ServerPinger(final InetSocketAddress obj, final String obj2) {
        this.cacheLock = new Object();
        this.cache = null;
        this.cacheError = null;
        this.cacheUntil = Long.MIN_VALUE;
        this.address = Objects.requireNonNull(obj, "address");
        this.version = Objects.requireNonNull(obj2, "version");
    }

    private static String readUTF16String(final HInput hInput) throws IOException {
        return new String(hInput.readByteArray(-(hInput.readUnsignedShort() << 1)), StandardCharsets.UTF_16BE);
    }

    private static void writeUTF16String(final HOutput hOutput, final String s) throws IOException {
        hOutput.writeShort((short)s.length());
        hOutput.stream.write(s.getBytes(StandardCharsets.UTF_16BE));
    }

    @LauncherAPI
    public Result ping() {
        synchronized (this.cacheLock) {
            if (System.currentTimeMillis() >= this.cacheUntil) {
                try {
                    this.cache = this.doPing();
                    this.cacheError = null;
                }
                catch (final Throwable aux) {
                    this.cache = null;
                    this.cacheError = aux;
                }
                finally {
                    this.cacheUntil = System.currentTimeMillis() + IOHelper.SOCKET_TIMEOUT;
                }
            }
            if (this.cacheError != null) {
                JVMHelper.UNSAFE.throwException(this.cacheError);
            }
            return this.cache;
        }
    }

    private Result doPing() throws IOException {
        final Socket socket = IOHelper.newSocket();
        try {
            socket.connect(IOHelper.resolve(this.address), IOHelper.SOCKET_TIMEOUT);
            try (final HInput hInput = new HInput(socket.getInputStream())) {
                try (final HOutput hOutput = new HOutput(socket.getOutputStream())) {
                    return (ClientProfile.Version.compare(this.version, "1.7") >= 0) ? this.modernPing(hInput, hOutput) : this.legacyPing(hInput, hOutput, ClientProfile.Version.compare(this.version, "1.6") >= 0);
                }
            }
        }
        catch (final Throwable t3) {
            if (socket != null) {
                try {
                    socket.close();
                }
                catch (final Throwable exception3) {
                    t3.addSuppressed(exception3);
                }
            }
            throw t3;
        }
    }

    private Result legacyPing(final HInput hInput, final HOutput hOutput, final boolean b) throws IOException {
        hOutput.writeUnsignedByte(254);
        hOutput.writeUnsignedByte(1);
        if (b) {
            hOutput.writeUnsignedByte(250);
            writeUTF16String(hOutput, "MC|PingHost");
            byte[] byteArray;
            try (final ByteArrayOutputStream byteArrayOutput = IOHelper.newByteArrayOutput()) {
                try (final HOutput hOutput2 = new HOutput(byteArrayOutput)) {
                    hOutput2.writeUnsignedByte(78);
                    writeUTF16String(hOutput2, this.address.getHostString());
                    hOutput2.writeInt(this.address.getPort());
                }
                byteArray = byteArrayOutput.toByteArray();
            }
            hOutput.writeShort((short)byteArray.length);
            hOutput.stream.write(byteArray);
        }
        hOutput.flush();
        final int unsignedByte = hInput.readUnsignedByte();
        if (unsignedByte != 255) {
            throw new IOException("Illegal kick packet ID: " + unsignedByte);
        }
        final String aux = readUTF16String(hInput);
        LogHelper.debug("Ping response (legacy): '%s'", aux);
        final String[] split = "§1".split(aux);
        if (split.length != 6) {
            throw new IOException("Tokens count mismatch");
        }
        final String str = split[0];
        if (!str.equals("§1")) {
            throw new IOException("Magic string mismatch: " + str);
        }
        final int int1 = Integer.parseInt(split[1]);
        if (int1 != 78) {
            throw new IOException("Protocol mismatch: " + int1);
        }
        final String s = split[2];
        if (!s.equals(this.version)) {
            throw new IOException(String.format("Version mismatch: '%s'", s));
        }
        return new Result(VerifyHelper.verifyInt(Integer.parseInt(split[4]), VerifyHelper.NOT_NEGATIVE, "onlinePlayers can't be < 0"), VerifyHelper.verifyInt(Integer.parseInt(split[5]), VerifyHelper.NOT_NEGATIVE, "maxPlayers can't be < 0"), aux);
    }

    private Result modernPing(final HInput hInput, final HOutput hOutput) throws IOException {
        byte[] byteArray;
        try (final ByteArrayOutputStream byteArrayOutput = IOHelper.newByteArrayOutput()) {
            try (final HOutput hOutput2 = new HOutput(byteArrayOutput)) {
                hOutput2.writeVarInt(0);
                hOutput2.writeVarInt(-1);
                hOutput2.writeString(this.address.getHostString(), 0);
                hOutput2.writeShort((short)this.address.getPort());
                hOutput2.writeVarInt(1);
            }
            byteArray = byteArrayOutput.toByteArray();
        }
        hOutput.writeByteArray(byteArray, 65535);
        hOutput.writeVarInt(1);
        hOutput.writeVarInt(0);
        hOutput.flush();
        int i;
        for (i = 0; i <= 0; i = IOHelper.verifyLength(hInput.readVarInt(), 65535)) {}
        String string;
        try (final HInput hInput2 = new HInput(hInput.readByteArray(-i))) {
            final int varInt = hInput2.readVarInt();
            if (varInt != 0) {
                throw new IOException("Illegal status packet ID: " + varInt);
            }
            string = hInput2.readString(65535);
            LogHelper.debug("Ping response (modern): '%s'", string);
        }
        final JsonObject object = Json.parse(string).asObject().get("players").asObject();
        return new Result(object.get("online").asInt(), object.get("max").asInt(), string);
    }

    static {
        LEGACY_PING_HOST_DELIMETER = Pattern.compile("\u0000", 16);
    }

    public static final class Result {
        @LauncherAPI
        public final int onlinePlayers;
        @LauncherAPI
        public final int maxPlayers;
        @LauncherAPI
        public final String raw;

        public Result(int n, int n2, String string) {
            this.onlinePlayers = VerifyHelper.verifyInt(n, VerifyHelper.NOT_NEGATIVE, "onlinePlayers can't be < 0");
            this.maxPlayers = VerifyHelper.verifyInt(n2, VerifyHelper.NOT_NEGATIVE, "maxPlayers can't be < 0");
            this.raw = string;
        }

        @LauncherAPI
        public boolean isOverfilled() {
            return this.onlinePlayers >= this.maxPlayers;
        }
    }
}
