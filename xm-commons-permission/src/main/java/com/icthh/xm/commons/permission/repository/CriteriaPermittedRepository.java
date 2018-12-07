package com.icthh.xm.commons.permission.repository;

import com.icthh.xm.commons.permission.service.FilterConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

/**
 * Converts criteria to JPQL statement and performs permitted find in DB.
 * <p>
 * Should be used instead of generated find by criteria logic based on
 * {@link org.springframework.data.jpa.domain.Specification}
 * <p>
 * For now works only for RDBMS (Elasticsearch is not supported yet).
 */
@Slf4j
@RequiredArgsConstructor
@Repository
public class CriteriaPermittedRepository {

    private final PermittedRepository permittedRepository;

    /**
     * Find entities with applied filtering and dynamic permissions written in SpEL (rresource condition).
     *
     * @param type         Entity class
     * @param criteria     Filtering criteria from request
     * @param page         page
     * @param privilegeKey privilege key
     * @param <T>          Entity type
     * @return Entity page.
     */
    public <T> Page<T> findWithPermission(final Class<T> type,
                                          final Object criteria,
                                          final Pageable page,
                                          final String privilegeKey) {
        FilterConverter.QueryPart queryPart = FilterConverter.toJpql(criteria);

        Page<T> result;
        if (queryPart.isEmpty()) {
            result = permittedRepository.findAll(page, type, privilegeKey);
        } else {
            log.debug("find with condition: {}", queryPart);
            result = permittedRepository.findByCondition(queryPart.getQuery().toString(),
                queryPart.getParams(),
                page,
                type,
                privilegeKey);
        }
        return result;
    }
}
