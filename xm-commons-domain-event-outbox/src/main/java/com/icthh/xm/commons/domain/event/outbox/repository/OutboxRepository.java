package com.icthh.xm.commons.domain.event.outbox.repository;

import com.icthh.xm.commons.domain.event.outbox.domain.Outbox;
import com.icthh.xm.commons.domain.event.outbox.domain.RecordStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Transactional
public interface OutboxRepository extends JpaRepository<Outbox, UUID>, JpaSpecificationExecutor<Outbox> {

    @Modifying
    @Query("update Outbox o set o.status = :status where o.id = :id")
    void updateStatus(@Param("status") RecordStatus status, @Param("id") UUID id);

    @Modifying
    @Query("update Outbox o set o.status = :status where o.id in (:ids)")
    void updateStatus(@Param("status") RecordStatus status, @Param("ids") Iterable<UUID> ids);
}
