package com.icthh.xm.commons.domainevent.service;

import com.icthh.xm.commons.domainevent.outbox.domain.Outbox;
import com.icthh.xm.commons.domainevent.outbox.domain.RecordStatus;
import com.icthh.xm.commons.domainevent.outbox.repository.OutboxRepository;
import com.icthh.xm.commons.domainevent.outbox.service.OutboxTransportService;
import com.icthh.xm.commons.domainevent.outbox.service.mapper.DomainEventMapper;
import com.icthh.xm.lep.api.LepManager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OutboxTransportServiceTest {

    private OutboxTransportService outboxTransportService;

    @Mock
    private LepManager lepManager;
    @Mock
    private OutboxRepository outboxRepository;
    @Mock
    private DomainEventMapper domainEventMapper;
    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        outboxTransportService = new OutboxTransportService(lepManager, outboxRepository, domainEventMapper);
    }

    @Test
    public void shouldFindAllDomainEvents() {
        PageRequest pageRequest = PageRequest.of(0, 10);
        PageImpl<Outbox> resultPage = new PageImpl<>(List.of(new Outbox(), new Outbox()));
        Specification<Outbox> filter = Specification.where(null);
        when(outboxRepository.findAll(eq(filter), eq(pageRequest))).thenReturn(resultPage);
        outboxTransportService.findAll(filter, pageRequest);
        verify(outboxRepository, times(1)).findAll(eq(filter), eq(pageRequest));
        verify(domainEventMapper, times(resultPage.getNumberOfElements())).toEntity(any());
    }

    @Test
    public void shouldChangeStatus() {
        RecordStatus requestStatus = RecordStatus.COMPLETE;
        List<UUID> requestUuids = List.of(UUID.randomUUID(), UUID.randomUUID());
        outboxTransportService.changeStatus(requestStatus, requestUuids);
        verify(outboxRepository, times(1)).updateStatus(eq(requestStatus), eq(requestUuids));
    }

    @Test
    public void shouldChangeStatusById() {
        RecordStatus requestStatus = RecordStatus.COMPLETE;
        UUID requestUuid = UUID.randomUUID();
        outboxTransportService.changeStatusById(requestStatus, requestUuid);
        verify(outboxRepository, times(1)).updateStatus(eq(requestStatus), eq(requestUuid));
    }

}
