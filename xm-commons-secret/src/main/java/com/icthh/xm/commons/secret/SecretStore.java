package com.icthh.xm.commons.secret;

import java.util.Map;
import java.util.Optional;

/**
 * Storage of secrets. Paths are relative to the store root, as {@code <TENANT>/<alias>}; the store knows nothing about tenants.
 */
public interface SecretStore {

    /** Returns empty when the secret does not exist. */
    Optional<Map<String, String>> read(String path);

    /** Creates or replaces. */
    void write(String path, Map<String, String> data);

    void delete(String path);
}
