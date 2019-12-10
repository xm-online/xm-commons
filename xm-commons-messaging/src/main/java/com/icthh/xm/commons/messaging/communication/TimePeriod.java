package com.icthh.xm.commons.messaging.communication;

import lombok.Data;

import java.time.OffsetDateTime;

/**
 * A base / value business entity used to represent a period of time between two timepoints.
 */
@Data
public class TimePeriod {

    private OffsetDateTime endDateTime;
    private OffsetDateTime startDateTime;
}

