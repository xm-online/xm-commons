package com.icthh.xm.commons.config.client.applied;

import com.icthh.xm.commons.config.client.api.FetchConfigurationSettings;
import com.icthh.xm.commons.config.client.config.XmConfigProperties;
import com.icthh.xm.commons.config.client.repository.FileCommonConfigRepository;
import com.icthh.xm.commons.config.client.repository.kafka.ConfigTopicConsumer;
import com.icthh.xm.commons.config.client.service.CommonConfigService;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.apache.kafka.clients.consumer.ConsumerRecord;

import java.io.File;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

public class AppliedConfigFileModeUnitTest {

    private static final String PATH = "/config/tenants/XM/entity/a.yml";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private CommonConfigService configService;
    private AppliedConfigService applied;

    @Before
    public void setUp() throws Exception {
        FileUtils.writeStringToFile(new File(folder.getRoot(), PATH), "content", UTF_8);
        XmConfigProperties properties = new XmConfigProperties();
        properties.setConfigMode("FILE");
        properties.setConfigDirPath(folder.getRoot().getAbsolutePath());
        FileCommonConfigRepository repository = new FileCommonConfigRepository(properties);
        FetchConfigurationSettings settings = new FetchConfigurationSettings("entity", false);
        configService = new CommonConfigService(settings, repository);
        applied = new AppliedConfigService(repository, settings, false);
        configService.addConfigurationChangedListener(applied);
    }

    @Test
    public void commitIsUnknownAndHashesMatchFiles() {
        applied.loadStartupVersion();
        applied.onInitialLoad(configService.getConfigMapAntPattern(null, new FetchConfigurationSettings("entity", false).getMsConfigPatterns()));

        assertThat(applied.getVersion().mainVersion()).isEqualTo("unknown");
        assertThat(applied.getPathHashes()).containsExactly(
            new AppliedConfigService.PathHash(PATH, DigestUtils.sha256Hex("content")));
    }

    @Test
    public void fileRefreshUpdatesHashAndEventsDoNotChangeVersion() throws Exception {
        FileUtils.writeStringToFile(new File(folder.getRoot(), PATH), "changed", UTF_8);

        configService.updateConfigurations(String.valueOf(System.currentTimeMillis()), java.util.List.of(PATH));
        new ConfigTopicConsumer(configService, applied).consumeEvent(new ConsumerRecord<>("t", 1, 1, "k",
            "{\"eventId\":\"id\",\"commit\":\"123\",\"paths\":[]}"));

        assertThat(applied.getPathHashes()).containsExactly(
            new AppliedConfigService.PathHash(PATH, DigestUtils.sha256Hex("changed")));
        assertThat(applied.getVersion().mainVersion()).isEqualTo("unknown");
    }
}
