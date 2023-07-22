package com.icthh.xm.lep.api;

import java.util.List;

/**
 * Interface for build dynamic LEP keys.
 */
public interface LepKeyResolver {
    default String group(LepMethod method) {
        return method.getLepBaseKey().getGroup();
    }
    List<String> segments(LepMethod method);
}
