/*
 * Original version of this file is located at:
 * https://github.com/spring-projects/spring-data-elasticsearch/blob/3.1.12.RELEASE/src/main/java/org/springframework/data/elasticsearch/core/query/AbstractQuery.java
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

import com.icthh.xm.commons.search.filter.SourceFilter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Collections.addAll;

public abstract class AbstractQuery implements Query {

    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final Pageable DEFAULT_PAGE = PageRequest.of(0, DEFAULT_PAGE_SIZE);

    protected Pageable pageable = DEFAULT_PAGE;
    protected Sort sort;
    protected SourceFilter sourceFilter;
    protected List<String> indices = new ArrayList<>();
    protected List<String> types = new ArrayList<>();

    @Override
    public Pageable getPageable() {
        return this.pageable;
    }

    @Override
    public final <T extends Query> T setPageable(Pageable pageable) {
        Assert.notNull(pageable, "Pageable must not be null!");

        this.pageable = pageable;

        return (T) this.addSort(pageable.getSort());
    }

    @Override
    public List<String> getIndices() {
        return this.indices;
    }

    @Override
    public final <T extends Query> T addSort(Sort sort) {
        if (sort == null) {
            return (T) this;
        }

        if (this.sort == null) {
            this.sort = sort;
        } else {
            this.sort = this.sort.and(sort);
        }

        return (T) this;
    }

    @Override
    public Sort getSort() {
        return this.sort;
    }

    @Override
    public void addIndices(String ... indices) {
        Collections.addAll(this.indices, indices);
    }

    public void addSourceFilter(SourceFilter sourceFilter) {
        this.sourceFilter = sourceFilter;
    }

    @Override
    public void addTypes(String... types) {
        addAll(this.types, types);
    }

    @Override
    public List<String> getTypes() {
        return types;
    }

    @Override
    public SourceFilter getSourceFilter() {
        return sourceFilter;
    }
}
