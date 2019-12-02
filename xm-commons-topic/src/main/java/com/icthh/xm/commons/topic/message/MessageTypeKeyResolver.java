package com.icthh.xm.commons.topic.message;

import com.icthh.xm.commons.lep.AppendLepKeyResolver;
import com.icthh.xm.commons.topic.domain.TopicConfig;
import com.icthh.xm.lep.api.LepManagerService;
import com.icthh.xm.lep.api.LepMethod;
import com.icthh.xm.lep.api.commons.SeparatorSegmentedLepKey;
import org.springframework.stereotype.Component;

@Component
public class MessageTypeKeyResolver extends AppendLepKeyResolver {

    @Override
    protected String[] getAppendSegments(SeparatorSegmentedLepKey baseKey,
                                         LepMethod method,
                                         LepManagerService managerService) {
        TopicConfig topicConfig = getRequiredParam(method, "topicConfig", TopicConfig.class);
        String translateToLepConvention = translateToLepConvention(topicConfig.getTypeKey());
        return new String[]{translateToLepConvention};
    }
}
