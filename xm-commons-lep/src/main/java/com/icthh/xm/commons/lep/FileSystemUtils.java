package com.icthh.xm.commons.lep;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * The {@link FileSystemUtils} class.
 */
@UtilityClass
final class FileSystemUtils {

    static final String APP_HOME_DIR = getAppHomeDir().toString();

    private static Path getAppHomeDir() {
        String base = SystemUtils.USER_HOME;

        if (StringUtils.isBlank(base)) {
            // For WINDOWS
            if (SystemUtils.IS_OS_WINDOWS) {
                // try to set APPDATA
                base = System.getenv("APPDATA");

                // try to set SYSTEMDRIVE
                if (StringUtils.isBlank(base)) {
                    base = System.getenv("SYSTEMDRIVE");
                    base = StringUtils.isNotBlank(base) ? base + "\\" : null;
                }

                // try to set C disk
                if (StringUtils.isBlank(base)) {
                    base = "C:\\";
                }
            } else {
                // no strict check for other OS file system - just use Unix like file path
                base = "/opt";
            }
        }

        return Paths.get(base, "xm-online");
    }

}
