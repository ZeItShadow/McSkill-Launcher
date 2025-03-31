package launcher.hasher;

import launcher.LauncherAPI;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.regex.Pattern;

public final class FileNameMatcher {
    private static final Entry[] NO_ENTRIES = new Entry[0];
    private final Entry[] update;
    private final Entry[] verify;
    private final Entry[] exclusions;

    @LauncherAPI
    public FileNameMatcher(String[] stringArray, String[] stringArray2, String[] stringArray3) {
        this.update = FileNameMatcher.toEntries(stringArray);
        this.verify = FileNameMatcher.toEntries(stringArray2);
        this.exclusions = FileNameMatcher.toEntries(stringArray3);
    }

    private FileNameMatcher(Entry[] coM3Array, Entry[] coM3Array2, Entry[] coM3Array3) {
        this.update = coM3Array;
        this.verify = coM3Array2;
        this.exclusions = coM3Array3;
    }

    private static boolean anyMatch(Entry[] coM3Array, Collection collection) {
        return Arrays.stream(coM3Array).anyMatch(coM32 -> ((Entry)coM32).matches(collection));
    }

    private static Entry[] toEntries(String ... stringArray) {
        return (Entry[])Arrays.stream(stringArray).map(charSequence -> new Entry((CharSequence)charSequence)).toArray(Entry[]::new);
    }

    @LauncherAPI
    public boolean shouldUpdate(Collection collection) {
        return (FileNameMatcher.anyMatch(this.update, collection) || FileNameMatcher.anyMatch(this.verify, collection)) && !FileNameMatcher.anyMatch(this.exclusions, collection);
    }

    @LauncherAPI
    public boolean shouldVerify(Collection collection) {
        return FileNameMatcher.anyMatch(this.verify, collection) && !FileNameMatcher.anyMatch(this.exclusions, collection);
    }

    @LauncherAPI
    public FileNameMatcher verifyOnly() {
        return new FileNameMatcher(NO_ENTRIES, this.verify, this.exclusions);
    }

    private static final class Entry {
        private static final Pattern SPLITTER = Pattern.compile(Pattern.quote("/") + '+');
        private final Pattern[] parts;

        private Entry(CharSequence charSequence) {
            this.parts = (Pattern[])SPLITTER.splitAsStream(charSequence).map(Pattern::compile).toArray(Pattern[]::new);
        }

        private boolean matches(Collection collection) {
            if (this.parts.length > collection.size()) {
                return false;
            }
            Iterator iterator = collection.iterator();
            for (Pattern pattern : this.parts) {
                String string = (String)iterator.next();
                if (pattern.matcher(string).matches()) continue;
                return false;
            }
            return true;
        }
    }
}
