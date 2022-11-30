package com.icthh.xm.commons.domain.event.outbox.domain;

public enum RecordStatus {
    NEW,
    PROCESSING,
    COMPLETE,
    ERROR
}
