/*
 * Original version of this file is located at:
 * https://github.com/elastic/elasticsearch/blob/v6.4.3/server/src/main/java/org/elasticsearch/index/query/TermsQueryBuilder.java
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

import com.icthh.xm.commons.search.common.Strings;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TermsQueryBuilder extends AbstractQueryBuilder<TermsQueryBuilder> {

    private final String fieldName;
    private final List<Object> values;

    public TermsQueryBuilder(String fieldName, String... values) {
        this(fieldName, values != null ? Arrays.asList(values) : null);
    }

    public TermsQueryBuilder(String fieldName, int... values) {
        this(fieldName, values != null ? Arrays.stream(values).boxed().collect(Collectors.toList()) : null);
    }

    public TermsQueryBuilder(String fieldName, long... values) {
        this(fieldName, values != null ? Arrays.stream(values).boxed().collect(Collectors.toList()) : null);
    }

    public TermsQueryBuilder(String fieldName, float... values) {
        this(fieldName, values != null ? IntStream.range(0, values.length)
                           .mapToObj(i -> values[i]).collect(Collectors.toList()) : null);
    }

    public TermsQueryBuilder(String fieldName, double... values) {
        this(fieldName, values != null ? Arrays.stream(values).boxed().collect(Collectors.toList()) : null);
    }

    public TermsQueryBuilder(String fieldName, Object... values) {
        this(fieldName, values != null ? Arrays.stream(values).collect(Collectors.toList()) : null);
    }

    public TermsQueryBuilder(String fieldName, Collection<?> values) {
        this(fieldName, values != null ? convertCollection(values) : null);
    }

    public TermsQueryBuilder(String fieldName, List<Object> values) {
        if (Strings.isEmpty(fieldName)) {
            throw new IllegalArgumentException("field name cannot be null.");
        }
        if (values == null) {
            throw new IllegalArgumentException("No value specified for terms query");
        }
        this.fieldName = fieldName;
        this.values = values;
    }

    public String fieldName() {
        return this.fieldName;
    }

    public List<Object> values() {
        return this.values;
    }

    private static <T> List<Object> convertCollection(Collection<T> inputCollection) {
        return inputCollection.stream()
            .map(item -> (Object) item)
            .collect(Collectors.toList());
    }

}
