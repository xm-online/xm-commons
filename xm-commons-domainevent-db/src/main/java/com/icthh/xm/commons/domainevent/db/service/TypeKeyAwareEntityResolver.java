package com.icthh.xm.commons.domainevent.db.service;

import com.icthh.xm.commons.domainevent.db.domain.JpaEntityContext;
import com.icthh.xm.commons.domainevent.db.domain.State;
import com.icthh.xm.commons.lep.AppendLepKeyResolver;
import com.icthh.xm.lep.api.LepManagerService;
import com.icthh.xm.lep.api.LepMethod;
import com.icthh.xm.lep.api.commons.SeparatorSegmentedLepKey;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.icthh.xm.commons.domainevent.db.service.impl.TypeKeyAwareJpaEntityMapper.TYPE_KEY;

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
        Map<String, State> propertyNameToStates = jpaEntityContext.getPropertyNameToStates();

        if (propertyNameToStates.containsKey(TYPE_KEY)) {
            State state = propertyNameToStates.get(TYPE_KEY);
            return state.current() != null ? state.current().toString() : state.previous().toString();
        }

        return StringUtils.EMPTY;
    }

}
