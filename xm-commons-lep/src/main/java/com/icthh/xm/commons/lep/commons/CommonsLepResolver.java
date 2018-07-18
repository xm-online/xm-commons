package com.icthh.xm.commons.lep.commons;

import com.icthh.xm.commons.lep.SeparatorSegmentedLepKeyResolver;
import com.icthh.xm.commons.lep.XmLepConstants;
import com.icthh.xm.lep.api.LepKey;
import com.icthh.xm.lep.api.LepManagerService;
import com.icthh.xm.lep.api.LepMethod;
import com.icthh.xm.lep.api.commons.GroupMode;
import com.icthh.xm.lep.api.commons.SeparatorSegmentedLepKey;
import org.springframework.stereotype.Component;

@Component
public class CommonsLepResolver extends SeparatorSegmentedLepKeyResolver {
    @Override
    protected LepKey resolveKey(SeparatorSegmentedLepKey inBaseKey, LepMethod method, LepManagerService managerService) {
        String group = getRequiredParam(method, "group", String.class) + ".Commons";
        String name = getRequiredParam(method, "name", String.class);
        SeparatorSegmentedLepKey baseKey = new SeparatorSegmentedLepKey(group, XmLepConstants.EXTENSION_KEY_SEPARATOR, XmLepConstants.EXTENSION_KEY_GROUP_MODE);
        GroupMode groupMode = new GroupMode.Builder().prefixAndIdIncludeGroup(baseKey.getGroupSegmentsSize()).build();
        return baseKey.append(name, groupMode);
    }
}
