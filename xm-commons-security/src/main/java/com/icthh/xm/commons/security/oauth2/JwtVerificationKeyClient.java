package com.icthh.xm.commons.security.oauth2;

import lombok.SneakyThrows;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public interface JwtVerificationKeyClient {

    byte[] fetchKeyContent();

    @SneakyThrows
    default PublicKey getVerificationKey() {
        byte[] content = fetchKeyContent();
        if (content == null) {
            return null;
        }

        CertificateFactory f = CertificateFactory.getInstance("X.509");
        X509Certificate certificate = (X509Certificate) f.generateCertificate(new ByteArrayInputStream(content));
        return certificate.getPublicKey();
    }
}
