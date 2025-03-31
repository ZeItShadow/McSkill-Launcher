package launcher.serialize;

import launcher.LauncherAPI;
import launcher.helper.IOHelper;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.Objects;
import java.util.UUID;

public class HInput implements AutoCloseable {
    @LauncherAPI
    public final InputStream stream;

    @LauncherAPI
    public HInput(InputStream inputStream) {
        this.stream = Objects.requireNonNull(inputStream, "stream");
    }

    @LauncherAPI
    public HInput(byte[] byArray) {
        this.stream = new ByteArrayInputStream(byArray);
    }

    @Override
    public void close() throws IOException {
        this.stream.close();
    }

    @LauncherAPI
    public String readASCII(int maxBytes) throws IOException {
        return IOHelper.decodeASCII(this.readByteArray(maxBytes));
    }

    @LauncherAPI
    public BigInteger readBigInteger(int maxBytes) throws IOException {
        return new BigInteger(this.readByteArray(maxBytes));
    }

    @LauncherAPI
    public boolean readBoolean() throws IOException {
        int n = this.readUnsignedByte();
        switch (n) {
            case 0: {
                return false;
            }
            case 1: {
                return true;
            }
        }
        throw new IOException("Invalid boolean state: " + n);
    }

    @LauncherAPI
    public byte[] readByteArray(int max) throws IOException {
        byte[] byArray = new byte[this.readLength(max)];
        IOHelper.read(this.stream, byArray);
        return byArray;
    }

    @LauncherAPI
    public int readInt() throws IOException {
        return (this.readUnsignedByte() << 24) + (this.readUnsignedByte() << 16) + (this.readUnsignedByte() << 8) + this.readUnsignedByte();
    }

    @LauncherAPI
    public int readLength(int max) throws IOException {
        if (max < 0) {
            return -max;
        }
        return IOHelper.verifyLength(this.readVarInt(), max);
    }

    @LauncherAPI
    public long readLong() throws IOException {
        return (long)this.readInt() << 32 | (long)this.readInt() & 0xFFFFFFFFL;
    }

    @LauncherAPI
    public short readShort() throws IOException {
        return (short)((this.readUnsignedByte() << 8) + this.readUnsignedByte());
    }

    @LauncherAPI
    public String readString(int maxBytes) throws IOException {
        return IOHelper.decode(this.readByteArray(maxBytes));
    }

    @LauncherAPI
    public UUID readUUID() throws IOException {
        return new UUID(this.readLong(), this.readLong());
    }

    @LauncherAPI
    public int readUnsignedByte() throws IOException {
        int n = this.stream.read();
        if (n < 0) {
            throw new EOFException("readUnsignedByte");
        }
        return n;
    }

    @LauncherAPI
    public int readUnsignedShort() throws IOException {
        return Short.toUnsignedInt(this.readShort());
    }

    @LauncherAPI
    public int readVarInt() throws IOException {
        int n = 0;
        for (int i = 0; i < 32; i += 7) {
            int n2 = this.readUnsignedByte();
            n |= (n2 & 0x7F) << i;
            if ((n2 & 0x80) != 0) continue;
            return n;
        }
        throw new IOException("VarInt too big");
    }

    @LauncherAPI
    public long readVarLong() throws IOException {
        long l = 0L;
        for (int i = 0; i < 64; i += 7) {
            int n = this.readUnsignedByte();
            l |= (long)(n & 0x7F) << i;
            if ((n & 0x80) != 0) continue;
            return l;
        }
        throw new IOException("VarLong too big");
    }
}