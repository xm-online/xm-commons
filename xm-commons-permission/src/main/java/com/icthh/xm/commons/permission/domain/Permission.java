package com.icthh.xm.commons.permission.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.icthh.xm.commons.permission.config.SpelDeserializer;
import com.icthh.xm.commons.permission.config.SpelSerializer;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.expression.Expression;

@Getter
@Setter
@EqualsAndHashCode(of = {"msName", "roleKey", "privilegeKey"})
@ToString
@JsonPropertyOrder( {"privilegeKey", "disabled", "deleted", "envCondition", "resourceCondition", "reactionStrategy"})
public class Permission implements Comparable<Permission> {

    @JsonIgnore
    private String msName;
    @JsonIgnore
    private String roleKey;
    private String privilegeKey;
    private boolean disabled;
    private boolean deleted;
    private ReactionStrategy reactionStrategy;
    @JsonSerialize(using = SpelSerializer.class)
    @JsonDeserialize(using = SpelDeserializer.class)
    private Expression envCondition;
    @JsonSerialize(using = SpelSerializer.class)
    @JsonDeserialize(using = SpelDeserializer.class)
    private Expression resourceCondition;

    @Override
    public int compareTo(Permission o) {
        return privilegeKey.compareTo(o.getPrivilegeKey());
    }
}
