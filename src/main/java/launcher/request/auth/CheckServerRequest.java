package launcher.request.auth;

import launcher.Launcher;
import launcher.LauncherAPI;
import launcher.client.PlayerProfile;
import launcher.helper.VerifyHelper;
import launcher.request.Request;
import launcher.serialize.HInput;
import launcher.serialize.HOutput;

import java.io.IOException;

public final class CheckServerRequest
        extends Request<PlayerProfile> {
    private final String aux;
    private final String Aux;

    @LauncherAPI
    public CheckServerRequest(Launcher.Config config, String string, String string2) {
        super(config);
        this.aux = VerifyHelper.verifyUsername(string);
        this.Aux = JoinServerRequest.verifyServerID(string2);
    }

    @LauncherAPI
    public CheckServerRequest(String string, String string2) {
        this(null, string, string2);
    }

    @Override
    public Type getType() {
        return Type.CHECK_SERVER;
    }

    @Override
    protected PlayerProfile requestDo(HInput hInput, HOutput hOutput) throws IOException {
        hOutput.writeString(this.aux, 64);
        hOutput.writeASCII(this.Aux, 41);
        hOutput.flush();
        this.readError(hInput);
        return hInput.readBoolean() ? new PlayerProfile(hInput) : null;
    }
}
