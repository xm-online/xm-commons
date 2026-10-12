package com.icthh.xm.commons.config.client.repository;

import com.icthh.xm.commons.config.client.config.XmConfigProperties;
import com.icthh.xm.commons.config.domain.Configuration;
import com.icthh.xm.commons.exceptions.BusinessException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class FileCommonConfigRepositoryUnitTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private File baseDir;
    private FileCommonConfigRepository repository;

    @Before
    public void setUp() throws Exception {
        baseDir = folder.newFolder("repo");
        XmConfigProperties properties = new XmConfigProperties();
        properties.setConfigDirPath(baseDir.getAbsolutePath());
        repository = new FileCommonConfigRepository(properties);
    }

    @Test
    public void updateConfigFullPathWritesInsideBaseDir() throws Exception {
        repository.updateConfigFullPath(new Configuration("/config/tenants/XM/a.yml", "content"), null);

        assertThat(Files.readString(new File(baseDir, "config/tenants/XM/a.yml").toPath())).isEqualTo("content");
    }

    @Test
    public void updateConfigFullPathRejectsPathOutsideBaseDir() {
        assertThatThrownBy(() -> repository.updateConfigFullPath(
            new Configuration("/config/tenants/XM/../../../../escaped.yml", "content"), null))
            .isInstanceOf(BusinessException.class);

        assertThat(new File(baseDir.getParentFile(), "escaped.yml")).doesNotExist();
    }
}
