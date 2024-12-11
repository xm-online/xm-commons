/*
 * Original version of this file is located at:
 * https://github.com/elastic/elasticsearch/blob/v6.4.3/server/src/main/java/org/elasticsearch/search/aggregations/bucket/terms/InternalTerms.java
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
package com.icthh.xm.commons.search.aggregation.terms;

import com.icthh.xm.commons.search.aggregation.Aggregations;
import com.icthh.xm.commons.search.aggregation.internal.InternalAggregations;
import com.icthh.xm.commons.search.aggregation.internal.InternalMultiBucketAggregation;
import com.icthh.xm.commons.search.builder.aggregation.support.DocValueFormat;

import java.util.List;

public abstract class InternalTerms<A extends InternalTerms<A, B>, B extends InternalTerms.Bucket<B>>
    extends InternalMultiBucketAggregation<A, B> implements Terms {

    protected InternalTerms(String name) {
        super(name);
    }

    public abstract static class Bucket<B extends Bucket<B>> extends InternalBucket
        implements Terms.Bucket {

        protected long docCount;
        protected long docCountError;
        protected InternalAggregations aggregations;
        protected final boolean showDocCountError;
        protected final DocValueFormat format;

        protected Bucket(long docCount, InternalAggregations aggregations, boolean showDocCountError, long docCountError,
                         DocValueFormat formatter) {
            this.showDocCountError = showDocCountError;
            this.format = formatter;
            this.docCount = docCount;
            this.aggregations = aggregations;
            this.docCountError = docCountError;
        }

        @Override
        public long getDocCount() {
            return docCount;
        }

        @Override
        public Aggregations getAggregations() {
            return aggregations;
        }
    }

    public abstract List<B> getBuckets();
}
