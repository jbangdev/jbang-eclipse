package dev.jbang.eclipse.core.internal.utils;

import java.util.Arrays;
import java.util.Objects;

public class JBangVersionHelper {

    public static boolean isHigherOrEqual(String v1, String v2) {
        return isEqual(v1, v2) || isHigher(v1, v2);
    }

    public static boolean isLowerOrEqual(String v1, String v2) {
        return isEqual(v1, v2) || isLower(v1, v2);
    }

    public static boolean isEqual(String v1, String v2) {
        return Objects.equals(v1, v2);
    }

    public static boolean isHigher(String v1, String v2) {
        int[] v1Parts = splitVersion(v1);
        int[] v2Parts = splitVersion(v2);
        for (int i = 0; i < v1Parts.length && i < v2Parts.length; i++) {
            if (v1Parts[i] > v2Parts[i]) {
                return true;
            }
        }
        return v1Parts.length > v2Parts.length;
    }

    public static boolean isLower(String v1, String v2) {
        int[] v1Parts = splitVersion(v1);
        int[] v2Parts = splitVersion(v2);
        for (int i = 0; i < v1Parts.length && i < v2Parts.length; i++) {
            if (v1Parts[i] < v2Parts[i]) {
                return true;
            }
        }
        return v1Parts.length < v2Parts.length;
    }

    public static int[] splitVersion(String version) {
        return Arrays.stream(version.split("\\."))
            .mapToInt(Integer::parseInt)
            .toArray();
    }
}
