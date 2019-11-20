package com.icthh.xm.commons.permission.service.translator;

import com.icthh.xm.commons.permission.access.subject.Subject;
import org.apache.commons.lang3.StringUtils;

import static org.apache.commons.lang3.StringUtils.replaceAll;

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
     *  quote="'"
     *
     * <p>result="big 'frog' fat 'frog'"
     *
     *
     * <p>Example:
     *  jpql="big "value" fat value"
     *  search="value"
     *  replace="frog"
     *  quote="'"
     *
     * <p>result="big 'frog' fat 'frog'"
     *
     * @param query the query string
     * @param search the search string
     * @param replace the replacement string
     * @return jpql with data string
     */
    default String replaceWithQuotes(String query, String search, String replace, String quote) {
        if (StringUtils.isNotBlank(replace)) {
            String replacement = quote + replace + quote;
            query = replaceAll(query, "'" + search + "'", replacement);
            query = replaceAll(query, "\"" + search + "\"", replacement);
            query = replaceAll(query, search, replacement);
        }
        return query;
    }

    /**
     * Apply subject variables to expression.
     * @param spel the spring expression
     * @param subject the subject
     * @return expression with subject data
     */
    default String applySubject(String spel, Subject subject) {
        return applySubject(spel, subject, "'");
    }

    /**
     * Apply subject variables to expression.
     * @param spel the spring expression
     * @param subject the subject
     * @param quote the quote that wrap subject value
     * @return expression with subject data
     */
    default String applySubject(String spel, Subject subject, String quote) {
        String result = spel;
        result = replaceWithQuotes(result, "#subject.login", subject.getLogin(), quote);
        result = replaceWithQuotes(result, "#subject.userKey", subject.getUserKey(), quote);
        result = replaceWithQuotes(result, "#subject.role", subject.getRole(), quote);
        return result;
    }
}
