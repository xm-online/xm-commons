package com.icthh.xm.commons.domainevent.db.domain;

import com.icthh.xm.commons.domainevent.domain.enums.DefaultDomainEventOperation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.Map;

@Data
@EqualsAndHashCode
@AllArgsConstructor
@Builder
public class JpaEntityContext {
    private Object entity;
    private Serializable id;
    private Map<String, State> propertyNameToStates;
    private DefaultDomainEventOperation domainEventOperation;

    public String findPropertyStateValue(String property) {

        if (propertyNameToStates.containsKey(property)) {
            State state = propertyNameToStates.get(property);
            return state.getCurrent() != null ? state.getCurrent().toString() : state.getPrevious().toString();
        }

        return StringUtils.EMPTY;
    }

}
