package com.icthh.xm.commons.lep.groovy;

/**
 * Default of the {@code groovy.antlr4.cache.threshold} system property.
 *
 * <p>Groovy reads the property once, in the static initializer of its parser {@code AtnManager}, loaded by the
 * first groovy parse of the JVM. With the groovy default (0) the parser DFA cache lives behind a SoftReference
 * and is dropped on any memory pressure, so a big compilation keeps re-paying the DFA warmup it just did.
 * A positive threshold pins the cache and clears it deterministically every N parsed files instead.
 */
public final class GroovyAntlrCacheThreshold {

    public static final String PROPERTY_NAME = "groovy.antlr4.cache.threshold";
    public static final String DEFAULT_THRESHOLD = "50000";

    private GroovyAntlrCacheThreshold() {
    }

    /**
     * Called from the static initializer of every class of this module that triggers groovy parsing, so the
     * default is in place before the first parse can load the {@code AtnManager}. A value provided by the user
     * ({@code -Dgroovy.antlr4.cache.threshold=...} or {@code System.setProperty} before the first parse) wins.
     */
    public static void applyDefault() {
        if (System.getProperty(PROPERTY_NAME) == null) {
            System.setProperty(PROPERTY_NAME, DEFAULT_THRESHOLD);
        }
    }
}
