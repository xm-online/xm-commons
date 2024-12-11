package com.icthh.xm.commons.permission.repository;

import com.icthh.xm.commons.permission.service.PermissionCheckService;
import com.icthh.xm.commons.permission.service.translator.SpelToJpqlTranslator;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Subgraph;
import jakarta.persistence.TypedQuery;
import lombok.Value;
import org.hibernate.jpa.QueryHints;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PermittedRepositoryUnitTest {

    @InjectMocks
    private PermittedRepository repository;
    @Mock
    private PermissionCheckService permissionCheckService;
    @Mock
    private EntityManager em;
    @Mock
    private TypedQuery selectQuery;
    @Mock
    private TypedQuery countQuery;
    @Mock
    private EntityGraph entityGraph;
    @Mock
    private Subgraph subgraph;

    @Before
    public void before() {
        repository.setEm(em);
    }

    @Test
    public void findByConditionWithoutEmbed() {
        when(em.createQuery("select distinct returnObject from TestEntity returnObject where a = :b and (f = g) order by d ASC", TestEntity.class)).thenReturn(selectQuery);
        when(em.createQuery("select distinct count(returnObject) from TestEntity returnObject where a = :b and (f = g)", Long.class)).thenReturn(countQuery);
        when(
            permissionCheckService.createCondition(
                eq(SecurityContextHolder.getContext().getAuthentication()),
                eq("TEST"),
                any(SpelToJpqlTranslator.class)
            )
        ).thenReturn("(f = g)");
        when(selectQuery.getResultList()).thenReturn(asList(new TestEntity(1), new TestEntity(2)));

        Page<TestEntity> result = repository.findByCondition(
            "a = :b",
            Collections.singletonMap("b", "bbb"),
            null,
            PageRequest.of(5, 10, Sort.by(Sort.DEFAULT_DIRECTION, "d")),
            TestEntity.class,
            "TEST"
        );

        assertThat(result).isNotNull();
        assertThat(result.getTotalPages()).isEqualTo(6);
        assertThat(result.getTotalElements()).isEqualTo(52);
        assertThat(result.getContent()).containsExactly(new TestEntity(1), new TestEntity(2));

        verify(em, times(0)).createEntityGraph(any(Class.class));
        verify(selectQuery, times(0)).setHint(QueryHints.HINT_LOADGRAPH, entityGraph);
    }

    @Test
    public void findByConditionWithEmbed() {
        when(em.createEntityGraph(TestEntity.class)).thenReturn(entityGraph);
        when(em.createQuery("select distinct returnObject from TestEntity returnObject where a = :b and (f = g) order by d ASC", TestEntity.class)).thenReturn(selectQuery);
        when(em.createQuery("select distinct count(returnObject) from TestEntity returnObject where a = :b and (f = g)", Long.class)).thenReturn(countQuery);
        when(entityGraph.addSubgraph("d")).thenReturn(subgraph);
        when(
            permissionCheckService.createCondition(
                eq(SecurityContextHolder.getContext().getAuthentication()),
                eq("TEST"),
                any(SpelToJpqlTranslator.class)
            )
        ).thenReturn("(f = g)");
        when(selectQuery.getResultList()).thenReturn(asList(new TestEntity(1), new TestEntity(2)));

        Page<TestEntity> result = repository.findByCondition(
            "a = :b",
            Collections.singletonMap("b", "bbb"),
            List.of("c", "d.e"),
            PageRequest.of(5, 10, Sort.by(Sort.DEFAULT_DIRECTION, "d")),
            TestEntity.class,
            "TEST"
        );

        assertThat(result).isNotNull();
        assertThat(result.getTotalPages()).isEqualTo(6);
        assertThat(result.getTotalElements()).isEqualTo(52);
        assertThat(result.getContent()).containsExactly(new TestEntity(1), new TestEntity(2));

        verify(selectQuery).setHint(QueryHints.HINT_LOADGRAPH, entityGraph);
        verify(selectQuery).setParameter("b", "bbb");
        verify(entityGraph).addAttributeNodes("c");
        verify(entityGraph).addSubgraph("d");
        verify(subgraph).addAttributeNodes("e");
    }

    @Test
    public void shouldFindAllWithMultiConditions() {
        when(em.createQuery(anyString(), any())).thenReturn(selectQuery);
        when(em.createQuery(anyString(), any())).thenReturn(countQuery);

        doReturn("(a = b) OR (v = g)").when(permissionCheckService).createCondition(any(), any(), any());

        repository.findAll(null, PermittedRepositoryUnitTest.class, "key");

        verify(em, times(1)).createQuery("select distinct count(returnObject) from PermittedRepositoryUnitTest returnObject where (a = b) OR (v = g)", Long.class);
        verify(em, times(1)).createQuery("select distinct returnObject from PermittedRepositoryUnitTest returnObject where (a = b) OR (v = g)", PermittedRepositoryUnitTest.class);
    }

    @Value
    private static class TestEntity {
        private int id;
    }
}
