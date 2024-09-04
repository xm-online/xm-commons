/*
 * Original version of this file is located at:
 * https://github.com/elastic/elasticsearch/blob/v6.4.3/server/src/main/java/org/elasticsearch/index/query/RangeQueryBuilder.java
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

public class RangeQueryBuilder extends AbstractQueryBuilder<RangeQueryBuilder> {

    public static final String NAME = "range";

    public static final boolean DEFAULT_INCLUDE_UPPER = true;
    public static final boolean DEFAULT_INCLUDE_LOWER = true;

    private final String fieldName;

    private Object gt;

    private Object gte;

    private Object lt;

    private Object lte;

    private Object from;

    private Object to;

    private String timeZone;

    private boolean includeLower = DEFAULT_INCLUDE_LOWER;

    private boolean includeUpper = DEFAULT_INCLUDE_UPPER;

    private String format;

    public RangeQueryBuilder(String fieldName) {
        if (Strings.isEmpty(fieldName)) {
            throw new IllegalArgumentException("field name is null or empty");
        }
        this.fieldName = fieldName;
    }

    public String fieldName() {
        return this.fieldName;
    }

    public RangeQueryBuilder from(Object from, boolean includeLower) {
        this.from = maybeConvertToBytesRef(from);
        this.includeLower = includeLower;
        return this;
    }

    public RangeQueryBuilder from(Object from) {
        return from(from, this.includeLower);
    }

    public Object from() {
        return maybeConvertToString(this.from);
    }

    public RangeQueryBuilder gt(Object from) {
        this.gt = from;
        return from(from, false);
    }

    public Object getGt() {
        return this.gt;
    }

    public RangeQueryBuilder gte(Object from) {
        this.gte = from;
        return from(from, true);
    }

    public Object getGte() {
        return this.gte;
    }

    public RangeQueryBuilder to(Object to, boolean includeUpper) {
        this.to = maybeConvertToBytesRef(to);
        this.includeUpper = includeUpper;
        return this;
    }

    public RangeQueryBuilder to(Object to) {
        return to(to, this.includeUpper);
    }

    public Object to() {
        return maybeConvertToString(this.to);
    }

    public RangeQueryBuilder lt(Object to) {
        this.lt = to;
        return to(to, false);
    }

    public Object getLt() {
        return this.lt;
    }

    public RangeQueryBuilder lte(Object to) {
        this.lte = to;
        return to(to, true);
    }

    public Object getLte() {
        return this.lte;
    }

    public RangeQueryBuilder includeLower(boolean includeLower) {
        this.includeLower = includeLower;
        return this;
    }

    public boolean includeLower() {
        return this.includeLower;
    }

    public RangeQueryBuilder includeUpper(boolean includeUpper) {
        this.includeUpper = includeUpper;
        return this;
    }

    public boolean includeUpper() {
        return this.includeUpper;
    }

    public RangeQueryBuilder timeZone(String timeZone) {
        if (timeZone == null) {
            throw new IllegalArgumentException("timezone cannot be null");
        }
        this.timeZone = timeZone;
        return this;
    }

    public String timeZone() {
        return this.timeZone;
    }

    public RangeQueryBuilder format(String format) {
        if (format == null) {
            throw new IllegalArgumentException("format cannot be null");
        }
        this.format = format;
        return this;
    }

    public String format() {
        return this.format;
    }

    public String getWriteableName() {
        return NAME;
    }
}
