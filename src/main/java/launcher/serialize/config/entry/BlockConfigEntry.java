package launcher.serialize.config.entry;

import launcher.LauncherAPI;
import launcher.helper.VerifyHelper;
import launcher.serialize.HInput;
import launcher.serialize.HOutput;

import java.io.IOException;
import java.util.*;

public class BlockConfigEntry extends ConfigEntry<Map<String, ConfigEntry<?>>> {
    @LauncherAPI
    public BlockConfigEntry(Map map, boolean bl, int n) {
        super(map, bl, n);
    }

    @LauncherAPI
    public BlockConfigEntry(int n) {
        super(Collections.emptyMap(), false, n);
    }

    @LauncherAPI
    public BlockConfigEntry(HInput hInput, boolean bl) throws IOException {
        super(BlockConfigEntry.readMap(hInput, bl), bl, 0);
    }

    private static Map<String, ConfigEntry<?>> readMap(HInput hInput, boolean bl) throws IOException {
        int n = hInput.readLength(0);
        LinkedHashMap linkedHashMap = new LinkedHashMap(n);
        for (int i = 0; i < n; ++i) {
            String string = VerifyHelper.verifyIDName(hInput.readString(255));
            ConfigEntry<?> configEntry = BlockConfigEntry.readEntry(hInput, bl);
            VerifyHelper.putIfAbsent(linkedHashMap, string, configEntry, String.format("Duplicate config entry: '%s'", string));
        }
        return linkedHashMap;
    }

    @Override
    public Type getType() {
        return Type.BLOCK;
    }

    @Override
    public Map<String, ConfigEntry<?>> getValue() {
        Map<String, ConfigEntry<?>> map = (Map<String, ConfigEntry<?>>)super.getValue();
        return this.ro ? map : Collections.unmodifiableMap(map);
    }

    @Override
    protected void uncheckedSetValue(Map<String, ConfigEntry<?>> map) {
        LinkedHashMap<String, ConfigEntry<?>> linkedHashMap = new LinkedHashMap<>(map);
        linkedHashMap.keySet().stream().forEach(VerifyHelper::verifyIDName);
        super.uncheckedSetValue(this.ro ? Collections.unmodifiableMap(linkedHashMap) : linkedHashMap);
    }

    @Override
    public void write(HOutput hOutput) throws IOException {
        Set<Map.Entry<String, ConfigEntry<?>>> set = this.getValue().entrySet();
        hOutput.writeLength(set.size(), 0);
        for (Map.Entry<String, ConfigEntry<?>> entry : set) {
            hOutput.writeString((String)entry.getKey(), 255);
            BlockConfigEntry.writeEntry((ConfigEntry)entry.getValue(), hOutput);
        }
    }

    @LauncherAPI
    public void clear() {
        ((Map)super.getValue()).clear();
    }

    @LauncherAPI
    public ConfigEntry getEntry(String string, Class clazz) {
        Map map = (Map)super.getValue();
        ConfigEntry configEntry = (ConfigEntry)map.get(string);
        if (!clazz.isInstance(configEntry)) {
            throw new NoSuchElementException(string);
        }
        return (ConfigEntry)clazz.cast(configEntry);
    }

    @LauncherAPI
    public Object getEntryValue(String string, Class clazz) {
        return this.getEntry(string, clazz).getValue();
    }

    @LauncherAPI
    public boolean hasEntry(String string) {
        return this.getValue().containsKey(string);
    }

    @LauncherAPI
    public void remove(String string) {
        ((Map)super.getValue()).remove(string);
    }

    @LauncherAPI
    public void setEntry(String string, ConfigEntry configEntry) {
        ((Map)super.getValue()).put(VerifyHelper.verifyIDName(string), configEntry);
    }
}
