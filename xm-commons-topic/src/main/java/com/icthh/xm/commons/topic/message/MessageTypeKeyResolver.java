package com.icthh.xm.commons.topic.message;

import com.icthh.xm.commons.topic.domain.TopicConfig;
import com.icthh.xm.lep.api.LepKeyResolver;
import com.icthh.xm.lep.api.LepMethod;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MessageTypeKeyResolver implements LepKeyResolver {

    @Override
    public List<String> segments(LepMethod method) {
        return List.of(method.getParameter("topicConfig", TopicConfig.class).getTypeKey());
    }
}
