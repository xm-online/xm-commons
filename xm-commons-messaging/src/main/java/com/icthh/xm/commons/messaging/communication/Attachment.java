package com.icthh.xm.commons.messaging.communication;

import lombok.Data;

/**
 * Complements the description of an element (for instance a product) through video, pictures...
 */
@Data
public class Attachment {

    private String id;
    private String description;
    private String href;
    private String mimeType;
    private String name;
    private String path;
    private Integer size;
    private Integer sizeUnit;
    private String url;
    private TimePeriod validFor;
    private String atType;
    private String atSchemaLocation;
    private String atBaseType;
}

