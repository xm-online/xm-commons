package com.icthh.xm.commons.lep.spring.lepservice;

import com.icthh.xm.commons.lep.api.LepAdditionalContextField;

public interface LepServiceFactoryField extends LepAdditionalContextField {

    String FIELD_NAME = "lepServices";

    default LepServiceFactory getLepServices() {
        return (LepServiceFactory)get(FIELD_NAME);
    }

}
