package launcher.serialize.config.entry;

import launcher.LauncherAPI;
import launcher.serialize.HInput;
import launcher.serialize.HOutput;

import java.io.IOException;

public class StringConfigEntry extends ConfigEntry {
    @LauncherAPI
    public StringConfigEntry(String string, boolean bl, int n) {
        super(string, bl, n);
    }

    @LauncherAPI
    public StringConfigEntry(HInput hInput, boolean bl) throws IOException {
        this(hInput.readString(0), bl, 0);
    }

    @Override
    public Type getType() {
        return Type.STRING;
    }

    protected void uncheckedSetValue(String string) {
        super.uncheckedSetValue(string);
    }

    @Override
    public void write(HOutput hOutput) throws IOException {
        hOutput.writeString((String)this.getValue(), 0);
    }
}
