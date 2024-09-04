/*
 * Original version of this file is located at:
 * https://github.com/spring-projects/spring-data-elasticsearch/blob/3.1.12.RELEASE/src/main/java/org/springframework/data/elasticsearch/core/query/SearchQuery.java
 *
 * Copyright 2013-2019 the original author or authors.
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

package com.icthh.xm.commons.search.query;

import com.icthh.xm.commons.search.builder.QueryBuilder;
import com.icthh.xm.commons.search.builder.aggregation.AbstractAggregationBuilder;
import com.icthh.xm.commons.search.sort.SortBuilder;

import java.util.List;

public interface SearchQuery extends Query {

    QueryBuilder getQuery();

    QueryBuilder getFilter();

    List<SortBuilder> getElasticsearchSorts();

    List<AbstractAggregationBuilder> getAggregations();
}
