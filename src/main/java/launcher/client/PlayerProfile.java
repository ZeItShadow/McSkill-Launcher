package launcher.client;

import launcher.LauncherAPI;
import launcher.helper.IOHelper;
import launcher.helper.SecurityHelper;
import launcher.helper.VerifyHelper;
import launcher.serialize.HInput;
import launcher.serialize.HOutput;
import launcher.serialize.stream.StreamObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Objects;
import java.util.UUID;

public class PlayerProfile extends StreamObject {
    @LauncherAPI
    public final UUID uuid;
    @LauncherAPI
    public final String username;
    @LauncherAPI
    public final Texture skin;
    @LauncherAPI
    public final Texture cloak;

    @LauncherAPI
    public PlayerProfile(HInput hInput) throws IOException {
        this.uuid = hInput.readUUID();
        this.username = VerifyHelper.verifyUsername(hInput.readString(64));
        this.skin = hInput.readBoolean() ? new Texture(hInput) : null;
        this.cloak = hInput.readBoolean() ? new Texture(hInput) : null;
    }

    @LauncherAPI
    public PlayerProfile(UUID uUID, String string, Texture texture, Texture texture2) {
        this.uuid = Objects.requireNonNull(uUID, "uuid");
        this.username = VerifyHelper.verifyUsername(string);
        this.skin = texture;
        this.cloak = texture2;
    }

    @LauncherAPI
    public static PlayerProfile newOfflineProfile(String string) {
        return new PlayerProfile(PlayerProfile.offlineUUID(string), string, null, null);
    }

    @LauncherAPI
    public static UUID offlineUUID(String string) {
        return UUID.nameUUIDFromBytes(IOHelper.encodeASCII("OfflinePlayer:" + string));
    }

    @Override
    public void write(HOutput hOutput) throws IOException {
        hOutput.writeUUID(this.uuid);
        hOutput.writeString(this.username, 64);
        hOutput.writeBoolean(this.skin != null);
        if (this.skin != null) {
            this.skin.write(hOutput);
        }
        hOutput.writeBoolean(this.cloak != null);
        if (this.cloak != null) {
            this.cloak.write(hOutput);
        }
    }

    public static final class Texture extends StreamObject {
        private static final SecurityHelper.DigestAlgorithm DIGEST_ALGO = SecurityHelper.DigestAlgorithm.SHA256;
        @LauncherAPI
        public final String url;
        @LauncherAPI
        public final byte[] digest;

        @LauncherAPI
        public Texture(String string, byte[] byArray) {
            this.url = IOHelper.verifyURL(string);
            this.digest = Objects.requireNonNull(byArray, "digest");
        }

        @LauncherAPI
        public Texture(String url, boolean cloak) throws IOException {
            this.url = IOHelper.verifyURL(url);

            // Fetch texture
            byte[] texture;
            try (InputStream input = IOHelper.newInput(new URL(url))) {
                texture = IOHelper.read(input);
            }
            try (ByteArrayInputStream input = new ByteArrayInputStream(texture)) {
                IOHelper.readTexture(input, cloak); // Verify texture
            }

            // Get digest of texture
            digest = SecurityHelper.digest(DIGEST_ALGO, new URL(url));
        }

        @LauncherAPI
        public Texture(HInput hInput) throws IOException {
            this.url = IOHelper.verifyURL(hInput.readASCII(2048));
            this.digest = hInput.readByteArray(-DIGEST_ALGO.bytes);
        }

        @Override
        public void write(HOutput hOutput) throws IOException {
            hOutput.writeASCII(this.url, 2048);
            hOutput.writeByteArray(this.digest, -DIGEST_ALGO.bytes);
        }
    }
}