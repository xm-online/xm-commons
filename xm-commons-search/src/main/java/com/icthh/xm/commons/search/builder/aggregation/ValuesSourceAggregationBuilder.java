/*
 * Original version of this file is located at:
 * https://github.com/elastic/elasticsearch/blob/v6.4.3/server/src/main/java/org/elasticsearch/search/aggregations/support/ValuesSourceAggregationBuilder.java
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
package com.icthh.xm.commons.search.builder.aggregation;

import com.icthh.xm.commons.search.builder.aggregation.support.ValueType;
import com.icthh.xm.commons.search.builder.aggregation.support.ValuesSource;
import com.icthh.xm.commons.search.builder.aggregation.support.ValuesSourceType;

public abstract class ValuesSourceAggregationBuilder<VS extends ValuesSource, AB extends ValuesSourceAggregationBuilder<VS, AB>> extends AbstractAggregationBuilder<AB> {
    private final ValuesSourceType valuesSourceType;
    private final ValueType targetValueType;
    private String field = null;

    protected ValuesSourceAggregationBuilder(String name, ValuesSourceType valuesSourceType, ValueType targetValueType) {
        super(name);
        if (valuesSourceType == null) {
            throw new IllegalArgumentException("[valuesSourceType] must not be null: [" + name + "]");
        } else {
            this.valuesSourceType = valuesSourceType;
            this.targetValueType = targetValueType;
        }
    }

    public AB field(String field) {
        if (field == null) {
            throw new IllegalArgumentException("[field] must not be null: [" + this.name + "]");
        } else {
            this.field = field;
            return (AB) this;
        }
    }

    public String field() {
        return this.field;
    }
}
