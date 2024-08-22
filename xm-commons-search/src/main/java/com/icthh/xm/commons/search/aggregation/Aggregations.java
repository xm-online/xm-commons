/*
 * Original version of this file is located at:
 * https://github.com/elastic/elasticsearch/blob/v6.4.3/server/src/main/java/org/elasticsearch/search/aggregations/Aggregations.java
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

package com.icthh.xm.commons.search.aggregation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.Collections.unmodifiableMap;

public class Aggregations implements Iterable<Aggregation> {

    public static final String AGGREGATIONS_FIELD = "aggregations";

    protected List<? extends Aggregation> aggregations;
    protected Map<String, Aggregation> aggregationsAsMap;


    public Aggregations(List<? extends Aggregation> aggregations) {
        this.aggregations = aggregations;
    }

    @Override
    public final Iterator<Aggregation> iterator() {
        return aggregations.stream().map((p) -> (Aggregation) p).iterator();
    }

    public final List<Aggregation> asList() {
        return Collections.unmodifiableList(aggregations);
    }

    public final Map<String, Aggregation> asMap() {
        return getAsMap();
    }

    public final Map<String, Aggregation> getAsMap() {
        if (aggregationsAsMap == null) {
            Map<String, Aggregation> newAggregationsAsMap = new HashMap<>(aggregations.size());
            for (Aggregation aggregation : aggregations) {
                newAggregationsAsMap.put(aggregation.getName(), aggregation);
            }
            this.aggregationsAsMap = unmodifiableMap(newAggregationsAsMap);
        }
        return aggregationsAsMap;
    }

    @SuppressWarnings("unchecked")
    public final <A extends Aggregation> A get(String name) {
        return (A) asMap().get(name);
    }

    @Override
    public final boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        return aggregations.equals(((Aggregations) obj).aggregations);
    }

    @Override
    public final int hashCode() {
        return Objects.hash(getClass(), aggregations);
    }
}
