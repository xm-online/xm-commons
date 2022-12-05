package com.icthh.xm.commons.domainevent.domain;

import lombok.Data;

import java.time.Instant;

@Data
public class ValidFor {
    private Instant validFrom;
    private Instant validTo;
}
