package com.icthh.xm.lep.api;

import java.util.List;

/**
 * Interface for build dynamic LEP keys.
 */
public interface LepKeyResolver {

    /**
     * Resolve dynamic part of LEP key by method arguments.
     *
     * @param baseKey        base LEP key (prefix), can be {@code null}
     * @param method         method data on what LEP call occurs
     * @param managerService LEP manager service
     * @deprecated user methods group or segments
     * @return complete LEP key (baseKey + dynamic part)
     */
    @Deprecated(forRemoval = true)
    default LepKey resolve(LepKey baseKey,
                   LepMethod method,
                   LepManagerService managerService) {
        return baseKey;
    }

    default String group(LepMethod method) {
        return null;
    }

    default List<String> segments(LepMethod method) {
        return null;
    }

}
