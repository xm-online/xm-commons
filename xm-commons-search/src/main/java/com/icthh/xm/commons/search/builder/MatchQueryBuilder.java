/*
 * Original version of this file is located at:
 * https://github.com/elastic/elasticsearch/blob/v6.4.3/server/src/main/java/org/elasticsearch/index/query/MatchQueryBuilder.java
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

import com.icthh.xm.commons.search.query.MatchQuery;
import com.icthh.xm.commons.search.query.Operator;
import org.apache.lucene.search.FuzzyQuery;

public class MatchQueryBuilder extends AbstractQueryBuilder<MatchQueryBuilder> {

    public static final String NAME = "match";

    public static final Operator DEFAULT_OPERATOR = Operator.OR;

    private final String fieldName;

    private final Object value;

    private Operator operator = DEFAULT_OPERATOR;

    private int prefixLength = FuzzyQuery.defaultPrefixLength;

    private int  maxExpansions = FuzzyQuery.defaultMaxExpansions;

    private boolean fuzzyTranspositions = FuzzyQuery.defaultTranspositions;

    private boolean lenient = MatchQuery.DEFAULT_LENIENCY;

    private MatchQuery.ZeroTermsQuery zeroTermsQuery = MatchQuery.DEFAULT_ZERO_TERMS_QUERY;

    private boolean autoGenerateSynonymsPhraseQuery = true;

    public MatchQueryBuilder(String fieldName, Object value) {
        if (fieldName == null) {
            throw new IllegalArgumentException("[" + NAME + "] requires fieldName");
        }
        if (value == null) {
            throw new IllegalArgumentException("[" + NAME + "] requires query value");
        }
        this.fieldName = fieldName;
        this.value = value;
    }

    public String getFieldName() {
        return this.fieldName;
    }

    public Object getValue() {
        return this.value;
    }

    public Operator getOperator() {
        return this.operator;
    }

    public int getPrefixLength() {
        return prefixLength;
    }

    public int getMaxExpansions() {
        return maxExpansions;
    }

    public boolean isFuzzyTranspositions() {
        return fuzzyTranspositions;
    }

    public boolean isLenient() {
        return lenient;
    }

    public MatchQuery.ZeroTermsQuery getZeroTermsQuery() {
        return zeroTermsQuery;
    }

    public boolean isAutoGenerateSynonymsPhraseQuery() {
        return autoGenerateSynonymsPhraseQuery;
    }

    public String getWriteableName() {
        return NAME;
    }
}
