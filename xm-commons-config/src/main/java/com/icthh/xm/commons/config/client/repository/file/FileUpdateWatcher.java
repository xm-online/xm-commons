package com.icthh.xm.commons.config.client.repository.file;

import com.icthh.xm.commons.config.client.api.ConfigService;
import com.icthh.xm.commons.config.client.config.XmConfigProperties;

import java.io.File;
import java.util.List;
import java.util.Optional;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.springframework.beans.factory.DisposableBean;

import static java.io.File.separator;
import static org.apache.commons.lang3.StringUtils.prependIfMissing;
import static org.apache.commons.lang3.StringUtils.replaceChars;

@Slf4j
public class FileUpdateWatcher implements DisposableBean {

    public static final String MS_CONFIG_SEPARATOR = "/";
    private final FileAlterationMonitor monitor;
    private final ConfigService configService;

    @SneakyThrows
    public FileUpdateWatcher(ConfigService configService, XmConfigProperties xmConfigProperties) {
        this.configService = configService;
        this.monitor = createFileWatcher(xmConfigProperties);
        this.monitor.start();
    }

    private FileAlterationMonitor createFileWatcher(XmConfigProperties xmConfigProperties) {
        FileAlterationObserver observer = new FileAlterationObserver(new File(xmConfigProperties.getConfigDirPath()));
        observer.addListener(new FileAlterationListenerAdaptor() {

            @Override
            public void onFileCreate(File file) {
                updateFile(xmConfigProperties, file);
            }

            @Override
            public void onFileChange(File file) {
                updateFile(xmConfigProperties, file);
            }

            @Override
            public void onFileDelete(File file) {
                updateFile(xmConfigProperties, file);
            }
        });

        return new FileAlterationMonitor(xmConfigProperties.getDirWatchInterval(), observer);
    }

    public void updateFile(XmConfigProperties xmConfigProperties, File file) {

        List<String> configs = Optional.ofNullable(xmConfigProperties)
                                       .map(XmConfigProperties::getConfigDirPath)
                                       .map(String::length)
                                       .map(length -> file.getAbsolutePath().substring(length))
                                       .map(configPath -> replaceChars(configPath, separator, MS_CONFIG_SEPARATOR))
                                       .map(configPath -> prependIfMissing(configPath, MS_CONFIG_SEPARATOR))
                                       .stream().toList();

        configService.updateConfigurations("" + System.currentTimeMillis(), configs);
    }

    @Override
    public void destroy() throws Exception {
        monitor.stop();
    }
}
