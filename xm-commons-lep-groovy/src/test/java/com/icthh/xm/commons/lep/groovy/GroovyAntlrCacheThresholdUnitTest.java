package com.icthh.xm.commons.lep.groovy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.icthh.xm.commons.lep.groovy.GroovyAntlrCacheThreshold.DEFAULT_THRESHOLD;
import static com.icthh.xm.commons.lep.groovy.GroovyAntlrCacheThreshold.PROPERTY_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class GroovyAntlrCacheThresholdUnitTest {

    private String originalValue;

    @BeforeEach
    void saveProperty() {
        originalValue = System.getProperty(PROPERTY_NAME);
    }

    @AfterEach
    void restoreProperty() {
        if (originalValue == null) {
            System.clearProperty(PROPERTY_NAME);
        } else {
            System.setProperty(PROPERTY_NAME, originalValue);
        }
    }

    @Test
    void appliesDefaultWhenPropertyIsAbsent() {
        System.clearProperty(PROPERTY_NAME);

        GroovyAntlrCacheThreshold.applyDefault();

        assertEquals(DEFAULT_THRESHOLD, System.getProperty(PROPERTY_NAME));
    }

    @Test
    void keepsValueProvidedByUser() {
        System.setProperty(PROPERTY_NAME, "12345");

        GroovyAntlrCacheThreshold.applyDefault();

        assertEquals("12345", System.getProperty(PROPERTY_NAME));
    }
}
