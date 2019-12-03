package com.icthh.xm.commons.messaging.communication;

import lombok.Data;

/**
 * The values of parameters which are used in the content if the content contains them.
 */
@Data
public class CommunicationRequestCharacteristic {

    private String name;
    private String value;
}

