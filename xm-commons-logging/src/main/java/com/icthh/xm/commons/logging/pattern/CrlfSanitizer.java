package com.icthh.xm.commons.logging.pattern;

/**
 * Replaces line breaks with a space to prevent log forging (CWE-117).
 *
 * <p>Each run of {@code \r}, {@code \n}, vertical tab, form feed, NEL ({@code \u0085}),
 * line separator ({@code  }) and paragraph separator ({@code  }) becomes one space.
 * Other characters, including ordinary spaces and tabs, are kept as is.
 */
public final class CrlfSanitizer {

    private static final char REPLACEMENT = ' ';

    private CrlfSanitizer() {
        throw new IllegalAccessError("Access not allowed");
    }

    public static String sanitize(String value) {
        if (value == null) {
            return null;
        }
        int first = indexOfLineBreak(value);
        if (first < 0) {
            return value;
        }
        StringBuilder result = new StringBuilder(value.length());
        result.append(value, 0, first);
        boolean inBreak = false;
        for (int i = first; i < value.length(); i++) {
            char c = value.charAt(i);
            if (isLineBreak(c)) {
                if (!inBreak) {
                    result.append(REPLACEMENT);
                    inBreak = true;
                }
            } else {
                result.append(c);
                inBreak = false;
            }
        }
        return result.toString();
    }

    private static int indexOfLineBreak(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (isLineBreak(value.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isLineBreak(char c) {
        return c == '\n' || c == '\r' || c == '\u000B' || c == '\f'
            || c == '\u0085' || c == ' ' || c == ' ';
    }
}
