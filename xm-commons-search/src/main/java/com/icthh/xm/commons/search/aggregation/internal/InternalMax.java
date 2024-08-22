/*
 * Original version of this file is located at:
 * https://github.com/elastic/elasticsearch/blob/v6.4.3/server/src/main/java/org/elasticsearch/search/aggregations/metrics/max/InternalMax.java
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

import com.icthh.xm.commons.search.aggregation.stats.Max;
import com.icthh.xm.commons.search.builder.aggregation.support.DocValueFormat;

public class InternalMax extends InternalNumericMetricsAggregation.SingleValue implements Max {
    private final double max;

    public InternalMax(String name, double max, DocValueFormat formatter) {
        super(name);
        this.format = formatter;
        this.max = max;
    }

    @Override
    public double value() {
        return max;
    }

    @Override
    public double getValue() {
        return max;
    }
}
