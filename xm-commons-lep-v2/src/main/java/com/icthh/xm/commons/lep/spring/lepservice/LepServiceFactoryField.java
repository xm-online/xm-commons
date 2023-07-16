package com.icthh.xm.commons.lep.spring.lepservice;

import com.icthh.xm.commons.lep.api.LepAdditionalContextField;

public interface LepServiceFactoryField extends LepAdditionalContextField {

    String LEP_SERVICES = "lepServices";

    default LepServiceFactory getLepServices() {
        return (LepServiceFactory)get(LEP_SERVICES);
    }

}
