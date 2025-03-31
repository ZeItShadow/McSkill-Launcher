package launcher.hasher;

import launcher.LauncherAPI;
import launcher.serialize.HInput;
import launcher.serialize.stream.EnumSerializer;
import launcher.serialize.stream.StreamObject;

import java.io.IOException;

public abstract class HashedEntry
        extends StreamObject {
    @LauncherAPI
    public boolean flag;

    @LauncherAPI
    public abstract Type getType();

    @LauncherAPI
    public abstract long size();

    @LauncherAPI
    public enum Type implements EnumSerializer.Itf
    {
        DIR(1),
        FILE(2);

        private static final EnumSerializer<Type> SERIALIZER = new EnumSerializer<>(Type.class);
        private final int n;

        Type(int n2) {
            this.n = n2;
        }

        public static Type read(HInput hInput) throws IOException {
            return SERIALIZER.read(hInput);
        }

        @Override
        public int getNumber() {
            return this.n;
        }
    }

}

