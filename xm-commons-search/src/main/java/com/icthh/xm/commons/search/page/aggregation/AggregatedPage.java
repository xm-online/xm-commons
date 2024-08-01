/*
 * Original version of this file is located at:
 * https://github.com/spring-projects/spring-data-elasticsearch/blob/3.1.12.RELEASE/src/main/java/org/springframework/data/elasticsearch/core/aggregation/AggregatedPage.java
 *
 * Copyright 2016-2019 the original author or authors.
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
package com.icthh.xm.commons.search.page.aggregation;

import com.icthh.xm.commons.search.aggregation.Aggregation;
import com.icthh.xm.commons.search.aggregation.Aggregations;
import com.icthh.xm.commons.search.page.ScoredPage;
import com.icthh.xm.commons.search.page.ScrolledPage;

public interface AggregatedPage<T> extends ScrolledPage<T>, ScoredPage<T> {

    boolean hasAggregations();

    Aggregations getAggregations();

    Aggregation getAggregation(String name);
}