package com.icthh.xm.commons.permission.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.icthh.xm.commons.permission.domain.Role;
import com.icthh.xm.commons.permission.domain.mapper.RoleMapper;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

public class RoleMapperUnitTest {

    private static final String CORRECT_YML = "---\n"
        + "KEY1:\n"
        + "  description: \"test1\"\n"
        + "  createdDate: \"C_DATE1\"\n"
        + "  createdBy: \"C_BY1\"\n"
        + "  updatedDate: \"U_DATE1\"\n"
        + "  updatedBy: \"U_BY1\"\n"
        + "KEY2:\n"
        + "  description: \"test2\"\n"
        + "  createdDate: \"C_DATE2\"\n"
        + "  createdBy: \"C_BY2\"\n"
        + "  updatedDate: \"U_DATE2\"\n"
        + "  updatedBy: \"U_BY2\"\n"
        + "KEY3:\n"
        + "  description: \"test3\"\n"
        + "  createdDate: \"C_DATE3\"\n"
        + "  createdBy: \"C_BY3\"\n"
        + "  updatedDate: \"U_DATE3\"\n"
        + "  updatedBy: \"U_BY3\"\n";

    @Test
    public void testCollectionToYml() throws Exception {
        String yml = RoleMapper.rolesToYml(roles());

        assertEquals(CORRECT_YML, yml);
    }

    @Test
    public void testYmlToCollection() throws Exception {
        Map<String, Role> roles = RoleMapper.ymlToRoles(CORRECT_YML);

        assertEquals(3, roles.size());

        assertEquals(getRole(1), roles.get("KEY1"));
        assertEquals(getRole(2), roles.get("KEY2"));
        assertEquals(getRole(3), roles.get("KEY3"));
    }

    @Test
    public void testError() throws Exception {
        assertNull(RoleMapper.rolesToYml(null));
        assertTrue(RoleMapper.ymlToRoles(null).isEmpty());
    }

    private Collection<Role> roles() {
        Set<Role> roles = new HashSet<>();
        roles.add(getRole(2));
        roles.add(getRole(3));
        roles.add(getRole(1));
        return roles;
    }

    private Role getRole(int num) {
        Role role = new Role();
        role.setKey("KEY" + num);
        role.setDescription("test" + num);
        role.setCreatedDate("C_DATE" + num);
        role.setCreatedBy("C_BY" + num);
        role.setUpdatedDate("U_DATE" + num);
        role.setUpdatedBy("U_BY" + num);
        return role;
    }
}
