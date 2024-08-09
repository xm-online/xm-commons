/*
 * Original version of this file is located at:
 * https://github.com/spring-projects/spring-data-elasticsearch/blob/3.1.12.RELEASE/src/main/java/org/springframework/data/elasticsearch/core/ElasticsearchOperations.java
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
package com.icthh.xm.commons.search;

import com.icthh.xm.commons.search.dto.SearchDto;
import com.icthh.xm.commons.search.mapper.extractor.ResultsExtractor;
import com.icthh.xm.commons.search.query.SearchQuery;
import com.icthh.xm.commons.search.query.dto.DeleteQuery;
import com.icthh.xm.commons.search.query.dto.GetQuery;
import com.icthh.xm.commons.search.query.dto.IndexQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;

public interface ElasticsearchOperations {

    static final String INDEX_QUERY_TYPE = "xmentity";

    String composeIndexName(String tenantCode);

    String getIndexName();

    <T> boolean createIndex(Class<T> clazz);

    boolean createIndex(String indexName);

    boolean createIndex(String indexName, Object settings);

    <T> boolean putMapping(Class<T> clazz);

    boolean putMapping(String indexName, String type, Object mappings);

    <T> boolean putMapping(Class<T> clazz, Object mappings);

    <T> T queryForObject(GetQuery query, Class<T> clazz);

    <T> Page<T> queryForPage(SearchQuery query, Class<T> clazz);

    <T> List<T> queryForList(SearchQuery query, Class<T> clazz);

    <T> List<String> queryForIds(SearchQuery query);

    <T> long count(SearchQuery query, Class<T> clazz);

    <T> long count(SearchQuery query);

    String index(IndexQuery query);

    <T> boolean indexExists(Class<T> clazz);

    <T> boolean indexExists(String indexName);

    void bulkIndex(List<IndexQuery> queries);

    String delete(String indexName, String type, String id);

    <T> String delete(Class<T> clazz, String id);

    <T> void delete(DeleteQuery query, Class<T> clazz);

    void delete(DeleteQuery query);

    boolean deleteIndex(String indexName);

    <T> boolean deleteIndex(Class<T> clazz);

    void refresh(String tenantCode);

    <T> void refresh(Class<T> clazz);

    <T> Page<T> startScroll(long scrollTimeInMillis, SearchQuery query, Class<T> clazz);

    <T> Page<T> continueScroll(String scrollId, long scrollTimeInMillis, Class<T> clazz);

    <T> void clearScroll(String scrollId);

    <T> T query(SearchQuery query, ResultsExtractor<T> resultsExtractor);

    <T> List<T> search(String query, Class<T> entityClass, String privilegeKey);

    <T> Page<T> searchForPage(SearchDto searchDto, String privilegeKey);

    <T> Page<T> search(Long scrollTimeInMillis,
                       String query,
                       Pageable pageable,
                       Class<T> entityClass,
                       String privilegeKey);

    <T> Page<T> searchByQueryAndTypeKey(String query,
                                        String typeKey,
                                        Pageable pageable,
                                        Class<T> entityClass,
                                        String privilegeKey);

    public <T> Page<T> searchWithIdNotIn(String query,
                                         Set<Long> ids,
                                         String targetEntityTypeKey,
                                         Pageable pageable,
                                         Class<T> entityClass,
                                         String privilegeKey);
}
