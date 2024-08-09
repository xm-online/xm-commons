/*
 * Original version of this file is located at:
 * https://github.com/elastic/elasticsearch/blob/v6.4.3/server/src/main/java/org/elasticsearch/search/aggregations/bucket/terms/TermsAggregationBuilder.java
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

public class TermsAggregationBuilder extends ValuesSourceAggregationBuilder<ValuesSource, TermsAggregationBuilder> {

    protected static final TermsAggregator.BucketCountThresholds DEFAULT_BUCKET_COUNT_THRESHOLDS = new TermsAggregator.BucketCountThresholds(1L, 0L, 10, -1);

    private TermsAggregator.BucketCountThresholds bucketCountThresholds;
    private boolean showTermDocCountError;

    public TermsAggregationBuilder(String name, ValueType valueType) {
        super(name, ValuesSourceType.ANY, valueType);
        this.bucketCountThresholds = new TermsAggregator.BucketCountThresholds(DEFAULT_BUCKET_COUNT_THRESHOLDS);
        this.showTermDocCountError = false;
    }

    public TermsAggregationBuilder size(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("[size] must be greater than 0. Found [" + size + "] in [" + this.name + "]");
        } else {
            this.bucketCountThresholds.setRequiredSize(size);
            return this;
        }
    }

    public int size() {
        return this.bucketCountThresholds.getRequiredSize();
    }

    public boolean showTermDocCountError() {
        return showTermDocCountError;
    }

    public TermsAggregationBuilder showTermDocCountError(boolean showTermDocCountError) {
        this.showTermDocCountError = showTermDocCountError;
        return this;
    }

    public long minDocCount() {
        return bucketCountThresholds.getMinDocCount();
    }

    public int shardSize() {
        return bucketCountThresholds.getShardSize();
    }
}
