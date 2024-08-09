/*
 * Original version of this file is located at:
 * https://github.com/elastic/elasticsearch/blob/v6.4.3/server/src/main/java/org/elasticsearch/index/query/SimpleQueryStringBuilder.java
 *
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.icthh.xm.commons.search.builder;

import com.icthh.xm.commons.search.query.Operator;
import lombok.NoArgsConstructor;
import org.apache.lucene.search.FuzzyQuery;

import java.util.HashMap;
import java.util.Map;

public class SimpleQueryStringBuilder extends AbstractQueryBuilder<SimpleQueryStringBuilder> {

    public static final boolean DEFAULT_LENIENT = false;

    public static final boolean DEFAULT_ANALYZE_WILDCARD = false;

    public static final Operator DEFAULT_OPERATOR = Operator.OR;

    public static final int DEFAULT_FUZZY_PREFIX_LENGTH = FuzzyQuery.defaultPrefixLength;

    public static final int DEFAULT_FUZZY_MAX_EXPANSIONS = FuzzyQuery.defaultMaxExpansions;

    public static final boolean DEFAULT_FUZZY_TRANSPOSITIONS = FuzzyQuery.defaultTranspositions;

    private final String queryText;
    private final Map<String, Float> fieldsAndWeights = new HashMap<>();
    private final Operator defaultOperator = DEFAULT_OPERATOR;
    private String minimumShouldMatch;
    private final Settings settings = new Settings();

    public SimpleQueryStringBuilder(String queryText) {
        if (queryText == null) {
            throw new IllegalArgumentException("query text missing");
        }
        this.queryText = queryText;
    }

    public String value() {
        return this.queryText;
    }

    public Map<String, Float> fields() {
        return this.fieldsAndWeights;
    }

    public String minimumShouldMatch() {
        return minimumShouldMatch;
    }

    public Operator defaultOperator() {
        return defaultOperator;
    }

    public String quoteFieldSuffix() {
        return settings.quoteFieldSuffix();
    }

    public boolean lenient() {
        return this.settings.lenient();
    }

    public boolean analyzeWildcard() {
        return this.settings.analyzeWildcard();
    }

    public boolean autoGenerateSynonymsPhraseQuery() {
        return settings.autoGenerateSynonymsPhraseQuery();
    }

    public int fuzzyPrefixLength() {
        return settings.fuzzyPrefixLength();
    }

    public int fuzzyMaxExpansions() {
        return settings.fuzzyMaxExpansions();
    }

    public boolean fuzzyTranspositions() {
        return settings.fuzzyTranspositions();
    }

    @NoArgsConstructor
    public static class Settings {

        private boolean lenient = DEFAULT_LENIENT;

        private boolean analyzeWildcard = DEFAULT_ANALYZE_WILDCARD;

        private String quoteFieldSuffix = null;

        private boolean autoGenerateSynonymsPhraseQuery = true;

        private int fuzzyPrefixLength = DEFAULT_FUZZY_PREFIX_LENGTH;

        private int fuzzyMaxExpansions = DEFAULT_FUZZY_MAX_EXPANSIONS;

        private boolean fuzzyTranspositions = DEFAULT_FUZZY_TRANSPOSITIONS;

        public boolean lenient() {
            return this.lenient;
        }

        public boolean analyzeWildcard() {
            return analyzeWildcard;
        }

        public String quoteFieldSuffix() {
            return quoteFieldSuffix;
        }

        public boolean autoGenerateSynonymsPhraseQuery() {
            return autoGenerateSynonymsPhraseQuery;
        }

        public int fuzzyPrefixLength() {
            return fuzzyPrefixLength;
        }

        public int fuzzyMaxExpansions() {
            return fuzzyMaxExpansions;
        }

        public boolean fuzzyTranspositions() {
            return fuzzyTranspositions;
        }
    }

}
