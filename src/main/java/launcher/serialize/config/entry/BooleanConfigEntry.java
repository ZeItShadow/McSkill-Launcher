package launcher.serialize.config.entry;

import launcher.LauncherAPI;
import launcher.serialize.HInput;
import launcher.serialize.HOutput;

import java.io.IOException;

public class BooleanConfigEntry extends ConfigEntry<Boolean> {
    @LauncherAPI
    public BooleanConfigEntry(boolean bl, boolean bl2, int n) {
        super(bl, bl2, n);
    }

    @LauncherAPI
    public BooleanConfigEntry(HInput hInput, boolean bl) throws IOException {
        this(hInput.readBoolean(), bl, 0);
    }

    @Override
    public Type getType() {
        return Type.BOOLEAN;
    }

    @Override
    public void write(HOutput hOutput) throws IOException {
        hOutput.writeBoolean((Boolean)this.getValue());
    }
}
