package com.icthh.xm.commons.domain.event.service.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class ValidForDto {
    private Instant validFrom;
    private Instant validTo;
}
