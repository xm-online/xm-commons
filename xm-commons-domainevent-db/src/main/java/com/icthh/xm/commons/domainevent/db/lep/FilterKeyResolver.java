package com.icthh.xm.commons.domainevent.db.lep;

import com.icthh.xm.commons.lep.AppendLepKeyResolver;
import com.icthh.xm.lep.api.LepManagerService;
import com.icthh.xm.lep.api.LepMethod;
import com.icthh.xm.lep.api.commons.SeparatorSegmentedLepKey;
import org.springframework.stereotype.Component;

@Component
public class FilterKeyResolver extends AppendLepKeyResolver {

    @Override
    protected String[] getAppendSegments(SeparatorSegmentedLepKey baseKey, LepMethod method, LepManagerService managerService) {
        String key = getRequiredParam(method, "key", String.class);
        return new String[]{
            key.toUpperCase()
        };
    }

}
