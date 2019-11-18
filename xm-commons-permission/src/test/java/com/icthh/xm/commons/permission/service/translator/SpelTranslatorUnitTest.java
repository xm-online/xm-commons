package com.icthh.xm.commons.permission.service.translator;

import com.icthh.xm.commons.permission.access.subject.Subject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SpelTranslatorUnitTest {

    @Test
    public void testReplaceSubject() {
        SpelTranslator spelTranslator = new SpelTranslator(){
            @Override
            public String translate(String spel, Subject subject) {
                return applySubject(spel, subject, "\"");
            }
        };
        Subject subject = new Subject("", "user key", "");
        String expected = "subject.userKey == \"user key\"";
        assertEquals(expected, spelTranslator.translate("subject.userKey == #subject.userKey", subject));
        assertEquals(expected, spelTranslator.translate("subject.userKey == '#subject.userKey'", subject));
        assertEquals(expected, spelTranslator.translate("subject.userKey == \"#subject.userKey\"", subject));
    }

}
