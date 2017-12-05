package com.icthh.xm.commons.permission.service.translator;

import com.icthh.xm.commons.permission.access.subject.Subject;
import org.apache.commons.lang3.StringUtils;

public interface SpelTranslator {

    /**
     * Translate spel, and enrich with subject data.
     * @param spel the SPeL expression
     * @param subject the subject
     * @return translated expression
     */
    String translate(String spel, Subject subject);

    /**
     * Replace search string to data with quotes.
     *
     * <p>Example:
     *  jpql="big 'value' fat value"
     *  search="value"
     *  replace="frog"
     *
     * <p>result="big 'frog' fat 'frog'"
     *
     * @param jpql the jpql string
     * @param search the search string
     * @param replace the replacement string
     * @return jpql with data string
     */
    static String replaceWithQuotes(String jpql, String search, String replace) {
        if (StringUtils.isNotBlank(replace)) {
            return StringUtils.replaceAll(
                StringUtils.replaceAll(jpql, "'" + search + "'", "'" + replace + "'"),
                search, "'" + replace + "'");
        }
        return jpql;
    }

    /**
     * Apply subject variables to expression.
     * @param spel the spring expression
     * @param subject the subject
     * @return expression with subject data
     */
    static String applySubject(String spel, Subject subject) {
        String result = spel;
        result = replaceWithQuotes(result, "#subject.login", subject.getLogin());
        result = replaceWithQuotes(result, "#subject.userKey", subject.getUserKey());
        result = replaceWithQuotes(result, "#subject.role", subject.getRole());
        return result;
    }
}
