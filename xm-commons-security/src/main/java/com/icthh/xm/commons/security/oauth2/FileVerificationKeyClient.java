package com.icthh.xm.commons.security.oauth2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Value;

/**
 * Client fetching the public key from local config repository to create a SignatureVerifier.
 */
@Slf4j
public class FileVerificationKeyClient implements JwtVerificationKeyClient {

    private final String configDirPath;

    public FileVerificationKeyClient(@Value("${xm-config.configDirPath}") String configDirPath) {
        this.configDirPath = configDirPath;
    }

    @Override
    public byte[] fetchKeyContent() {
        try {
            Path keyPath = Path.of(configDirPath, "config", "public.cer");
            byte[] content = Files.readAllBytes(keyPath);
            if (ArrayUtils.isEmpty(content)) {
                log.warn("Public key not fetched from path: {}", keyPath);
                return null;
            }
            return content;
        } catch (IOException ex) {
            log.warn("Could not get public key, exception: {}", ex.getMessage());
            return null;
        }
    }

}
