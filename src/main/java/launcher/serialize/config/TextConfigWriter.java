package launcher.serialize.config;

import launcher.LauncherAPI;
import launcher.serialize.config.entry.*;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;

public class TextConfigWriter {
    private final Writer writer;
    private final boolean comments;

    private TextConfigWriter(Writer writer, boolean bl) {
        this.writer = writer;
        this.comments = bl;
    }

    @LauncherAPI
    public static void write(BlockConfigEntry blockConfigEntry, Writer writer, boolean bl) throws IOException {
        new TextConfigWriter(writer, bl).writeBlock(blockConfigEntry, false);
    }

    private void writeBlock(BlockConfigEntry blockConfigEntry, boolean bl) throws IOException {
        if (bl) {
            this.writer.write(123);
        }
        Map<String, ConfigEntry<?>> map = blockConfigEntry.getValue();
        for (Map.Entry entry : map.entrySet()) {
            String string = (String)entry.getKey();
            ConfigEntry configEntry = (ConfigEntry)entry.getValue();
            this.writeComment(configEntry.getComment(0));
            this.writer.write(string);
            this.writeComment(configEntry.getComment(1));
            this.writer.write(58);
            this.writeComment(configEntry.getComment(2));
            this.writeEntry(configEntry);
            this.writeComment(configEntry.getComment(3));
            this.writer.write(59);
        }
        this.writeComment(blockConfigEntry.getComment(-1));
        if (bl) {
            this.writer.write(125);
        }
    }

    private void writeBoolean(BooleanConfigEntry booleanConfigEntry) throws IOException {
        this.writer.write(((Boolean)booleanConfigEntry.getValue()).toString());
    }

    private void writeComment(String string) throws IOException {
        if (this.comments && string != null) {
            this.writer.write(string);
        }
    }

    private void writeEntry(ConfigEntry configEntry) throws IOException {
        ConfigEntry.Type lPt22 = configEntry.getType();
        switch (lPt22) {
            case BLOCK: {
                this.writeBlock((BlockConfigEntry)configEntry, true);
                break;
            }
            case STRING: {
                this.writeString((StringConfigEntry)configEntry);
                break;
            }
            case INTEGER: {
                this.writeInteger((IntegerConfigEntry)configEntry);
                break;
            }
            case BOOLEAN: {
                this.writeBoolean((BooleanConfigEntry)configEntry);
                break;
            }
            case LIST: {
                this.writeList((ListConfigEntry)configEntry);
                break;
            }
            default: {
                throw new AssertionError((Object)("Unsupported config entry type: " + lPt22.name()));
            }
        }
    }

    private void writeInteger(IntegerConfigEntry integerConfigEntry) throws IOException {
        this.writer.write(Integer.toString((Integer)integerConfigEntry.getValue()));
    }

    private void writeList(ListConfigEntry listConfigEntry) throws IOException {
        this.writer.write(91);
        List list = (List)listConfigEntry.getValue();
        for (int i = 0; i < list.size(); ++i) {
            if (i > 0) {
                this.writer.write(44);
            }
            ConfigEntry configEntry = (ConfigEntry)list.get(i);
            this.writeComment(configEntry.getComment(0));
            this.writeEntry(configEntry);
            this.writeComment(configEntry.getComment(1));
        }
        this.writeComment(listConfigEntry.getComment(-1));
        this.writer.write(93);
    }

    private void writeString(StringConfigEntry stringConfigEntry) throws IOException {
        this.writer.write(34);
        String string = (String)stringConfigEntry.getValue();
        block8: for (int i = 0; i < string.length(); ++i) {
            char c = string.charAt(i);
            switch (c) {
                case '\t': {
                    this.writer.write("\\t");
                    continue block8;
                }
                case '\b': {
                    this.writer.write("\\b");
                    continue block8;
                }
                case '\n': {
                    this.writer.write("\\n");
                    continue block8;
                }
                case '\r': {
                    this.writer.write("\\r");
                    continue block8;
                }
                case '\f': {
                    this.writer.write("\\f");
                    continue block8;
                }
                case '\"':
                case '\\': {
                    this.writer.write(92);
                    this.writer.write(c);
                    continue block8;
                }
                default: {
                    this.writer.write(c);
                }
            }
        }
        this.writer.write(34);
    }
}
