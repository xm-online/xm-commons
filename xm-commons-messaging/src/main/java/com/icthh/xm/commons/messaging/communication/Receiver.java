package com.icthh.xm.commons.messaging.communication;

import lombok.Data;

/**
 * Receivers of the communication message.
 */
@Data
public class Receiver {

    private String id;
    private String appUserId;
    private String email;
    private String ip;
    private String name;
    private String phoneNumber;
    private String type;
}

