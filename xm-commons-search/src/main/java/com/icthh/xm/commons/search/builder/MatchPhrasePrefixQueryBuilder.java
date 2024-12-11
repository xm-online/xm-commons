/*
 * Original version of this file is located at:
 * https://github.com/elastic/elasticsearch/blob/v6.4.3/server/src/main/java/org/elasticsearch/index/query/MatchPhrasePrefixQueryBuilder.java
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

public class MatchPhrasePrefixQueryBuilder extends AbstractQueryBuilder<MatchPhrasePrefixQueryBuilder> {

    public static final String NAME = "match_phrase_prefix";

    private final String fieldName;

    private final Object value;

    public MatchPhrasePrefixQueryBuilder(String fieldName, Object value) {
        if (fieldName == null) {
            throw new IllegalArgumentException("[" + NAME + "] requires fieldName");
        }
        if (value == null) {
            throw new IllegalArgumentException("[" + NAME + "] requires query value");
        }
        this.fieldName = fieldName;
        this.value = value;
    }

    public String fieldName() {
        return this.fieldName;
    }

    public Object value() {
        return this.value;
    }

    public String getWriteableName() {
        return NAME;
    }
}
