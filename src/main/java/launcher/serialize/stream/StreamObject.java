package launcher.serialize.stream;

import launcher.LauncherAPI;
import launcher.helper.IOHelper;
import launcher.serialize.HInput;
import launcher.serialize.HOutput;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public abstract class StreamObject {
    @LauncherAPI
    public abstract void write(HOutput var1) throws IOException;

    @LauncherAPI
    public final byte[] write() throws IOException {
        try (ByteArrayOutputStream array = IOHelper.newByteArrayOutput();){
            try (HOutput output = new HOutput(array);){
                this.write(output);
            }
            return array.toByteArray();
        }
    }

    @FunctionalInterface
    public static interface Adapter<O extends StreamObject> {
        @LauncherAPI
        public O convert(HInput var1) throws IOException;
    }
}
