package launcher.serialize.signed;

import launcher.LauncherAPI;
import launcher.helper.SecurityHelper;
import launcher.serialize.HInput;
import launcher.serialize.HOutput;
import launcher.serialize.stream.StreamObject;

import java.io.IOException;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

public class SignedBytesHolder extends StreamObject {
    protected final byte[] bytes;
    private final byte[] sign;

    @LauncherAPI
    public SignedBytesHolder(HInput hInput, RSAPublicKey rSAPublicKey) throws IOException, SignatureException {
        this(hInput.readByteArray(0), hInput.readByteArray(-256), rSAPublicKey);
    }

    @LauncherAPI
    public SignedBytesHolder(byte[] byArray, byte[] byArray2, RSAPublicKey rSAPublicKey) throws SignatureException {
        SecurityHelper.verifySign(byArray, byArray2, rSAPublicKey);
        this.bytes = (byte[])byArray.clone();
        this.sign = (byte[])byArray2.clone();
    }

    @LauncherAPI
    public SignedBytesHolder(byte[] byArray, RSAPrivateKey rSAPrivateKey) {
        this.bytes = (byte[])byArray.clone();
        this.sign = SecurityHelper.sign(byArray, rSAPrivateKey);
    }

    @Override
    public final void write(HOutput hOutput) throws IOException {
        hOutput.writeByteArray(this.bytes, 0);
        hOutput.writeByteArray(this.sign, -256);
    }

    @LauncherAPI
    public final byte[] getBytes() {
        return (byte[])this.bytes.clone();
    }

    @LauncherAPI
    public final byte[] getSign() {
        return (byte[])this.sign.clone();
    }
}
