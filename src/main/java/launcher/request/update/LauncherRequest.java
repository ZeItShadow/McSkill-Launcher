package launcher.request.update;

import launcher.Launcher;
import launcher.LauncherAPI;
import launcher.client.ClientLauncher;
import launcher.client.ClientProfile;
import launcher.helper.IOHelper;
import launcher.helper.JVMHelper;
import launcher.helper.LogHelper;
import launcher.helper.SecurityHelper;
import launcher.request.Request;
import launcher.serialize.HInput;
import launcher.serialize.HOutput;
import launcher.serialize.signed.SignedObjectHolder;

import java.io.IOException;
import java.nio.file.Path;
import java.security.SignatureException;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class LauncherRequest
        extends Request<LauncherRequest.Result> {
    @LauncherAPI
    public static final Path BINARY_PATH = IOHelper.getCodeSource(Launcher.class);
    @LauncherAPI
    public static final boolean EXE_BINARY = IOHelper.hasExtension(BINARY_PATH, "exe");
    //public static final boolean EXE_BINARY = true;


    @LauncherAPI
    public LauncherRequest(Launcher.Config config) {
        super(config);
    }

    @LauncherAPI
    public LauncherRequest() {
        this(null);
    }

    @LauncherAPI
    public static void update(Launcher.Config config, Result coM82) throws SignatureException, IOException {
        SecurityHelper.verifySign(coM82.binary, coM82.sign, config.publicKey);
        ArrayList<String> arrayList = new ArrayList<String>(8);
        arrayList.add(IOHelper.resolveJavaBin(null).toString());
        if (LogHelper.isDebugEnabled()) {
            arrayList.add(ClientLauncher.jvmProperty("launcher.debug", Boolean.toString(LogHelper.isDebugEnabled())));
        }
        if (Launcher.Config.ADDRESS_OVERRIDE != null) {
            arrayList.add(ClientLauncher.jvmProperty("launcher.addressOverride", Launcher.Config.ADDRESS_OVERRIDE));
        }
        arrayList.add("-jar");
        arrayList.add(BINARY_PATH.toString());
        ProcessBuilder processBuilder = new ProcessBuilder(arrayList.toArray(new String[arrayList.size()]));
        processBuilder.inheritIO();
        IOHelper.write(BINARY_PATH, coM82.binary);
        processBuilder.start();
        JVMHelper.RUNTIME.exit(255);
        throw new AssertionError((Object)"Why Launcher wasn't restarted?!");
    }

    @Override
    public Type getType() {
        return Type.LAUNCHER;
    }

    @Override
    protected Result requestDo(HInput hInput, HOutput hOutput) throws Throwable {
        hOutput.writeBoolean(EXE_BINARY);
        hOutput.flush();
        this.readError(hInput);
        RSAPublicKey rSAPublicKey = this.config.publicKey;
        byte[] byArray = hInput.readByteArray(-256);
        boolean shouldUpdate = !SecurityHelper.isValidSign(BINARY_PATH, byArray, rSAPublicKey);
        shouldUpdate = false;
        hOutput.writeBoolean(shouldUpdate);
        hOutput.flush();
        if (shouldUpdate) {
            byte[] byArray2 = hInput.readByteArray(0);
            SecurityHelper.verifySign(byArray2, byArray, this.config.publicKey);
            return new Result(byArray2, byArray, Collections.emptyList());
        }
        int count = hInput.readLength(0);
        ArrayList<SignedObjectHolder<ClientProfile>> arrayList = new ArrayList<SignedObjectHolder<ClientProfile>>(count);
        for (int i = 0; i < count; ++i) {
            arrayList.add(new SignedObjectHolder<>(hInput, rSAPublicKey, ClientProfile.RO_ADAPTER));
        }
        return new Result(null, byArray, arrayList);
    }

    public final class Result {
        @LauncherAPI
        public final List<SignedObjectHolder<ClientProfile>> profiles;
        private final byte[] binary;
        private final byte[] sign;

        private Result(byte[] byArray, byte[] byArray2, List<SignedObjectHolder<ClientProfile>> list) {
            this.binary = byArray == null ? null : (byte[])byArray.clone();
            this.sign = (byte[])byArray2.clone();
            this.profiles = Collections.unmodifiableList(list);
        }

        @LauncherAPI
        public byte[] getBinary() {
            return this.binary == null ? null : (byte[])this.binary.clone();
        }

        @LauncherAPI
        public byte[] getSign() {
            return (byte[])this.sign.clone();
        }
    }
}
