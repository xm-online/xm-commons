package com.icthh.xm.commons.permission.access.subject;

import lombok.Value;

@Value
public class Subject {
    private String login;
    private String userKey;
    private String role;

}
