package launcher.serialize.config.entry;

import launcher.LauncherAPI;
import launcher.serialize.HInput;
import launcher.serialize.HOutput;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class ListConfigEntry extends ConfigEntry<List<ConfigEntry<?>>> {
    @LauncherAPI
    public ListConfigEntry(List list, boolean bl, int n) {
        super(list, bl, n);
    }

    @LauncherAPI
    public ListConfigEntry(HInput hInput, boolean bl) throws IOException {
        super(ListConfigEntry.readList(hInput, bl), bl, 0);
    }

    private static List<ConfigEntry<?>> readList(HInput hInput, boolean bl) throws IOException {
        int n = hInput.readLength(0);
        ArrayList arrayList = new ArrayList(n);
        for (int i = 0; i < n; ++i) {
            arrayList.add(ListConfigEntry.readEntry(hInput, bl));
        }
        return arrayList;
    }

    @Override
    public Type getType() {
        return Type.LIST;
    }

    @Override
    protected void uncheckedSetValue(List<ConfigEntry<?>> list) {
        ArrayList arrayList = new ArrayList(list);
        super.uncheckedSetValue(this.ro ? Collections.unmodifiableList(arrayList) : arrayList);
    }

    @Override
    public void write(HOutput hOutput) throws IOException {
        List<ConfigEntry<?>> list = (List)this.getValue();
        hOutput.writeLength(list.size(), 0);
        for (ConfigEntry configEntry : list) {
            ListConfigEntry.writeEntry(configEntry, hOutput);
        }
    }

    @LauncherAPI
    public <V, E extends ConfigEntry<V>> Stream<V> stream(Class<E> clazz) {
        return (this.getValue()).stream().map(clazz::cast).map(ConfigEntry::getValue);
    }

    @LauncherAPI
    public void verifyOfType(Type lPt22) {
        if ((this.getValue()).stream().anyMatch(configEntry -> configEntry.getType() != lPt22)) {
            throw new IllegalArgumentException("List type mismatch: " + lPt22.name());
        }
    }
}