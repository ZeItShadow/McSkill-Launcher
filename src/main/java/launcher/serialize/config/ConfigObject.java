package launcher.serialize.config;

import launcher.LauncherAPI;
import launcher.serialize.HOutput;
import launcher.serialize.config.entry.BlockConfigEntry;
import launcher.serialize.stream.StreamObject;

import java.io.IOException;
import java.util.Objects;

public abstract class ConfigObject extends StreamObject {
    @LauncherAPI
    public final BlockConfigEntry block;

    @LauncherAPI
    protected ConfigObject(BlockConfigEntry blockConfigEntry) {
        this.block = Objects.requireNonNull(blockConfigEntry, "block");
    }

    @Override
    public final void write(HOutput hOutput) throws IOException {
        this.block.write(hOutput);
    }

    @FunctionalInterface
    public interface Adapter<O extends ConfigObject> {
        @LauncherAPI
        O convert(BlockConfigEntry entry);
    }
}
