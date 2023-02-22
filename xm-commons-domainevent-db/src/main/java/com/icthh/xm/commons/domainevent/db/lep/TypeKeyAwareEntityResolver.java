package com.icthh.xm.commons.domainevent.db.lep;

import com.icthh.xm.commons.domainevent.db.domain.JpaEntityContext;
import com.icthh.xm.commons.lep.AppendLepKeyResolver;
import com.icthh.xm.lep.api.LepManagerService;
import com.icthh.xm.lep.api.LepMethod;
import com.icthh.xm.lep.api.commons.SeparatorSegmentedLepKey;
import org.springframework.stereotype.Component;

import static com.icthh.xm.commons.domainevent.db.service.mapper.impl.TypeKeyAwareJpaEntityMapper.TYPE_KEY;

@Component
public class TypeKeyAwareEntityResolver extends AppendLepKeyResolver {

    @Override
    protected String[] getAppendSegments(SeparatorSegmentedLepKey baseKey, LepMethod method, LepManagerService managerService) {
        JpaEntityContext jpaEntityContext = getRequiredParam(method, "jpaEntityContext", JpaEntityContext.class);
        String typeKey = jpaEntityContext.findPropertyStateValue(TYPE_KEY);
        return new String[]{
            typeKey.toUpperCase()
        };
    }

}
