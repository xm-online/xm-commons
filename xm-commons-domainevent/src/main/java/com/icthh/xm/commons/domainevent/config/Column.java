package com.icthh.xm.commons.domainevent.config;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

@Getter
@Setter
@EqualsAndHashCode
@ToString
public class Column {

    private List<String> in;
    private List<String> notIn;
    private String equals;
    private String notEquals;
    private String match;

    public boolean match(String value) {

        if (value == null) {
            value = "null";
        }

        if (Objects.nonNull(in)) {
            return in.contains(value);
        }
        if (Objects.nonNull(notIn)) {
            return !notIn.contains(value);
        }
        if (Objects.nonNull(equals)) {
            return equals.equals(value);
        }
        if (Objects.nonNull(notEquals)) {
            return !notEquals.equals(value);
        }
        if (Objects.nonNull(match)) {
            return Pattern.matches(match, value);
        }

        return false;
    }

}
