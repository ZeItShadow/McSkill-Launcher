package launcher.request;

import launcher.Launcher;
import launcher.LauncherAPI;
import launcher.helper.VerifyHelper;
import launcher.serialize.HInput;
import launcher.serialize.HOutput;

public abstract class CustomRequest<T>
        extends Request<T> {
    @LauncherAPI
    public CustomRequest(Launcher.Config config) {
        super(config);
    }

    @LauncherAPI
    public CustomRequest() {
        this(null);
    }

    @Override
    public final Type getType() {
        return Type.CUSTOM;
    }

    @Override
    protected final T requestDo(HInput hInput, HOutput hOutput) throws Throwable {
        hOutput.writeASCII(VerifyHelper.verifyIDName(this.getName()), 255);
        hOutput.flush();
        return this.requestDoCustom(hInput, hOutput);
    }

    @LauncherAPI
    public abstract String getName();

    @LauncherAPI
    protected abstract T requestDoCustom(HInput var1, HOutput var2);
}
