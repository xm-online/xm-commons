package com.icthh.xm.commons.messaging.communication;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Communication message means a notification approach in the format of a message
 * which can be dispatched (sent) to the certain user
 * by the system with the content which can be felt and understood by the recipient.
 * The user can be either a final customer or a customer service agent.
 * The message can reach the customer in different interaction channels,
 * including: email, short message, mobile app notification (push).
 */
@Data
public class CommunicationMessage {

    private String id;
    private String href;
    private Boolean callbackFlag;
    private String content;
    private String description;
    private Boolean logFlag;
    private String priority;
    private OffsetDateTime sendTime;
    private OffsetDateTime sendTimeComplete;
    private String status;
    private String subject;
    private Integer tryTimes;
    private String type;
    private String version;
    private List<Receiver> receiver = new ArrayList<>();
    private Sender sender;
    private List<CommunicationRequestCharacteristic> characteristic;
}

