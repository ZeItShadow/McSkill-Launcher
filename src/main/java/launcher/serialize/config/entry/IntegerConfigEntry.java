package launcher.serialize.config.entry;

import launcher.LauncherAPI;
import launcher.serialize.HInput;
import launcher.serialize.HOutput;

import java.io.IOException;

public class IntegerConfigEntry extends ConfigEntry<Integer> {
    @LauncherAPI
    public IntegerConfigEntry(int n, boolean bl, int n2) {
        super(n, bl, n2);
    }

    @LauncherAPI
    public IntegerConfigEntry(HInput hInput, boolean bl) throws IOException {
        this(hInput.readVarInt(), bl, 0);
    }

    @Override
    public Type getType() {
        return Type.INTEGER;
    }

    @Override
    public void write(HOutput hOutput) throws IOException {
        hOutput.writeVarInt((Integer)this.getValue());
    }
}
