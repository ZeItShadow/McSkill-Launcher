package launcher.helper;

import launcher.LauncherAPI;

import java.util.Map;
import java.util.Objects;
import java.util.function.DoublePredicate;
import java.util.function.IntPredicate;
import java.util.function.LongPredicate;
import java.util.function.Predicate;
import java.util.regex.Pattern;

@LauncherAPI
public class VerifyHelper {
public static final IntPredicate POSITIVE = n -> n > 0;
    @LauncherAPI
    public static final IntPredicate NOT_NEGATIVE = n3 -> n3 >= 0;
    @LauncherAPI
    public static final LongPredicate L_POSITIVE = n5 -> n5 > 0L;
    @LauncherAPI
    public static final LongPredicate L_NOT_NEGATIVE = n7 -> n7 >= 0L;
    @LauncherAPI
    public static final Predicate<String> NOT_EMPTY = s -> !s.isEmpty();
    @LauncherAPI
    public static final Pattern USERNAME_PATTERN = Pattern.compile("[a-zA-Z\u0430-\u044f\u0410-\u042f0-9_.\\-]{1,16}");

    private VerifyHelper() {
    }

    @LauncherAPI
    public static <K, V> V getMapValue(Map<K, V> map, K o, String s) {
        return (V)VerifyHelper.verify(map.get(o), Objects::nonNull, s);
    }

    @LauncherAPI
    public static boolean isValidIDName(String s) {
        return !s.isEmpty() && s.length() <= 255 && s.chars().allMatch(VerifyHelper::isValidIDNameChar);
    }

    @LauncherAPI
    public static boolean isValidIDNameChar(int n) {
        return n >= 97 && n <= 122 || n >= 65 && n <= 90 || n >= 48 && n <= 57 || n == 45 || n == 95;
    }

    @LauncherAPI
    public static boolean isValidUsername(CharSequence input) {
        return USERNAME_PATTERN.matcher(input).matches();
    }

    @LauncherAPI
    public static void putIfAbsent(Map map, Object key, Object value, String s) {
        VerifyHelper.verify(map.putIfAbsent(key, value), Objects::isNull, s);
    }

    @LauncherAPI
    public static IntPredicate range(int min, int max) {
        return i -> i >= min && i <= max;
    }

    @LauncherAPI
    public static <T> T verify(T o, Predicate<T> predicate, String s) {
        if (predicate.test(o)) {
            return o;
        }
        throw new IllegalArgumentException(s);
    }

    @LauncherAPI
    public static double verifyDouble(double n, DoublePredicate doublePredicate, String s) {
        if (doublePredicate.test(n)) {
            return n;
        }
        throw new IllegalArgumentException(s);
    }

    @LauncherAPI
    public static String verifyIDName(String s) {
        return VerifyHelper.verify(s, VerifyHelper::isValidIDName, String.format("Invalid name: '%s'", s));
    }

    @LauncherAPI
    public static int verifyInt(int n, IntPredicate intPredicate, String s) {
        if (intPredicate.test(n)) {
            return n;
        }
        throw new IllegalArgumentException(s);
    }

    @LauncherAPI
    public static long verifyLong(long n, LongPredicate longPredicate, String s) {
        if (longPredicate.test(n)) {
            return n;
        }
        throw new IllegalArgumentException(s);
    }

    @LauncherAPI
    public static String verifyUsername(String s) {
        return VerifyHelper.verify(s, VerifyHelper::isValidUsername, String.format("Invalid username: '%s'", s));
    }
}
