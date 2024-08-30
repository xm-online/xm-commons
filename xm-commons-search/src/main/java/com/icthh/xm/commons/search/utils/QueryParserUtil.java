package com.icthh.xm.commons.search.utils;

import lombok.experimental.UtilityClass;

import java.util.Set;

@UtilityClass
public class QueryParserUtil {

    private static final Set<Character> CHARACTER_SET =
        Set.of('\\', '+', '-', '!', '(', ')', ':', '^', '[', ']', '"', '{', '}', '~', '*', '?', '|', '&', '/');

    /**
     * Returns a String where those characters that TextParser expects to be escaped are escaped by a preceding \.
     * @param s expected to be escaped
     * @return escaped string
     */
    public static String escape(String s) {
        StringBuilder sb = new StringBuilder();

        for (char c : s.toCharArray()) {
            if (CHARACTER_SET.contains(c)) {
                sb.append('\\');
            }
            sb.append(c);
        }
        return sb.toString();
    }
}
