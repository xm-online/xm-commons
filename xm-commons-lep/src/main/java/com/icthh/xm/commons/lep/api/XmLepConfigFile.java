package com.icthh.xm.commons.lep.api;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.time.Instant;

@Getter
@ToString(exclude = "content")
@RequiredArgsConstructor
// immutable: very important!
public class XmLepConfigFile {
    private final String path;
    private final String content;
    private final Instant updateDate = Instant.now();
}
