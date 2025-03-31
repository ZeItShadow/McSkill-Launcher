package launcher.request.uuid;

import launcher.Launcher;
import launcher.LauncherAPI;
import launcher.client.PlayerProfile;
import launcher.request.Request;
import launcher.serialize.HInput;
import launcher.serialize.HOutput;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

public class ProfileByUUIDRequest extends Request<PlayerProfile> {
    private final UUID uuid;

    @LauncherAPI
    public ProfileByUUIDRequest(Launcher.Config config, UUID uUID) {
        super(config);
        this.uuid = Objects.requireNonNull(uUID, "uuid");
    }

    @LauncherAPI
    public ProfileByUUIDRequest(UUID uUID) {
        this(null, uUID);
    }

    @Override
    public Type getType() {
        return Type.PROFILE_BY_UUID;
    }

    @Override
    protected PlayerProfile requestDo(HInput hInput, HOutput hOutput) throws IOException {
        hOutput.writeUUID(this.uuid);
        hOutput.flush();
        return hInput.readBoolean() ? new PlayerProfile(hInput) : null;
    }
}
