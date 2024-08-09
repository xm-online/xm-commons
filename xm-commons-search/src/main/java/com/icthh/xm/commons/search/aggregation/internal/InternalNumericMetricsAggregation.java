/*
 * Original version of this file is located at:
 * https://github.com/elastic/elasticsearch/blob/v6.4.3/server/src/main/java/org/elasticsearch/search/aggregations/metrics/InternalNumericMetricsAggregation.java
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
package com.icthh.xm.commons.search.aggregation.internal;

import com.icthh.xm.commons.search.aggregation.stats.NumericMetricsAggregation;
import com.icthh.xm.commons.search.builder.aggregation.support.DocValueFormat;

public abstract class InternalNumericMetricsAggregation extends InternalAggregation {

    private static final DocValueFormat DEFAULT_FORMAT = DocValueFormat.RAW;

    protected DocValueFormat format = DEFAULT_FORMAT;

    public abstract static class SingleValue extends InternalNumericMetricsAggregation implements NumericMetricsAggregation.SingleValue {
        protected SingleValue(String name) {
            super(name);
        }

        @Override
        public String getValueAsString() {
            return format.format(value()).toString();
        }

    }

    public abstract static class MultiValue extends InternalNumericMetricsAggregation implements NumericMetricsAggregation.MultiValue {
        protected MultiValue(String name) {
            super(name);
        }

        public abstract double value(String name);

        public String valueAsString(String name) {
            return format.format(value(name)).toString();
        }
    }

    private InternalNumericMetricsAggregation(String name) {
        super(name);
    }
}
