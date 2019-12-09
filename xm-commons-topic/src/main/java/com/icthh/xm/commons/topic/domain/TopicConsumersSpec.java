package com.icthh.xm.commons.topic.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.util.LinkedList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"topics"})
@Data
public class TopicConsumersSpec {

    @JsonProperty("topics")
    private List<TopicConfig> topics = new LinkedList<>();
}
