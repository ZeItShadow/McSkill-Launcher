package launcher.request.uuid;

import launcher.Launcher;
import launcher.LauncherAPI;
import launcher.client.PlayerProfile;
import launcher.helper.VerifyHelper;
import launcher.request.Request;
import launcher.serialize.HInput;
import launcher.serialize.HOutput;

import java.io.IOException;

public final class ProfileByUsernameRequest
        extends Request<PlayerProfile> {
    private final String aux;

    @LauncherAPI
    public ProfileByUsernameRequest(Launcher.Config config, String string) {
        super(config);
        this.aux = VerifyHelper.verifyUsername(string);
    }

    @LauncherAPI
    public ProfileByUsernameRequest(String string) {
        this(null, string);
    }

    @Override
    public Type getType() {
        return Type.PROFILE_BY_USERNAME;
    }

    @Override
    protected PlayerProfile requestDo(HInput hInput, HOutput hOutput) throws IOException {
        hOutput.writeString(this.aux, 64);
        hOutput.flush();
        return hInput.readBoolean() ? new PlayerProfile(hInput) : null;
    }
}
