package launcher.request.uuid;

import launcher.Launcher;
import launcher.LauncherAPI;
import launcher.client.PlayerProfile;
import launcher.helper.IOHelper;
import launcher.helper.VerifyHelper;
import launcher.request.Request;
import launcher.serialize.HInput;
import launcher.serialize.HOutput;

import java.io.IOException;

public class BatchProfileByUsernameRequest extends Request<PlayerProfile[]> {
    @LauncherAPI
    public static final int MAX_BATCH_SIZE = 128;
    private final String[] usernames;

    @LauncherAPI
    public BatchProfileByUsernameRequest(Launcher.Config config, String ... stringArray) throws IOException {
        super(config);
        this.usernames = (String[])stringArray.clone();
        IOHelper.verifyLength(this.usernames.length, 128);
        for (String string : this.usernames) {
            VerifyHelper.verifyUsername(string);
        }
    }

    @LauncherAPI
    public BatchProfileByUsernameRequest(String ... stringArray) throws IOException {
        this((Launcher.Config)null, stringArray);
    }

    @Override
    public Type getType() {
        return Type.BATCH_PROFILE_BY_USERNAME;
    }

    @Override
    protected PlayerProfile[] requestDo(HInput hInput, HOutput hOutput) throws IOException {
        hOutput.writeLength(this.usernames.length, 128);
        for (String string : this.usernames) {
            hOutput.writeString(string, 64);
        }
        hOutput.flush();
        PlayerProfile[] objectArray = new PlayerProfile[this.usernames.length];
        for (int i = 0; i < objectArray.length; ++i) {
            objectArray[i] = hInput.readBoolean() ? new PlayerProfile(hInput) : null;
        }
        return objectArray;
    }
}