package launcher.serialize.config.entry;

import launcher.LauncherAPI;
import launcher.serialize.HInput;
import launcher.serialize.HOutput;
import launcher.serialize.stream.EnumSerializer;
import launcher.serialize.stream.StreamObject;

import java.io.IOException;
import java.util.Objects;

public abstract class ConfigEntry <V>
        extends StreamObject {
    @LauncherAPI
    public final boolean ro;
    private final String[] comments;
    private V value;

    protected ConfigEntry(V object, boolean bl, int n) {
        this.ro = bl;
        this.comments = new String[n];
        this.uncheckedSetValue(object);
    }

    protected static ConfigEntry<?> readEntry(HInput hInput, boolean bl) throws IOException {
        Type lPt22 = Type.read(hInput);
        switch (lPt22) {
            case BOOLEAN: {
                return new BooleanConfigEntry(hInput, bl);
            }
            case INTEGER: {
                return new IntegerConfigEntry(hInput, bl);
            }
            case STRING: {
                return new StringConfigEntry(hInput, bl);
            }
            case LIST: {
                return new ListConfigEntry(hInput, bl);
            }
            case BLOCK: {
                return new BlockConfigEntry(hInput, bl);
            }
        }
        throw new AssertionError((Object)("Unsupported config entry type: " + lPt22.name()));
    }

    protected static void writeEntry(ConfigEntry<?> configEntry, HOutput hOutput) throws IOException {
        EnumSerializer.write(hOutput, configEntry.getType());
        configEntry.write(hOutput);
    }

    @LauncherAPI
    public abstract Type getType();

    @LauncherAPI
    public final String getComment(int n) {
        if (n < 0) {
            n += this.comments.length;
        }
        return n >= this.comments.length ? null : this.comments[n];
    }

    @LauncherAPI
    public V getValue() {
        return this.value;
    }

    @LauncherAPI
    public final void setValue(V object) {
        this.ensureWritable();
        this.uncheckedSetValue(object);
    }

    @LauncherAPI
    public final void setComment(int n, String string) {
        this.comments[n] = string;
    }

    protected final void ensureWritable() {
        if (this.ro) {
            throw new UnsupportedOperationException("Read-only");
        }
    }

    protected void uncheckedSetValue(V object) {
        this.value = Objects.requireNonNull(object, "value");
    }

    @LauncherAPI
    public enum Type implements EnumSerializer.Itf {
        BLOCK(1), BOOLEAN(2), INTEGER(3), STRING(4), LIST(5);
        private static final EnumSerializer<Type> SERIALIZER = new EnumSerializer<>(Type.class);
        private final int n;

        Type(int n) {
            this.n = n;
        }

        @Override
        public int getNumber() {
            return n;
        }

        public static Type read(HInput input) throws IOException {
            return SERIALIZER.read(input);
        }
    }
}
