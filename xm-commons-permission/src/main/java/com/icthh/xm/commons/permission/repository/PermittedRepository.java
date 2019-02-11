package com.icthh.xm.commons.permission.repository;

import static java.lang.String.format;

import com.google.common.annotations.VisibleForTesting;
import com.icthh.xm.commons.permission.service.PermissionCheckService;
import com.icthh.xm.commons.permission.service.translator.SpelToJpqlTranslator;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.jpa.QueryHints;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.support.PageableExecutionUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Subgraph;
import javax.persistence.TypedQuery;

@Slf4j
@Repository
@RequiredArgsConstructor
public class PermittedRepository {

    public static final String SELECT_ALL_SQL = "select distinct returnObject from %s returnObject";
    public static final String COUNT_ALL_SQL = "select distinct count(returnObject) from %s returnObject";
    public static final String WHERE_SQL = " where ";
    public static final String AND_SQL = " and ";
    public static final String ORDER_BY_SQL = " order by ";
    private static final String GRAPH_DELIMETER = ".";

    private final SpelToJpqlTranslator spelToJpqlTranslator = new SpelToJpqlTranslator();

    private final PermissionCheckService permissionCheckService;

    @Setter(AccessLevel.PACKAGE)
    @VisibleForTesting
    @PersistenceContext
    private EntityManager em;

    /**
     * Find all permitted entities.
     * @param entityClass the entity class to get
     * @param privilegeKey the privilege key for permission lookup
     * @param <T> the type of entity
     * @return list of permitted entities
     */
    public <T> List<T> findAll(Class<T> entityClass, String privilegeKey) {
        return findAll(null, entityClass, privilegeKey).getContent();
    }

    /**
     * Find all pageable permitted entities.
     * @param pageable the page info
     * @param entityClass the entity class to get
     * @param privilegeKey the privilege key for permission lookup
     * @param <T> the type of entity
     * @return page of permitted entities
     */
    public <T> Page<T> findAll(Pageable pageable, Class<T> entityClass, String privilegeKey) {
        String selectSql = format(SELECT_ALL_SQL, entityClass.getSimpleName());
        String countSql = format(COUNT_ALL_SQL, entityClass.getSimpleName());

        String permittedCondition = createPermissionCondition(privilegeKey);
        if (StringUtils.isNotBlank(permittedCondition)) {
            selectSql += WHERE_SQL + permittedCondition;
            countSql += WHERE_SQL + permittedCondition;
        }

        log.debug("Executing SQL '{}'", selectSql);

        return execute(createCountQuery(countSql), pageable, createSelectQuery(selectSql, pageable, entityClass));
    }

    /**
     * Find permitted entities by parameters.
     * @param whereCondition the parameters condition
     * @param conditionParams the parameters map
     * @param entityClass the entity class to get
     * @param privilegeKey the privilege key for permission lookup
     * @param <T> the type of entity
     * @return list of permitted entities
     */
    public <T> List<T> findByCondition(String whereCondition,
                                       Map<String, Object> conditionParams,
                                       Class<T> entityClass,
                                       String privilegeKey) {
        return findByCondition(whereCondition, conditionParams, null, entityClass, privilegeKey).getContent();
    }

    /**
     * Find permitted entities by parameters.
     * @param whereCondition the parameters condition
     * @param conditionParams the parameters map
     * @param pageable the page info
     * @param entityClass the entity class to get
     * @param privilegeKey the privilege key for permission lookup
     * @param <T> the type of entity
     * @return page of permitted entities
     */
    public <T> Page<T> findByCondition(String whereCondition,
                                       Map<String, Object> conditionParams,
                                       Pageable pageable,
                                       Class<T> entityClass,
                                       String privilegeKey) {
        return findByCondition(whereCondition, conditionParams, null, pageable, entityClass, privilegeKey);
    }

    /**
     * Find permitted entities by parameters with embed graph.
     * @param whereCondition the parameters condition
     * @param conditionParams the parameters map
     * @param embed the embed list
     * @param pageable the page info
     * @param entityClass the entity class to get
     * @param privilegeKey the privilege key for permission lookup
     * @param <T> the type of entity
     * @return page of permitted entities
     */
    public <T> Page<T> findByCondition(String whereCondition,
                                        Map<String, Object> conditionParams,
                                        Collection<String> embed,
                                        Pageable pageable,
                                        Class<T> entityClass,
                                        String privilegeKey) {
        String selectSql = format(SELECT_ALL_SQL, entityClass.getSimpleName());
        String countSql = format(COUNT_ALL_SQL, entityClass.getSimpleName());

        selectSql += WHERE_SQL + whereCondition;
        countSql += WHERE_SQL + whereCondition;

        String permittedCondition = createPermissionCondition(privilegeKey);
        if (StringUtils.isNotBlank(permittedCondition)) {
            selectSql += AND_SQL + "(" + permittedCondition + ")";
            countSql += AND_SQL + "(" + permittedCondition + ")";
        }

        TypedQuery<T> selectQuery = createSelectQuery(selectSql, pageable, entityClass);
        if (!CollectionUtils.isEmpty(embed)) {
            selectQuery.setHint(QueryHints.HINT_LOADGRAPH, createEnitityGraph(embed, entityClass));
        }
        TypedQuery<Long> countQuery = createCountQuery(countSql);

        conditionParams.forEach((paramName, paramValue) -> {
            selectQuery.setParameter(paramName, paramValue);
            countQuery.setParameter(paramName, paramValue);
        });

        log.debug("Executing SQL '{}' with params '{}'", selectQuery, conditionParams);

        return execute(countQuery, pageable, selectQuery);
    }

    protected <T> TypedQuery<T> createSelectQuery(String selectSql, Pageable pageable, Class<T> entityClass) {
        Sort sort = pageable == null ? null : pageable.getSort();
        return em.createQuery(applyOrder(selectSql, sort), entityClass);
    }

    protected TypedQuery<Long> createCountQuery(String countSql) {
        return em.createQuery(countSql, Long.class);
    }

    protected <T> Page<T> execute(TypedQuery<Long> countSql, Pageable pageable, TypedQuery<T> query) {
        return pageable == null ? new PageImpl<>(query.getResultList())
            : readPage(countSql, query, pageable);
    }

    protected String createPermissionCondition(String privilegeKey) {
        return permissionCheckService.createCondition(
            SecurityContextHolder.getContext().getAuthentication(),
            privilegeKey,
            spelToJpqlTranslator);
    }

    private static String applyOrder(String sql, Sort sort) {
        StringBuilder builder = new StringBuilder(sql);

        if (sort != null && !sort.equals(Sort.unsorted())) {
            builder.append(ORDER_BY_SQL);
            String sep = "";
            for (Sort.Order order : sort) {
                builder.append(sep)
                    .append(order.getProperty())
                    .append(" ")
                    .append(order.getDirection());
                sep = ", ";
            }
        }

        return builder.toString();
    }

    private <T> Page<T> readPage(TypedQuery<Long> countQuery, TypedQuery<T> query, Pageable pageable) {
        query.setFirstResult(Math.toIntExact(pageable.getOffset()));
        query.setMaxResults(pageable.getPageSize());

        return PageableExecutionUtils.getPage(query.getResultList(), pageable,
            () -> executeCountQuery(countQuery));
    }

    private static Long executeCountQuery(TypedQuery<Long> query) {
        List<Long> totals = query.getResultList();
        Long total = 0L;

        for (Long element : totals) {
            total += element == null ? 0 : element;
        }

        return total;
    }

    private <T> EntityGraph<T> createEnitityGraph(Collection<String> embed, Class<T> domainClass) {
        EntityGraph<T> graph = em.createEntityGraph(domainClass);
        if (!CollectionUtils.isEmpty(embed)) {
            embed.forEach(f -> addAttributeNodes(f, graph));
        }
        return graph;
    }

    private static void addAttributeNodes(String fieldName, EntityGraph<?> graph) {
        int pos = fieldName.indexOf(GRAPH_DELIMETER);
        if (pos < 0) {
            graph.addAttributeNodes(fieldName);
            return;
        }

        String subgraphName = fieldName.substring(0, pos);
        Subgraph<?> subGraph = graph.addSubgraph(subgraphName);
        String nextFieldName = fieldName.substring(pos + 1);
        pos = nextFieldName.indexOf(GRAPH_DELIMETER);

        while (pos > 0) {
            subgraphName = nextFieldName.substring(0, pos);
            subGraph = subGraph.addSubgraph(subgraphName);
            nextFieldName = nextFieldName.substring(pos + 1);
            pos = nextFieldName.indexOf(GRAPH_DELIMETER);
        }

        subGraph.addAttributeNodes(nextFieldName);
    }
}
