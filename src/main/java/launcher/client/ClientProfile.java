package launcher.client;

import launcher.LauncherAPI;
import launcher.hasher.FileNameMatcher;
import launcher.helper.IOHelper;
import launcher.helper.VerifyHelper;
import launcher.serialize.HInput;
import launcher.serialize.config.ConfigObject;
import launcher.serialize.config.entry.*;
import launcher.serialize.stream.StreamObject;

import java.io.IOException;
import java.net.InetSocketAddress;

public final class ClientProfile
        extends ConfigObject
        implements Comparable {
    @LauncherAPI
    public static final StreamObject.Adapter<ClientProfile> RO_ADAPTER = hInput -> new ClientProfile(hInput, true);
    private static final FileNameMatcher matcher = new FileNameMatcher(new String[0], new String[]{"indexes", "objects"}, new String[0]);
    private final StringConfigEntry version;
    private final StringConfigEntry assetIndex;
    private final IntegerConfigEntry sortIndex;
    private final StringConfigEntry title;
    private final StringConfigEntry serverAddress;
    private final IntegerConfigEntry serverPort;
    private final StringConfigEntry jvmVersion;
    private final ListConfigEntry update;
    private final ListConfigEntry updateVerify;
    private final ListConfigEntry updateExclusions;
    private final BooleanConfigEntry updateFastCheck;
    private final StringConfigEntry mainClass;
    private final ListConfigEntry classPath;
    private final ListConfigEntry jvmArgs;
    private final ListConfigEntry clientArgs;

    @LauncherAPI
    public ClientProfile(BlockConfigEntry blockConfigEntry) {
        super(blockConfigEntry);
        this.version = (StringConfigEntry)blockConfigEntry.getEntry("version", StringConfigEntry.class);
        this.assetIndex = (StringConfigEntry)blockConfigEntry.getEntry("assetIndex", StringConfigEntry.class);
        this.sortIndex = (IntegerConfigEntry)blockConfigEntry.getEntry("sortIndex", IntegerConfigEntry.class);
        this.title = (StringConfigEntry)blockConfigEntry.getEntry("title", StringConfigEntry.class);
        this.serverAddress = (StringConfigEntry)blockConfigEntry.getEntry("serverAddress", StringConfigEntry.class);
        this.serverPort = (IntegerConfigEntry)blockConfigEntry.getEntry("serverPort", IntegerConfigEntry.class);
        this.jvmVersion = (StringConfigEntry)blockConfigEntry.getEntry("jvmVersion", StringConfigEntry.class);
        this.update = (ListConfigEntry)blockConfigEntry.getEntry("update", ListConfigEntry.class);
        this.updateVerify = (ListConfigEntry)blockConfigEntry.getEntry("updateVerify", ListConfigEntry.class);
        this.updateExclusions = (ListConfigEntry)blockConfigEntry.getEntry("updateExclusions", ListConfigEntry.class);
        this.updateFastCheck = (BooleanConfigEntry)blockConfigEntry.getEntry("updateFastCheck", BooleanConfigEntry.class);
        this.mainClass = (StringConfigEntry)blockConfigEntry.getEntry("mainClass", StringConfigEntry.class);
        this.classPath = (ListConfigEntry)blockConfigEntry.getEntry("classPath", ListConfigEntry.class);
        this.jvmArgs = (ListConfigEntry)blockConfigEntry.getEntry("jvmArgs", ListConfigEntry.class);
        this.clientArgs = (ListConfigEntry)blockConfigEntry.getEntry("clientArgs", ListConfigEntry.class);
    }

    @LauncherAPI
    public ClientProfile(HInput hInput, boolean bl) throws IOException {
        this(new BlockConfigEntry(hInput, bl));
    }

    public int compare(ClientProfile clientProfile) {
        return Integer.compare(this.getSortIndex(), clientProfile.getSortIndex());
    }

    public String toString() {
        return (String)this.title.getValue();
    }

    @LauncherAPI
    public String getAssetIndex() {
        return (String)this.assetIndex.getValue();
    }

    @LauncherAPI
    public void setAssetIndex(String string) {
        this.assetIndex.setValue(string);
    }

    @LauncherAPI
    public FileNameMatcher getAssetUpdateMatcher() {
        return Version.compare(this.getVersion(), "1.7.3") >= 0 ? matcher : null;
    }

    @LauncherAPI
    public boolean supportsJVMWithARM() {
        return Version.compare(this.getVersion(), "1.18.2") > 0;
    }

    @LauncherAPI
    public String[] getClassPath() {
        return (String[])this.classPath.stream(StringConfigEntry.class).toArray(String[]::new);
    }

    @LauncherAPI
    public String[] getClientArgs() {
        return (String[])this.clientArgs.stream(StringConfigEntry.class).toArray(String[]::new);
    }

    @LauncherAPI
    public FileNameMatcher getClientUpdateMatcher() {
        String[] stringArray = (String[])this.update.stream(StringConfigEntry.class).toArray(String[]::new);
        String[] stringArray2 = (String[])this.updateVerify.stream(StringConfigEntry.class).toArray(String[]::new);
        String[] stringArray3 = (String[])this.updateExclusions.stream(StringConfigEntry.class).toArray(String[]::new);
        return new FileNameMatcher(stringArray, stringArray2, stringArray3);
    }

    @LauncherAPI
    public String getJvmVersion() {
        return (String)this.jvmVersion.getValue();
    }

    @LauncherAPI
    public String[] getJvmArgs() {
        return (String[])this.jvmArgs.stream(StringConfigEntry.class).toArray(String[]::new);
    }

    @LauncherAPI
    public String getMainClass() {
        return (String)this.mainClass.getValue();
    }

    @LauncherAPI
    public String getServerAddress() {
        return (String)this.serverAddress.getValue();
    }

    @LauncherAPI
    public int getServerPort() {
        return (Integer)this.serverPort.getValue();
    }

    @LauncherAPI
    public InetSocketAddress getServerSocketAddress() {
        return InetSocketAddress.createUnresolved(this.getServerAddress(), this.getServerPort());
    }

    @LauncherAPI
    public int getSortIndex() {
        return (Integer)this.sortIndex.getValue();
    }

    @LauncherAPI
    public String getTitle() {
        return (String)this.title.getValue();
    }

    @LauncherAPI
    public void setTitle(String string) {
        this.title.setValue(string);
    }

    @LauncherAPI
    public String getVersion() {
        return (String)this.version.getValue();
    }

    @LauncherAPI
    public void setVersion(String string) {
        this.version.setValue(string);
    }

    @LauncherAPI
    public boolean isUpdateFastCheck() {
        return (Boolean)this.updateFastCheck.getValue();
    }

    @LauncherAPI
    public void verify() {
        VerifyHelper.verify(this.getVersion(), VerifyHelper.NOT_EMPTY, "Game version can't be empty");
        IOHelper.verifyFileName(this.getAssetIndex());
        VerifyHelper.verify(this.getTitle(), VerifyHelper.NOT_EMPTY, "Profile title can't be empty");
        VerifyHelper.verify(this.getServerAddress(), VerifyHelper.NOT_EMPTY, "Server address can't be empty");
        VerifyHelper.verifyInt(this.getServerPort(), VerifyHelper.range(0, 65535), "Illegal server port: " + this.getServerPort());
        this.update.verifyOfType(ConfigEntry.Type.STRING);
        this.updateVerify.verifyOfType(ConfigEntry.Type.STRING);
        this.updateExclusions.verifyOfType(ConfigEntry.Type.STRING);
        this.jvmArgs.verifyOfType(ConfigEntry.Type.STRING);
        this.classPath.verifyOfType(ConfigEntry.Type.STRING);
        this.clientArgs.verifyOfType(ConfigEntry.Type.STRING);
        VerifyHelper.verify(this.getTitle(), VerifyHelper.NOT_EMPTY, "Main class can't be empty");
    }

    public int compareTo(Object object) {
        return this.compare((ClientProfile)object);
    }

    public static class Version {
        public static int compare(String string, String string2) {
            String[] stringArray = string.split("\\.");
            String[] stringArray2 = string2.split("\\.");
            int n = Math.max(stringArray.length, stringArray2.length);
            for (int i = 0; i < n; ++i) {
                int n3 = i < stringArray.length ? Integer.parseInt(stringArray[i]) : 0;
                int n2 = i < stringArray2.length ? Integer.parseInt(stringArray2[i]) : 0;
                int n4 = n2;
                if (n3 < n2) {
                    return -1;
                }
                if (n3 <= n2) continue;
                return 1;
            }
            return 0;
        }
    }

}
