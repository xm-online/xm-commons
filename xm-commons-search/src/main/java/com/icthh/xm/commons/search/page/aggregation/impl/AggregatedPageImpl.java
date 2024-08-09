/*
 * Original version of this file is located at:
 * https://github.com/spring-projects/spring-data-elasticsearch/blob/3.1.12.RELEASE/src/main/java/org/springframework/data/elasticsearch/core/aggregation/impl/AggregatedPageImpl.java
 *
 * Copyright 2017-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.icthh.xm.commons.search.page.aggregation.impl;

import com.icthh.xm.commons.search.aggregation.Aggregation;
import com.icthh.xm.commons.search.aggregation.Aggregations;
import com.icthh.xm.commons.search.page.aggregation.AggregatedPage;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

public class AggregatedPageImpl<T> extends PageImpl<T> implements AggregatedPage<T> {

    private final Aggregations aggregations;
    private final String scrollId;
    private final float maxScore;

    public AggregatedPageImpl(List<T> content, Pageable pageable, long total, Aggregations aggregations, String scrollId,
                              float maxScore) {
        super(content, pageable, total);
        this.aggregations = aggregations;
        this.scrollId = scrollId;
        this.maxScore = maxScore;
    }

    @Override
    public boolean hasAggregations() {
        return aggregations != null;
    }

    @Override
    public Aggregations getAggregations() {
        return aggregations;
    }

    @Override
    public Aggregation getAggregation(String name) {
        return aggregations == null ? null : aggregations.get(name);
    }

    @Override
    public String getScrollId() {
        return scrollId;
    }

    @Override
    public float getMaxScore() {
        return maxScore;
    }
}
