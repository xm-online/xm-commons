package com.icthh.xm.commons.domainevent.service.db;

import com.icthh.xm.commons.domainevent.domain.JpaEntityContext;
import com.icthh.xm.commons.lep.AppendLepKeyResolver;
import com.icthh.xm.lep.api.LepManagerService;
import com.icthh.xm.lep.api.LepMethod;
import com.icthh.xm.lep.api.commons.SeparatorSegmentedLepKey;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import static com.icthh.xm.commons.domainevent.service.db.impl.TypeKeyAwareJpaEntityMapper.TYPE_KEY;

@Component
public class TypeKeyAwareEntityResolver extends AppendLepKeyResolver {

    @Override
    protected String[] getAppendSegments(SeparatorSegmentedLepKey baseKey, LepMethod method, LepManagerService managerService) {
        JpaEntityContext jpaEntityContext = getRequiredParam(method, "jpaEntityContext", JpaEntityContext.class);
        String typeKey = getTypeKey(jpaEntityContext);
        return new String[]{
            typeKey.toUpperCase()
        };
    }

    private String getTypeKey(JpaEntityContext jpaEntityContext) {

        String[] propertyNames = jpaEntityContext.getPropertyNames();
        Object[] state = jpaEntityContext.getCurrentState() != null ? jpaEntityContext.getCurrentState() : jpaEntityContext.getPreviousState();

        for (int i = 0; i < propertyNames.length; i++) {
            if (propertyNames[i].equals(TYPE_KEY)) {
                return state[i].toString();
            }
        }
        return StringUtils.EMPTY;
    }
}
