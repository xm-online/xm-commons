package com.icthh.xm.commons.lep;

import org.springframework.core.io.AbstractResource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * The {@link XmLepScriptResource} class.
 */
// https://stackoverflow.com/questions/41856005/spring-resttemplate-streaming-response-into-another-request
public class XmLepScriptResource extends AbstractResource {

    private static final XmLepScriptResource NON_EXIST = new XmLepScriptResource("-none-");

    private final String xmMsConfigPath;
    private long lastModified;
    private String content;
    private boolean exists;

    public static XmLepScriptResource nonExist() {
        return NON_EXIST;
    }

    public XmLepScriptResource(String xmMsConfigPath) {
        this.xmMsConfigPath = Objects.requireNonNull(xmMsConfigPath, "xmMsConfigPath can't be null");
        this.exists = false;
        this.lastModified = 0L;
    }

    public XmLepScriptResource(String xmMsConfigPath, long lastModified) {
        this.xmMsConfigPath = Objects.requireNonNull(xmMsConfigPath, "xmMsConfigPath can't be null");
        this.exists = false;
        this.lastModified = lastModified;
    }

    public XmLepScriptResource(String xmMsConfigPath, String content, long lastModified) {
        this.xmMsConfigPath = Objects.requireNonNull(xmMsConfigPath, "xmMsConfigPath can't be null");
        this.content = Objects.requireNonNull(content, "content can't be null");
        this.lastModified = lastModified;
        this.exists = true;
    }

    public void update(String content, long lastModified) {
        if (!exists) {
            throw new IllegalStateException("Can't update content for not existing resource, location: "
                                                + xmMsConfigPath + ". Create new "
                                                + XmLepScriptResource.class.getSimpleName()
                                                + " instance for this");
        }
        this.content = Objects.requireNonNull(content, "content can't be null");
        this.lastModified = lastModified;
    }

    @Override
    public boolean exists() {
        return exists;
    }

    @Override
    public long lastModified() {
        return lastModified;
    }

    @Override
    public String getDescription() {
        return xmMsConfigPath;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (content == null) {
            throw new IOException("Content is null");
        }
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8.name()));
    }

}
