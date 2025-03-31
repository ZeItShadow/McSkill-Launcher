package launcher.request.auth;


import launcher.Launcher;
import launcher.LauncherAPI;
import launcher.client.PlayerProfile;
import launcher.helper.VerifyHelper;
import launcher.request.Request;
import launcher.serialize.HInput;
import launcher.serialize.HOutput;

import java.io.IOException;

public final class AuthRequest
        extends Request<AuthRequest.Result> {
    private final String login;
    private final byte[] encryptedPassword;

    @LauncherAPI
    public AuthRequest(Launcher.Config config, String string, byte[] byArray) {
        super(config);
        this.login = VerifyHelper.verify(string, VerifyHelper.NOT_EMPTY, "Login can't be empty");
        this.encryptedPassword = (byte[])byArray.clone();
    }

    @LauncherAPI
    public AuthRequest(String login, byte[] encryptedPassword) {
        this(null, login, encryptedPassword);
    }

    @Override
    public Type getType() {
        return Request.Type.AUTH;
    }

    @Override
    protected Result requestDo(HInput hInput, HOutput hOutput) throws IOException {
        hOutput.writeString(this.login, 255);
        hOutput.writeByteArray(this.encryptedPassword, 2048);
        hOutput.flush();
        this.readError(hInput);
        PlayerProfile playerProfile = new PlayerProfile(hInput);
        int n = hInput.readInt();
        String string = hInput.readASCII(-n);
        return new Result(playerProfile, string);
    }

    public static final class Result {
        @LauncherAPI
        public final PlayerProfile pp;
        @LauncherAPI
        public final String accessToken;

        private Result(PlayerProfile pp, String accessToken) {
            this.pp = pp;
            this.accessToken = accessToken;
        }
    }

}