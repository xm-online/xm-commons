package com.icthh.xm.commons.permission.mapper;

import com.icthh.xm.commons.permission.domain.Privilege;
import com.icthh.xm.commons.permission.domain.mapper.PrivilegeMapper;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class PrivilegeMapperUnitTest {

    private static final String CORRECT_YML = "---\n"
        + "MS1:\n" +
        "- key: \"KEY1\"\n" +
        "  description:\n" +
        "    en: \"test1\"\n" +
        "    ru: \"тест1\"\n" +
        "  resources:\n" +
        "  - \"res1\"\n" +
        "  - \"res2\"\n" +
        "  customDescription: \"customDescription1\"\n" +
        "- key: \"KEY2\"\n" +
        "  description:\n" +
        "    en: \"test2\"\n" +
        "    ru: \"тест2\"\n" +
        "  resources:\n" +
        "  - \"res1\"\n" +
        "  - \"res2\"\n" +
        "  customDescription: \"customDescription2\"\n" +
        "MS2:\n" +
        "- key: \"KEY3\"\n" +
        "  description:\n" +
        "    en: \"test3\"\n" +
        "    ru: \"тест3\"\n" +
        "  resources:\n" +
        "  - \"res1\"\n" +
        "  - \"res2\"\n" +
        "  customDescription: \"customDescription3\"\n";

    @Test
    public void testCollectionToYml() throws Exception {
        String yml = PrivilegeMapper.privilegesToYml(privileges());

        assertEquals(CORRECT_YML, yml);
    }


    @Test
    public void testYmlToCollection() throws Exception {
        Map<String, Set<Privilege>> privileges = PrivilegeMapper.ymlToPrivileges(CORRECT_YML);

        assertEquals(2, privileges.size());

        Iterator<Privilege> it = privileges.get("MS1").iterator();
        assertEquals(getPrivilege(1, 1), it.next());
        assertEquals(getPrivilege(2, 1), it.next());
        it = privileges.get("MS2").iterator();
        assertEquals(getPrivilege(3, 2), it.next());
    }

    @Test
    public void testError() throws Exception {
        assertNull(PrivilegeMapper.privilegesToYml(null));
        assertTrue(PrivilegeMapper.ymlToPrivileges(null).isEmpty());
    }

    private Collection<Privilege> privileges() {
        Set<Privilege> privileges = new HashSet<>();
        privileges.add(getPrivilege(2, 1));
        privileges.add(getPrivilege(3, 2));
        privileges.add(getPrivilege(1, 1));
        return privileges;
    }

    private Privilege getPrivilege(int num, int msNum) {
        Privilege privilege = new Privilege();
        privilege.setKey("KEY" + num);
        privilege.setMsName("MS" + msNum);
        privilege.getDescription().put("ru", "тест" + num);
        privilege.getDescription().put("en", "test" + num);
        privilege.getResources().add("res1");
        privilege.getResources().add("res2");
        privilege.setCustomDescription("customDescription" + num);
        return privilege;
    }
}
