/*
 * Original version of this file is located at:
 * https://github.com/elastic/elasticsearch/blob/v6.4.3/server/src/main/java/org/elasticsearch/index/query/QueryBuilders.java
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

import com.icthh.xm.commons.search.enums.ScoreMode;

import java.util.Collection;

public final class QueryBuilders {

    private QueryBuilders() {
    }

    public static MatchAllQueryBuilder matchAllQuery() {
        return new MatchAllQueryBuilder();
    }

    public static MatchQueryBuilder matchQuery(String name, Object text) {
        return new MatchQueryBuilder(name, text);
    }

    public static CommonTermsQueryBuilder commonTermsQuery(String fieldName, Object text) {
        return new CommonTermsQueryBuilder(fieldName, text);
    }

    public static NestedQueryBuilder nestedQuery(String path, QueryBuilder query, ScoreMode scoreMode) {
        return new NestedQueryBuilder(path, query, scoreMode);
    }

    public static BoolQueryBuilder boolQuery() {
        return new BoolQueryBuilder();
    }

    public static QueryBuilder termQuery(String name, String value) {
        return new TermQueryBuilder(name, value);
    }

    public static TermQueryBuilder termQuery(String name, int value) {
        return new TermQueryBuilder(name, value);
    }

    public static TermQueryBuilder termQuery(String name, long value) {
        return new TermQueryBuilder(name, value);
    }

    public static TermQueryBuilder termQuery(String name, float value) {
        return new TermQueryBuilder(name, value);
    }

    public static TermQueryBuilder termQuery(String name, double value) {
        return new TermQueryBuilder(name, value);
    }

    public static TermQueryBuilder termQuery(String name, boolean value) {
        return new TermQueryBuilder(name, value);
    }

    public static TermQueryBuilder termQuery(String name, Object value) {
        return new TermQueryBuilder(name, value);
    }

    public static TermsQueryBuilder termsQuery(String name, String... values) {
        return new TermsQueryBuilder(name, values);
    }

    public static TermsQueryBuilder termsQuery(String name, int... values) {
        return new TermsQueryBuilder(name, values);
    }

    public static TermsQueryBuilder termsQuery(String name, long... values) {
        return new TermsQueryBuilder(name, values);
    }

    public static TermsQueryBuilder termsQuery(String name, float... values) {
        return new TermsQueryBuilder(name, values);
    }

    public static TermsQueryBuilder termsQuery(String name, double... values) {
        return new TermsQueryBuilder(name, values);
    }

    public static TermsQueryBuilder termsQuery(String name, Object... values) {
        return new TermsQueryBuilder(name, values);
    }

    public static TermsQueryBuilder termsQuery(String name, Collection<?> values) {
        return new TermsQueryBuilder(name, values);
    }

    public static QueryStringQueryBuilder queryStringQuery(String queryString) {
        return new QueryStringQueryBuilder(queryString);
    }

    public static SimpleQueryStringBuilder simpleQueryStringQuery(String queryString) {
        return new SimpleQueryStringBuilder(queryString);
    }

    public static PrefixQueryBuilder prefixQuery(String name, String prefix) {
        return new PrefixQueryBuilder(name, prefix);
    }

    public static ExistsQueryBuilder existsQuery(String name) {
        return new ExistsQueryBuilder(name);
    }

    public static RangeQueryBuilder rangeQuery(String name) {
        return new RangeQueryBuilder(name);
    }

    public static MatchPhrasePrefixQueryBuilder matchPhrasePrefixQuery(String name, Object text) {
        return new MatchPhrasePrefixQueryBuilder(name, text);
    }
}
