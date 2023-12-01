package com.icthh.xm.commons.lep.api;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.ToString;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamSource;

import java.time.Instant;

import static java.nio.charset.StandardCharsets.UTF_8;

@Getter
@ToString(exclude = "contentStream")
public class XmLepConfigFile  {
    private final String path;
    private final InputStreamSource contentStream;
    private final long lastModified = Instant.now().toEpochMilli();

    public XmLepConfigFile(String path, InputStreamSource content) {
        this.path = path;
        this.contentStream = content;
    }

    public XmLepConfigFile(String path, String content) {
        this(path, new ByteArrayResource(content.getBytes(UTF_8)));
    }

    @SneakyThrows
    public String readContent() {
        return IOUtils.toString(contentStream.getInputStream(), UTF_8);
    }

    public String metadataKey() {
        return path + ":" + lastModified;
    }
}
