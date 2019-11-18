package com.icthh.xm.commons.permission.service.translator;

import com.icthh.xm.commons.permission.access.subject.Subject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class SpelToJpqlTranslator implements SpelTranslator {

    @Override
    public String translate(String spel, Subject subject) {
        if (StringUtils.isNotBlank(spel)) {

            String jpql = StringUtils.replaceAll(spel, "#returnObject", "returnObject");
            jpql = replaceOperators(jpql);
            jpql = applySubject(jpql, subject);

            log.debug("SpEL was translated to JPQL for permission filtering: [{}] --> [{}]", spel, jpql);
            return jpql;
        }
        return spel;
    }

    /**
     * Replace SPEL ==, &&, || to SQL =, and, or .
     * @param spel the spring expression
     * @return sql expression
     */
    private static String replaceOperators(String spel) {
        if (StringUtils.isBlank(spel)) {
            return spel;
        }
        return spel.replaceAll("==", " = ")
            .replaceAll("&&", " and ")
            .replaceAll("\\|\\|", " or ");
    }
}
