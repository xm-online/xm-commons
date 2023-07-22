package com.icthh.xm.commons.lep;

import com.icthh.xm.lep.api.LepKey;
import com.icthh.xm.lep.api.LepManagerService;
import com.icthh.xm.lep.api.LepMethod;
import com.icthh.xm.lep.api.commons.GroupMode;
import com.icthh.xm.lep.api.commons.GroupModeType;
import com.icthh.xm.lep.api.commons.SeparatorSegmentedLepKey;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * The {@link AppendLepKeyResolver} class.
 */
public abstract class AppendLepKeyResolver extends SeparatorSegmentedLepKeyResolver {

    private static final Set<GroupModeType> SUPPORTED_GROUP_MODES = new HashSet<>(Arrays.asList(
        GroupModeType.PREFIX_EXCLUDE_LAST_SEGMENTS,
        GroupModeType.PREFIX
    ));

    @Override
    protected LepKey resolveKey(SeparatorSegmentedLepKey baseKey, LepMethod method, LepManagerService managerService) {
        GroupModeType baseKeyGroupMode = baseKey.getGroupMode().getType();
        if (!SUPPORTED_GROUP_MODES.contains(baseKeyGroupMode)) {
            throw new IllegalArgumentException("Base key unsupported group mode: " + baseKeyGroupMode);
        }

        String[] appendSegments = getAppendSegments(baseKey, method, managerService);

        GroupMode groupMode = new GroupMode.Builder().prefixAndIdIncludeGroup(baseKey.getGroupSegmentsSize()).build();
        return baseKey.append(appendSegments, groupMode);
    }

    protected abstract String[] getAppendSegments(SeparatorSegmentedLepKey baseKey,
                                                  LepMethod method,
                                                  LepManagerService managerService);

}
