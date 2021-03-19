package com.icthh.xm.commons.permission.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.icthh.xm.commons.permission.domain.Permission;
import com.icthh.xm.commons.permission.domain.ReactionStrategy;
import com.icthh.xm.commons.permission.domain.mapper.PermissionMapper;
import org.junit.Test;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PermissionMapperUnitTest {

    private static final String CORRECT_YML = "---\n"
        + "MS1:\n"
        + "  ROLE1:\n"
        + "  - privilegeKey: \"PRIVILEGE_KEY1\"\n"
        + "    disabled: false\n"
        + "    deleted: false\n"
        + "    envCondition: \"ENV_COND1\"\n"
        + "    resourceCondition: \"RESOLUTION_STRAT1\"\n"
        + "    reactionStrategy: \"SKIP\"\n"
        + "  ROLE2:\n"
        + "  - privilegeKey: \"PRIVILEGE_KEY2\"\n"
        + "    disabled: false\n"
        + "    deleted: false\n"
        + "    envCondition: \"ENV_COND2\"\n"
        + "    resourceCondition: \"RESOLUTION_STRAT2\"\n"
        + "    reactionStrategy: \"SKIP\"\n"
        + "MS2:\n"
        + "  ROLE3:\n"
        + "  - privilegeKey: \"PRIVILEGE_KEY3\"\n"
        + "    disabled: false\n"
        + "    deleted: false\n"
        + "    envCondition: \"ENV_COND3\"\n"
        + "    resourceCondition: \"RESOLUTION_STRAT3\"\n"
        + "    reactionStrategy: \"SKIP\"\n";

    private ExpressionParser parser = new SpelExpressionParser();

    @Test
    public void testCollectionToYml() throws Exception {
        String yml = PermissionMapper.permissionsToYml(permissions());

        assertEquals(CORRECT_YML, yml);
    }

    @Test
    public void testYmlToCollection() throws Exception {
        Map<String, Permission> permissions = PermissionMapper.ymlToPermissions(CORRECT_YML);

        assertEquals(3, permissions.size());

        assertEquals(getPermission(1, 1), permissions.get("ROLE1:PRIVILEGE_KEY1"));
        assertEquals(getPermission(2, 1), permissions.get("ROLE2:PRIVILEGE_KEY2"));
        assertEquals(getPermission(3, 2), permissions.get("ROLE3:PRIVILEGE_KEY3"));
    }

    @Test
    public void testError() throws Exception {
        assertNull(PermissionMapper.permissionsToYml(null));
        assertTrue(PermissionMapper.ymlToPermissions(null).isEmpty());
    }

    private Collection<Permission> permissions() {
        Set<Permission> permissions = new HashSet<>();
        permissions.add(getPermission(2, 1));
        permissions.add(getPermission(3, 2));
        permissions.add(getPermission(1, 1));
        return permissions;
    }

    private Permission getPermission(int num, int msNum) {
        Permission perm = new Permission();
        perm.setRoleKey("ROLE" + num);
        perm.setMsName("MS" + msNum);
        perm.setDisabled(false);
        perm.setDeleted(false);
        perm.setEnvCondition(parser.parseExpression("ENV_COND" + num));
        perm.setPrivilegeKey("PRIVILEGE_KEY" + num);
        perm.setReactionStrategy(ReactionStrategy.SKIP);
        perm.setResourceCondition(parser.parseExpression("RESOLUTION_STRAT" + num));
        return perm;
    }
}
