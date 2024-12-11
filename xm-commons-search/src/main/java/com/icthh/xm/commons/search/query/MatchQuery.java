package com.icthh.xm.commons.search.query;

public class MatchQuery {

    public enum ZeroTermsQuery {
        NONE(0),
        ALL(1);

        private final int ordinal;

        ZeroTermsQuery(int ordinal) {
            this.ordinal = ordinal;
        }

    }

    public static final boolean DEFAULT_LENIENCY = false;

    public static final ZeroTermsQuery DEFAULT_ZERO_TERMS_QUERY = ZeroTermsQuery.NONE;
}
