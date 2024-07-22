package com.icthh.xm.commons.flow.api;

import com.icthh.xm.commons.flow.context.FlowLepAdditionalContext.FlowLepAdditionalContextField;
import com.icthh.xm.commons.flow.context.StepLepAdditionalContext.StepLepAdditionalContextField;
import com.icthh.xm.commons.flow.context.StepsLepAdditionalContext.StepsLepAdditionalContextField;
import com.icthh.xm.commons.flow.context.TenantResourceLepAdditionalContext.TenantResourceLepAdditionalContextField;

public interface FlowLepContextFields extends
    TenantResourceLepAdditionalContextField,
    StepLepAdditionalContextField,
    StepsLepAdditionalContextField,
    FlowLepAdditionalContextField
{
}
