package launcher.serialize;

import launcher.LauncherAPI;
import launcher.helper.IOHelper;

import java.io.Flushable;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.Objects;
import java.util.UUID;

public class HOutput implements Flushable,
        AutoCloseable {
    @LauncherAPI
    public final OutputStream stream;

    @LauncherAPI
    public HOutput(OutputStream outputStream) {
        this.stream = Objects.requireNonNull(outputStream, "stream");
    }

    @Override
    public void close() throws IOException {
        this.stream.close();
    }

    @Override
    public void flush() throws IOException {
        this.stream.flush();
    }

    @LauncherAPI
    public void writeASCII(String string, int n) throws IOException {
        this.writeByteArray(IOHelper.encodeASCII(string), n);
    }

    @LauncherAPI
    public void writeBigInteger(BigInteger bigInteger, int n) throws IOException {
        this.writeByteArray(bigInteger.toByteArray(), n);
    }

    @LauncherAPI
    public void writeBoolean(boolean bl) throws IOException {
        this.writeUnsignedByte(bl ? 1 : 0);
    }

    @LauncherAPI
    public void writeByteArray(byte[] byArray, int n) throws IOException {
        this.writeLength(byArray.length, n);
        this.stream.write(byArray);
    }

    @LauncherAPI
    public void writeInt(int n) throws IOException {
        this.writeUnsignedByte(n >>> 24 & 0xFF);
        this.writeUnsignedByte(n >>> 16 & 0xFF);
        this.writeUnsignedByte(n >>> 8 & 0xFF);
        this.writeUnsignedByte(n & 0xFF);
    }

    @LauncherAPI
    public void writeLength(int n, int n2) throws IOException {
        IOHelper.verifyLength(n, n2);
        if (n2 >= 0) {
            this.writeVarInt(n);
        }
    }

    @LauncherAPI
    public void writeLong(long l) throws IOException {
        this.writeInt((int)(l >> 32));
        this.writeInt((int)l);
    }

    @LauncherAPI
    public void writeShort(short s) throws IOException {
        this.writeUnsignedByte(s >>> 8 & 0xFF);
        this.writeUnsignedByte(s & 0xFF);
    }

    @LauncherAPI
    public void writeString(String string, int n) throws IOException {
        this.writeByteArray(IOHelper.encode(string), n);
    }

    @LauncherAPI
    public void writeUUID(UUID uUID) throws IOException {
        this.writeLong(uUID.getMostSignificantBits());
        this.writeLong(uUID.getLeastSignificantBits());
    }

    @LauncherAPI
    public void writeUnsignedByte(int n) throws IOException {
        this.stream.write(n);
    }

    @LauncherAPI
    public void writeVarInt(int n) throws IOException {
        while (((long)n & 0xFFFFFFFFFFFFFF80L) != 0L) {
            this.writeUnsignedByte(n & 0x7F | 0x80);
            n >>>= 7;
        }
        this.writeUnsignedByte(n);
    }

    @LauncherAPI
    public void writeVarLong(long l) throws IOException {
        while ((l & 0xFFFFFFFFFFFFFF80L) != 0L) {
            this.writeUnsignedByte((int)l & 0x7F | 0x80);
            l >>>= 7;
        }
        this.writeUnsignedByte((int)l);
    }
}
