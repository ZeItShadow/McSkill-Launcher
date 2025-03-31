package launcher.hasher;

import launcher.LauncherAPI;
import launcher.helper.IOHelper;
import launcher.helper.SecurityHelper;
import launcher.helper.VerifyHelper;
import launcher.serialize.HInput;
import launcher.serialize.HOutput;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

public final class HashedFile extends HashedEntry {
    public static final SecurityHelper.DigestAlgorithm DIGEST_ALGO = SecurityHelper.DigestAlgorithm.MD5;
    @LauncherAPI
    public final long size;
    private final byte[] digest;

    @LauncherAPI
    public HashedFile(long l, byte[] byArray) {
        this.size = VerifyHelper.verifyLong(l, VerifyHelper.L_NOT_NEGATIVE, "Illegal size: " + l);
        this.digest = byArray == null ? null : (byte[])DIGEST_ALGO.verify(byArray).clone();
    }

    @LauncherAPI
    public HashedFile(Path path, long l, boolean bl) throws IOException {
        this(l, bl ? SecurityHelper.digest(DIGEST_ALGO, path) : null);
    }

    @LauncherAPI
    public HashedFile(HInput hInput) throws IOException {
        this(hInput.readVarLong(), hInput.readBoolean() ? hInput.readByteArray(-HashedFile.DIGEST_ALGO.bytes) : null);
    }

    @Override
    public Type getType() {
        return Type.FILE;
    }

    @Override
    public long size() {
        return this.size;
    }

    @Override
    public void write(HOutput hOutput) throws IOException {
        hOutput.writeVarLong(this.size);
        hOutput.writeBoolean(this.digest != null);
        if (this.digest != null) {
            hOutput.writeByteArray(this.digest, -HashedFile.DIGEST_ALGO.bytes);
        }
    }

    @LauncherAPI
    public boolean isSame(HashedFile hashedFile) {
        return this.size == hashedFile.size && (this.digest == null || hashedFile.digest == null || Arrays.equals(this.digest, hashedFile.digest));
    }

    @LauncherAPI
    public boolean isSame(Path path, boolean bl) throws IOException {
        if (this.size != IOHelper.readAttributes(path).size()) {
            return false;
        }
        if (!bl || this.digest == null) {
            return true;
        }
        byte[] byArray = SecurityHelper.digest(DIGEST_ALGO, path);
        return Arrays.equals(this.digest, byArray);
    }

    @LauncherAPI
    public boolean isSameDigest(byte[] byArray) {
        return this.digest == null || byArray == null || Arrays.equals(this.digest, byArray);
    }
}
