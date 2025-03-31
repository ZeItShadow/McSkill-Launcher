package launcher.serialize.signed;

import launcher.LauncherAPI;
import launcher.serialize.HInput;
import launcher.serialize.stream.StreamObject;

import java.io.IOException;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

public class SignedObjectHolder<O extends StreamObject>
        extends SignedBytesHolder {
    @LauncherAPI
    public final O object;

    @LauncherAPI
    public SignedObjectHolder(HInput hInput, RSAPublicKey rSAPublicKey, StreamObject.Adapter<O> adapter) throws IOException, SignatureException {
        super(hInput, rSAPublicKey);
        this.object = this.newInstance(adapter);
    }

    @LauncherAPI
    public SignedObjectHolder(O streamObject, RSAPrivateKey rSAPrivateKey) throws IOException {
        super(((StreamObject)streamObject).write(), rSAPrivateKey);
        this.object = streamObject;
    }

    public boolean equals(Object object) {
        return object instanceof SignedObjectHolder && this.object.equals(((SignedObjectHolder)object).object);
    }

    public int hashCode() {
        return this.object.hashCode();
    }

    public String toString() {
        return this.object.toString();
    }

    @LauncherAPI
    public O newInstance(StreamObject.Adapter<O> adapter) throws IOException {
        try (HInput hInput = new HInput(this.bytes);){
            O o;
            O o2;
            O o3 = o2 = (o = adapter.convert(hInput));
            return o3;
        }
    }
}
