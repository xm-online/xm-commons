package com.icthh.xm.commons.config.client.state;

import com.icthh.xm.commons.config.domain.Configuration;
import lombok.SneakyThrows;

import java.security.MessageDigest;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ConfigStateHolder {

    private static final String SHA_256 = "SHA-256";
    private static final int CHUNK_SIZE = 65536;
    private static final byte SEPARATOR = 0;
    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

    private final AtomicReference<String> receivedCommit = new AtomicReference<>();
    private final AtomicReference<String> processedCommit = new AtomicReference<>();
    private final AtomicReference<String> processedConfigsHash = new AtomicReference<>();

    public void onCommitReceived(String commit) {
        receivedCommit.set(commit);
    }

    public void onCommitProcessed(String commit) {
        processedCommit.set(commit);
    }

    public void onConfigurationsProcessed(Map<String, Configuration> configurations) {
        processedConfigsHash.set(hashConfigurations(configurations));
    }

    public String getReceivedCommit() {
        return receivedCommit.get();
    }

    public String getProcessedCommit() {
        return processedCommit.get();
    }

    public String getProcessedConfigsHash() {
        return processedConfigsHash.get();
    }

    @SneakyThrows
    private static String hashConfigurations(Map<String, Configuration> configurations) {
        MessageDigest digest = MessageDigest.getInstance(SHA_256);
        configurations.entrySet()
            .stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> {
                update(digest, entry.getKey());
                digest.update(SEPARATOR);
                update(digest, getContent(entry.getValue()));
                digest.update(SEPARATOR);
            });
        return toHex(digest.digest());
    }

    private static String getContent(Configuration configuration) {
        return configuration == null ? null : configuration.getContent();
    }

    private static void update(MessageDigest digest, String value) {
        if (value == null) {
            return;
        }
        int length = value.length();
        int from = 0;
        while (from < length) {
            int to = Math.min(from + CHUNK_SIZE, length);
            if (to < length && Character.isHighSurrogate(value.charAt(to - 1))) {
                to++;
            }
            digest.update(value.substring(from, to).getBytes(UTF_8));
            from = to;
        }
    }

    private static String toHex(byte[] bytes) {
        char[] hex = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int value = bytes[i] & 0xFF;
            hex[i * 2] = HEX_DIGITS[value >>> 4];
            hex[i * 2 + 1] = HEX_DIGITS[value & 0x0F];
        }
        return new String(hex);
    }
}
