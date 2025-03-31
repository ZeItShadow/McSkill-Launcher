package launcher.serialize.config;

import launcher.LauncherAPI;
import launcher.helper.VerifyHelper;
import launcher.serialize.config.entry.*;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;

public class TextConfigReader {
    private final LineNumberReader reader;
        private final boolean ro;
        private String skipped;
        private int ch = -1;

        private TextConfigReader(Reader reader, boolean bl) {
            this.reader = new LineNumberReader(reader);
            this.reader.setLineNumber(1);
            this.ro = bl;
        }

        @LauncherAPI
        public static BlockConfigEntry read(Reader reader, boolean bl) throws IOException {
            return new TextConfigReader(reader, bl).readBlock(0);
        }

        private IOException aux(String string) {
            return new IOException(string + " (line " + this.reader.getLineNumber() + ')');
        }

        private int nextChar(boolean bl) throws IOException {
            this.ch = this.reader.read();
            if (bl && this.ch < 0) {
                throw this.aux("Unexpected end of config");
            }
            return this.ch;
        }

        private int nextClean(boolean bl) throws IOException {
            this.nextChar(bl);
            return this.skipWhitespace(bl);
        }

        private BlockConfigEntry readBlock(int n) throws IOException {
            LinkedHashMap<String, ConfigEntry> linkedHashMap = new LinkedHashMap<String, ConfigEntry>(16);
            boolean bl = this.ch == 123;
            boolean bl2 = bl;
            while (!(this.nextClean(bl) < 0 || bl && this.ch == 125)) {
                String object = this.skipped;
                String string = this.readToken();
                if (this.skipWhitespace(true) != 58) {
                    throw this.aux("Value start expected");
                }
                String string2 = this.skipped;
                this.nextClean(true);
                String string3 = this.skipped;
                ConfigEntry ConfigEntry2 = this.readEntry(4);
                if (this.skipWhitespace(true) != 59) {
                    throw this.aux("Value end expected");
                }
                ConfigEntry2.setComment(0, object);
                ConfigEntry2.setComment(1, string2);
                ConfigEntry2.setComment(2, string3);
                ConfigEntry2.setComment(3, this.skipped);
                if (linkedHashMap.put(string, ConfigEntry2) == null) continue;
                throw this.aux(String.format("Duplicate config entry: '%s'", string));
            }
            BlockConfigEntry block = new BlockConfigEntry(linkedHashMap, this.ro, n + 1);
            block.setComment(n, this.skipped);
            this.nextChar(false);
            return block;
        }

        private ConfigEntry readEntry(int n) throws IOException {
            String string;
            switch (this.ch) {
                case 34: {
                    return this.readString(n);
                }
                case 91: {
                    return this.readList(n);
                }
                case 123: {
                    return this.readBlock(n);
                }
            }
            if (this.ch == 45 || this.ch >= 48 && this.ch <= 57) {
                return this.readInteger(n);
            }
            switch (string = this.readToken()) {
                case "true": {
                    return new BooleanConfigEntry(Boolean.TRUE, this.ro, n);
                }
                case "false": {
                    return new BooleanConfigEntry(Boolean.FALSE, this.ro, n);
                }
            }
            throw this.aux(String.format("Unknown statement: '%s'", string));
        }

        private ConfigEntry readInteger(int n) throws IOException {
            return new IntegerConfigEntry(Integer.parseInt(this.readToken()), this.ro, n);
        }

        private ConfigEntry readList(int n) throws IOException {
            ArrayList<ConfigEntry> arrayList = new ArrayList<ConfigEntry>(16);
            boolean bl = this.nextClean(true) != 93;
            String string = this.skipped;
            while (bl) {
                ConfigEntry ConfigEntry2 = this.readEntry(2);
                bl = this.skipWhitespace(true) != 93;
                ConfigEntry2.setComment(0, string);
                ConfigEntry2.setComment(1, this.skipped);
                arrayList.add(ConfigEntry2);
                if (!bl) continue;
                if (this.ch != 44) {
                    throw this.aux("Comma expected");
                }
                this.nextClean(true);
                string = this.skipped;
            }
            boolean bl2 = arrayList.isEmpty();
            ListConfigEntry listConfigEntry = new ListConfigEntry(arrayList, this.ro, bl2 ? n + 1 : n);
            if (bl2) {
                listConfigEntry.setComment(n, this.skipped);
            }
            this.nextChar(false);
            return listConfigEntry;
        }

        private ConfigEntry readString(int n) throws IOException {
            StringBuilder stringBuilder = new StringBuilder();
            block12: while (this.nextChar(true) != 34) {
                switch (this.ch) {
                    case 10:
                    case 13: {
                        throw this.aux("String termination");
                    }
                    case 92: {
                        int n2 = this.nextChar(true);
                        switch (n2) {
                            case 116: {
                                stringBuilder.append('\t');
                                continue block12;
                            }
                            case 98: {
                                stringBuilder.append('\b');
                                continue block12;
                            }
                            case 110: {
                                stringBuilder.append('\n');
                                continue block12;
                            }
                            case 114: {
                                stringBuilder.append('\r');
                                continue block12;
                            }
                            case 102: {
                                stringBuilder.append('\f');
                                continue block12;
                            }
                            case 34:
                            case 92: {
                                stringBuilder.append((char)n2);
                                continue block12;
                            }
                        }
                        throw this.aux("Illegal char escape: " + (char)n2);
                    }
                }
                stringBuilder.append((char)this.ch);
            }
            this.nextChar(false);
            return new StringConfigEntry(stringBuilder.toString(), this.ro, n);
        }

        private String readToken() throws IOException {
            StringBuilder stringBuilder = new StringBuilder();
            while (VerifyHelper.isValidIDNameChar(this.ch)) {
                stringBuilder.append((char)this.ch);
                this.nextChar(false);
            }
            String string = stringBuilder.toString();
            if (string.isEmpty()) {
                throw this.aux("Not a token");
            }
            return string;
        }

        private void skipComment(StringBuilder stringBuilder, boolean bl) throws IOException {
            while (this.ch >= 0 && this.ch != 13 && this.ch != 10) {
                stringBuilder.append((char)this.ch);
                this.nextChar(bl);
            }
        }

        private int skipWhitespace(boolean bl) throws IOException {
            StringBuilder stringBuilder = new StringBuilder();
            while (Character.isWhitespace(this.ch) || this.ch == 35) {
                if (this.ch == 35) {
                    this.skipComment(stringBuilder, bl);
                    continue;
                }
                stringBuilder.append((char)this.ch);
                this.nextChar(bl);
            }
            this.skipped = stringBuilder.toString();
            return this.ch;
        }
}