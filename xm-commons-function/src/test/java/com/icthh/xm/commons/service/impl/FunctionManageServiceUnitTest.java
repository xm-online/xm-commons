package com.icthh.xm.commons.service.impl;

import static com.icthh.xm.commons.utils.FunctionSpecReaderUtils.loadFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import com.icthh.xm.commons.config.FunctionApiSpecConfiguration;
import com.icthh.xm.commons.config.FunctionApiSpecsProcessor;
import com.icthh.xm.commons.config.client.repository.CommonConfigRepository;
import com.icthh.xm.commons.config.client.service.CommonConfigService;
import com.icthh.xm.commons.config.domain.Configuration;
import com.icthh.xm.commons.domain.FunctionSpecWithFileName;
import com.icthh.xm.commons.domain.enums.FunctionTxTypes;
import com.icthh.xm.commons.domain.spec.FunctionSpec;
import com.icthh.xm.commons.exceptions.BusinessException;
import com.icthh.xm.commons.listener.JsonListenerService;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

public class FunctionManageServiceUnitTest {

    public static final String TEST_TENANT = "TEST_TENANT";
    public static final String TEST_PATH = "/config/tenants/TEST_TENANT/testApp/functions/test-file.yml";
    public static final String TEST_NEW_PATH = "/config/tenants/TEST_TENANT/testApp/functions/test-new-file.yml";

    private FunctionManageServiceImpl functionManageService;

    private FunctionApiSpecConfiguration specService = new FunctionApiSpecConfiguration(
        "testApp", mock(JsonListenerService.class), mock(FunctionApiSpecsProcessor.class)
    );
    @Mock
    private TenantContextHolder tenantContextHolder;
    @Mock
    private CommonConfigRepository commonConfigRepository;
    @Mock
    private CommonConfigService commonConfigService;

    @BeforeEach
    void setUp() {
        openMocks(this);
        functionManageService = new FunctionManageServiceImpl(
            specService, tenantContextHolder, commonConfigRepository, commonConfigService
        );
    }

    @Test
    public void testAddFunctionToNewFile() {
        when(tenantContextHolder.getTenantKey()).thenReturn(TEST_TENANT);
        FunctionSpec functionSpec = mockSpec();
        FunctionSpecWithFileName<FunctionSpec> testData = new FunctionSpecWithFileName<>();
        testData.setItem(functionSpec);
        testData.setFileKey("test-file");

        functionManageService.addFunction(testData);

        verify(commonConfigRepository).updateConfigFullPath(argThat(c -> {
            assertEquals(TEST_PATH, c.getPath());
            assertEquals(loadFile("config/functions/test-add-function.yml"), c.getContent());
            return true;
        }), isNull());
    }

    @Test
    public void testKeyExists() {
        when(tenantContextHolder.getTenantKey()).thenReturn(TEST_TENANT);
        FunctionSpec functionSpec = mockSpec();
        FunctionSpecWithFileName<FunctionSpec> testData = new FunctionSpecWithFileName<>();
        testData.setItem(functionSpec);
        testData.setFileKey("test-file2");

        specService.onRefresh(TEST_PATH, loadFile("config/functions/test-add-function.yml"));
        specService.refreshFinished(List.of(TEST_PATH));

        assertThrows(BusinessException.class, () -> {
            functionManageService.addFunction(testData);
        });

        specService.onRefresh(TEST_PATH, null);
        specService.refreshFinished(List.of(TEST_PATH));
    }

    @Test
    public void testAddToExistingFileKeepSpaces() {
        when(tenantContextHolder.getTenantKey()).thenReturn(TEST_TENANT);
        FunctionSpec functionSpec = mockSpec();
        FunctionSpecWithFileName<FunctionSpec> testData = new FunctionSpecWithFileName<>();
        testData.setItem(functionSpec);
        testData.setFileKey("test-file");
        specService.onRefresh(TEST_PATH, loadFile("config/functions/test-existing-function.yml"));
        specService.refreshFinished(List.of(TEST_PATH));

        functionManageService.addFunction(testData);

        verify(commonConfigRepository).updateConfigFullPath(argThat(c -> {
            assertEquals(TEST_PATH, c.getPath());
            assertEquals(loadFile("config/functions/test-existing-function-expected.yml"), c.getContent());
            return true;
        }), isNull());

        specService.onRefresh(TEST_PATH, null);
        specService.refreshFinished(List.of(TEST_PATH));
    }

    @Test
    public void testAddToExistingFile2Spaces() {
        when(tenantContextHolder.getTenantKey()).thenReturn(TEST_TENANT);
        FunctionSpec functionSpec = mockSpec();
        FunctionSpecWithFileName<FunctionSpec> testData = new FunctionSpecWithFileName<>();
        testData.setItem(functionSpec);
        testData.setFileKey("test-file");

        specService.onRefresh(TEST_PATH, loadFile("config/functions/test-existing-function-2.yml"));
        specService.refreshFinished(List.of(TEST_PATH));

        functionManageService.addFunction(testData);

        verify(commonConfigRepository).updateConfigFullPath(argThat(c -> {
            assertEquals(TEST_PATH, c.getPath());
            assertEquals(loadFile("config/functions/test-existing-function-2-expected.yml"), c.getContent());
            return true;
        }), isNull());

        specService.onRefresh(TEST_PATH, null);
        specService.refreshFinished(List.of(TEST_PATH));
    }

    @Test
    public void testAddToExistingFile4Spaces() {
        when(tenantContextHolder.getTenantKey()).thenReturn(TEST_TENANT);
        FunctionSpec functionSpec = mockSpec();
        FunctionSpecWithFileName<FunctionSpec> testData = new FunctionSpecWithFileName<>();
        testData.setItem(functionSpec);
        testData.setFileKey("test-file");

        specService.onRefresh(TEST_PATH, loadFile("config/functions/test-existing-function-4.yml"));
        specService.refreshFinished(List.of(TEST_PATH));

        functionManageService.addFunction(testData);

        verify(commonConfigRepository).updateConfigFullPath(argThat(c -> {
            assertEquals(TEST_PATH, c.getPath());
            assertEquals(loadFile("config/functions/test-existing-function-4-expected.yml"), c.getContent());
            return true;
        }), isNull());

        specService.onRefresh(TEST_PATH, null);
        specService.refreshFinished(List.of(TEST_PATH));
    }

    @Test
    public void testUpdateFunctionInSameFile() {
        when(tenantContextHolder.getTenantKey()).thenReturn(TEST_TENANT);
        FunctionSpec functionSpec = mockFunc2();
        FunctionSpecWithFileName<FunctionSpec> testData = new FunctionSpecWithFileName<>();
        testData.setItem(functionSpec);
        testData.setFileKey("test-file");

        specService.onRefresh(TEST_PATH, loadFile("config/functions/test-update-function.yml"));
        specService.refreshFinished(List.of(TEST_PATH));

        functionManageService.updateFunction(testData);

        verify(commonConfigRepository).updateConfigFullPath(argThat(c -> {
            assertEquals(TEST_PATH, c.getPath());
            assertEquals(loadFile("config/functions/test-update-function-expected.yml"), c.getContent());
            return true;
        }), isNull());

        specService.onRefresh(TEST_PATH, null);
        specService.refreshFinished(List.of(TEST_PATH));
    }

    @Test
    public void testUpdateFunctionWithFileMove() {
        when(tenantContextHolder.getTenantKey()).thenReturn(TEST_TENANT);
        doAnswer(it -> {
            Configuration argument = (Configuration) it.getArguments()[0];
            specService.onRefresh(argument.getPath(), argument.getContent());
            specService.refreshFinished(List.of(argument.getPath()));
            return null;
        }).when(commonConfigService).notifyUpdated(any(Configuration.class));

        FunctionSpec functionSpec = mockFunc2();
        FunctionSpecWithFileName<FunctionSpec> testData = new FunctionSpecWithFileName<>();
        testData.setItem(functionSpec);
        testData.setFileKey("test-new-file");

        specService.onRefresh(TEST_PATH, loadFile("config/functions/test-update-function.yml"));
        specService.refreshFinished(List.of(TEST_PATH));

        functionManageService.updateFunction(testData);

        ArgumentCaptor<Configuration> cfg = ArgumentCaptor.forClass(Configuration.class);
        ArgumentCaptor<String> msg = ArgumentCaptor.forClass(String.class);

        verify(commonConfigRepository, times(2))
            .updateConfigFullPath(cfg.capture(), msg.capture());

        Configuration first  = cfg.getAllValues().get(0);
        Configuration second = cfg.getAllValues().get(1);

        assertThat(first.getPath()).isEqualTo(TEST_PATH);
        assertThat(first.getContent())
            .isEqualTo(loadFile("config/functions/test-update-move-function-expected.yml"));

        assertThat(second.getPath()).isEqualTo(TEST_NEW_PATH);
        assertThat(second.getContent())
            .isEqualTo(loadFile("config/functions/test-update-move-function-new-file-expected.yml"));

        specService.onRefresh(TEST_PATH, null);
        specService.onRefresh(TEST_NEW_PATH, null);
        specService.refreshFinished(List.of(TEST_PATH, TEST_NEW_PATH));
    }

    @Test
    public void testDeleteFunction() {
        when(tenantContextHolder.getTenantKey()).thenReturn(TEST_TENANT);

        specService.onRefresh(TEST_PATH, loadFile("config/functions/test-update-function.yml"));
        specService.refreshFinished(List.of(TEST_PATH));

        functionManageService.removeFunction("FUNC_2");

        verify(commonConfigRepository).updateConfigFullPath(argThat(c -> {
            assertEquals(TEST_PATH, c.getPath());
            assertEquals(loadFile("config/functions/test-delete-function-expected.yml"), c.getContent());
            return true;
        }), isNull());

        specService.onRefresh(TEST_PATH, null);
        specService.refreshFinished(List.of(TEST_PATH));
    }

    private FunctionSpec mockSpec() {
        FunctionSpec functionSpec = new FunctionSpec();
        functionSpec.setKey("flow/http/flow-http-trigger-PUT-UPDATE_PHONE_NUMBER_RESOURCE");
        functionSpec.setPath("resource/{orgRef}/phoneNumber/{phoneNumber}");
        functionSpec.setInputSpec("{\"$ref\":\"#/xmDefinition/PhoneNumberDto\"}");
        functionSpec.setTxType(FunctionTxTypes.NO_TX);
        functionSpec.setTags(List.of("resource"));
        functionSpec.setHttpMethods(List.of("PUT"));
        functionSpec.setName(Map.of("en", "Update phone number resource"));
        functionSpec.setDescription("Update phone number resource");
        functionSpec.setOutputSpec("{\"$ref\":\"#/xmDefinition/PhoneNumberDto\"}");
        return functionSpec;
    }

    private FunctionSpec mockFunc2() {
        FunctionSpec functionSpec = new FunctionSpec();
        functionSpec.setKey("FUNC_2");
        functionSpec.setPath("func2");
        functionSpec.setInputSpec("{\"$ref\":\"#/xmDefinition/PhoneNumberDto\"}");
        functionSpec.setTxType(FunctionTxTypes.NO_TX);
        functionSpec.setTags(List.of("resource"));
        functionSpec.setHttpMethods(List.of("PUT", "GET", "POST"));
        functionSpec.setAnonymous(true);
        functionSpec.setName(Map.of("en", "func2"));
        functionSpec.setDescription("func2");
        functionSpec.setOutputSpec("{\"$ref\":\"#/xmDefinition/PhoneNumberDto\"}");
        return functionSpec;
    }

}
