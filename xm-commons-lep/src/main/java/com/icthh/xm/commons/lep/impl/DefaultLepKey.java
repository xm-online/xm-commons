package com.icthh.xm.commons.lep.impl;

import com.icthh.xm.commons.lep.api.LepKey;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class DefaultLepKey implements LepKey {

    private final String group;
    private final String baseKey;
    private final List<String> segments;

    public DefaultLepKey(String group, String baseKey) {
        this(group, baseKey, Collections.emptyList());
    }

    @Override
    public String toString() {
        return "lep://" + group + "/" + baseKey + "[" + StringUtils.join(segments, ",") + "]";
    }
}
