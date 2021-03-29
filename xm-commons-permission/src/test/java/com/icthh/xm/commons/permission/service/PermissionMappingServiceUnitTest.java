package com.icthh.xm.commons.permission.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import com.icthh.xm.commons.permission.domain.Permission;
import com.icthh.xm.commons.permission.domain.ReactionStrategy;
import com.icthh.xm.commons.permission.service.filter.EqualsOrNullPermissionMsNameFilter;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
public class PermissionMappingServiceUnitTest {

    private static final String TEST_YML = "---\n" +
        "MS1:\n" +
        "  ROLE1:\n" +
        "    - privilegeKey: \"PRIVILEGE_KEY1\"\n" +
        "      disabled: false\n" +
        "      deleted: false\n" +
        "      envCondition: \"ENV_COND1\"\n" +
        "      resourceCondition: \"RESOLUTION_START1\"\n" +
        "      reactionStrategy: \"SKIP\"\n" +
        "  ROLE2:\n" +
        "    - privilegeKey: \"PRIVILEGE_KEY2\"\n" +
        "      disabled: false\n" +
        "      deleted: false\n" +
        "      envCondition: \"ENV_COND2\"\n" +
        "      resourceCondition: \"RESOLUTION_START2\"\n" +
        "      reactionStrategy: \"SKIP\"\n" +
        "MS2:\n" +
        "  ROLE1:\n" +
        "    - privilegeKey: \"PRIVILEGE_KEY1\"\n" +
        "      disabled: false\n" +
        "      deleted: false\n" +
        "      envCondition: \"ENV_COND3\"\n" +
        "      resourceCondition: \"RESOLUTION_START3\"\n" +
        "      reactionStrategy: \"SKIP\"\n" +
        "  ROLE3:\n" +
        "    - privilegeKey: \"PRIVILEGE_KEY3\"\n" +
        "      disabled: false\n" +
        "      deleted: false\n" +
        "      envCondition: \"ENV_COND3\"\n" +
        "      resourceCondition: \"RESOLUTION_START3\"\n" +
        "      reactionStrategy: \"SKIP\"\n";

    private final ExpressionParser parser = new SpelExpressionParser();

    private PermissionMappingService service;

    private EqualsOrNullPermissionMsNameFilter filter;

    @Before
    public void setUp() {
        filter = new EqualsOrNullPermissionMsNameFilter();
        service = new PermissionMappingService(filter);
    }

    @Test
    public void testYmlToPermissions_shouldFilterByMsName() {
        ReflectionTestUtils.setField(filter, "msName", "MS2", String.class);

        Map<String, Permission> permissions = service.ymlToPermissions(TEST_YML);

        assertThat(permissions).size().isEqualTo(2);

        assertEquals(createPermission(2, 1, 1, 3, 3), permissions.get("ROLE1:PRIVILEGE_KEY1"));
        assertEquals(createPermission(2, 3, 3, 3, 3), permissions.get("ROLE3:PRIVILEGE_KEY3"));
    }

    @Test
    public void testYmlToPermissions_shouldOverrideValue_whenPrivilegesHaveEqualKeys() {
        Map<String, Permission> permissions = service.ymlToPermissions(TEST_YML);

        assertThat(permissions).size().isEqualTo(3);

        assertEquals(createPermission(1, 2, 2, 2, 2), permissions.get("ROLE2:PRIVILEGE_KEY2"));
        assertEquals(createPermission(2, 3, 3, 3, 3), permissions.get("ROLE3:PRIVILEGE_KEY3"));
        // value for key ROLE1:PRIVILEGE_KEY1 is overridden with object from MS2, since we do not perform filtering by ms name
        assertEquals(createPermission(2, 1, 1, 3, 3), permissions.get("ROLE1:PRIVILEGE_KEY1"));
    }

    @Test
    public void testYmlToPermissionsList_shouldReturnAllPermissionsAsList() {
        List<Permission> permissions = service.ymlToPermissionsList(TEST_YML);

        assertThat(permissions).size().isEqualTo(4);

        assertThat(permissions).contains(createPermission(1, 1, 1, 1, 1));
        assertThat(permissions).contains(createPermission(1, 2, 2, 2, 2));
        assertThat(permissions).contains(createPermission(2, 1, 1, 3, 3));
        assertThat(permissions).contains(createPermission(2, 3, 3, 3, 3));
    }

    private Permission createPermission(int msNum, int roleNum, int privNum, int condNum, int resNum) {
        Permission perm = new Permission();
        perm.setRoleKey("ROLE" + roleNum);
        perm.setMsName("MS" + msNum);
        perm.setDisabled(false);
        perm.setDeleted(false);
        perm.setEnvCondition(parser.parseExpression("ENV_COND" + condNum));
        perm.setPrivilegeKey("PRIVILEGE_KEY" + privNum);
        perm.setReactionStrategy(ReactionStrategy.SKIP);
        perm.setResourceCondition(parser.parseExpression("RESOLUTION_START" + resNum));
        return perm;
    }
}