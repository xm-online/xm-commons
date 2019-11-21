package com.icthh.xm.commons.config.client.repository;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.ObjectUtils;

/**
 * Implementation of byte array resource with file name.
 *
 * It is needed to send file name in ContentDisposition header inside each Part because on the server side {@link
 * org.springframework.web.multipart.support.StandardMultipartHttpServletRequest} reads only parts with defined filename
 * (see method parseRequest()).
 */
class NamedByteArrayResource extends ByteArrayResource {

    private final String filename;

    public NamedByteArrayResource(final byte[] byteArray, String fileName) {
        super(byteArray, "byteArray.length = " + byteArray.length + ", fileName = " + fileName);
        this.filename = fileName;
    }

    @Override
    public String getFilename() {
        return filename;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || other.getClass() != getClass()) {
            return false;
        }
        NamedByteArrayResource otherEntity = (NamedByteArrayResource) other;
        return (ObjectUtils.nullSafeEquals(this.getByteArray(), otherEntity.getByteArray())
            && ObjectUtils.nullSafeEquals(this.filename, otherEntity.filename));
    }

    @Override
    public int hashCode() {
        return (ObjectUtils.nullSafeHashCode(this.getByteArray()) * 29
                + ObjectUtils.nullSafeHashCode(this.filename));
    }

}
