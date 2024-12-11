/*
 * Original version of this file is located at:
 * https://github.com/elastic/elasticsearch/blob/v6.4.3/server/src/main/java/org/elasticsearch/index/query/QueryStringQueryBuilder.java
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
import org.apache.lucene.search.FuzzyQuery;

import java.util.Map;
import java.util.TreeMap;

public class QueryStringQueryBuilder extends AbstractQueryBuilder<QueryStringQueryBuilder> {

    public static final String NAME = "query_string";

    public static final Operator DEFAULT_OPERATOR = Operator.OR;
    public static final int DEFAULT_FUZZY_PREFIX_LENGTH = FuzzyQuery.defaultPrefixLength;
    public static final int DEFAULT_FUZZY_MAX_EXPANSIONS = FuzzyQuery.defaultMaxExpansions;
    public static final boolean DEFAULT_FUZZY_TRANSPOSITIONS = FuzzyQuery.defaultTranspositions;

    private final Map<String, Float> fieldsAndWeights = new TreeMap<>();

    private final String queryString;

    private int fuzzyPrefixLength = DEFAULT_FUZZY_PREFIX_LENGTH;

    private int fuzzyMaxExpansions = DEFAULT_FUZZY_MAX_EXPANSIONS;

    private boolean fuzzyTranspositions = DEFAULT_FUZZY_TRANSPOSITIONS;

    private Operator defaultOperator = DEFAULT_OPERATOR;

    private boolean autoGenerateSynonymsPhraseQuery = true;

    public QueryStringQueryBuilder(String queryString) {
        if (queryString == null) {
            throw new IllegalArgumentException("query text missing");
        } else {
            this.queryString = queryString;
        }
    }

    public String getQueryString() {
        return this.queryString;
    }

    public int getFuzzyPrefixLength() {
        return fuzzyPrefixLength;
    }

    public int getFuzzyMaxExpansions() {
        return fuzzyMaxExpansions;
    }

    public boolean isFuzzyTranspositions() {
        return fuzzyTranspositions;
    }

    public Operator getDefaultOperator() {
        return defaultOperator;
    }

    public boolean isAutoGenerateSynonymsPhraseQuery() {
        return autoGenerateSynonymsPhraseQuery;
    }

    public String getWriteableName() {
        return NAME;
    }

    public QueryStringQueryBuilder field(String field) {
        this.fieldsAndWeights.put(field, AbstractQueryBuilder.DEFAULT_BOOST);
        return this;
    }

    public Map<String, Float> fields() {
        return this.fieldsAndWeights;
    }
}
