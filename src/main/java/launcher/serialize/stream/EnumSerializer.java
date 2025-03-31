package launcher.serialize.stream;

import launcher.LauncherAPI;
import launcher.helper.VerifyHelper;
import launcher.serialize.HInput;
import launcher.serialize.HOutput;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class EnumSerializer<E extends Enum<?> & EnumSerializer.Itf> {
    private final Map<Integer, E> aux = new HashMap<Integer, E>(16);

    @LauncherAPI
    public EnumSerializer(Class<E> clazz) {
        for (Enum e : (Enum[])clazz.getEnumConstants()) {
            VerifyHelper.putIfAbsent(this.aux, ((Itf)((Object)e)).getNumber(), e, "Duplicate number for enum constant " + e.name());
        }
    }

    @LauncherAPI
    public static void write(HOutput hOutput, Itf itf) throws IOException {
        hOutput.writeVarInt(itf.getNumber());
    }

    @LauncherAPI
    public E read(HInput hInput) throws IOException {
        int n = hInput.readVarInt();
        return (E)((Enum)VerifyHelper.getMapValue(this.aux, n, "Unknown enum number: " + n));
    }
    @FunctionalInterface
    public interface Itf {
        @LauncherAPI
        int getNumber();
    }
}
