package launcher.request.auth;

import launcher.Launcher;
import launcher.LauncherAPI;
import launcher.helper.SecurityHelper;
import launcher.helper.VerifyHelper;
import launcher.request.Request;
import launcher.serialize.HInput;
import launcher.serialize.HOutput;

import java.io.IOException;
import java.util.regex.Pattern;

public final class JoinServerRequest
        extends Request<Boolean> {
    private static final Pattern SERVERID_PATTERN = Pattern.compile("-?[0-9a-f]{1,40}");
    private final String username;
    private final String accessToken;
    private final String serverID;

    @LauncherAPI
    public JoinServerRequest(Launcher.Config config, String string, String string2, String string3) {
        super(config);
        this.username = VerifyHelper.verifyUsername(string);
        this.accessToken = SecurityHelper.verifyToken(string2);
        this.serverID = JoinServerRequest.verifyServerID(string3);
    }

    @LauncherAPI
    public JoinServerRequest(String string, String string2, String string3) {
        this(null, string, string2, string3);
    }

    @LauncherAPI
    public static boolean isValidServerID(CharSequence charSequence) {
        return SERVERID_PATTERN.matcher(charSequence).matches();
    }

    @LauncherAPI
    public static String verifyServerID(String string) {
        return VerifyHelper.verify(string, JoinServerRequest::isValidServerID, String.format("Invalid server ID: '%s'", string));
    }

    @Override
    public Type getType() {
        return Type.JOIN_SERVER;
    }

    @Override
    protected Boolean requestDo(HInput hInput, HOutput hOutput) throws IOException {
        hOutput.writeString(this.username, 64);
        hOutput.writeInt(this.accessToken.length());
        hOutput.writeASCII(this.accessToken, -this.accessToken.length());
        hOutput.writeASCII(this.serverID, 41);
        hOutput.flush();
        this.readError(hInput);
        return hInput.readBoolean();
    }
}
