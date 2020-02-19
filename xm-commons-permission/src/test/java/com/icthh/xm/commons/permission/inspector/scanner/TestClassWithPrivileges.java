package com.icthh.xm.commons.permission.inspector.scanner;

import com.icthh.xm.commons.permission.annotation.FindWithPermission;
import com.icthh.xm.commons.permission.annotation.PrivilegeDescription;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;

public class TestClassWithPrivileges {

    @FindWithPermission("TEST.FIND.WITH.PERMISSION")
    @PrivilegeDescription("test description for find with permission method")
    public void testFindWithPermissionMethod() {

    }

    @PreAuthorize("hasPermission({'preAuthVar': #preAuthVar}, 'TEST.PRE.AUTHORIZE')")
    @PrivilegeDescription("test description for pre authorize method")
    public void testPreAuthorizeMethod(Object preAuthVar) {

    }

    @PostAuthorize("hasPermission({'postAuthVar': #postAuthVar}, 'TEST.POST.AUTHORIZE')")
    @PrivilegeDescription("test description for post authorize method")
    public void testPostAuthorizeMethod(Object postAuthVar) {

    }

    @PostFilter("hasPermission({'postFilter': #postFilter}, 'TEST.POST.FILTER')")
    @PrivilegeDescription("test description for post filter method")
    public void testPostFilterMethod(Object postFilter) {

    }

    @FindWithPermission("TEST.FIND.WITH.PERMISSION.WITHOUT.DESCRIPTION")
    public void testFindWithPermissionWithoutDescriptionMethod() {

    }

    @PreAuthorize("hasPermission({'preAuthVar': #preAuthVar}, 'TEST.PRE.AUTHORIZE.WITHOUT.DESCRIPTION')")
    public void testPreAuthorizeWithoutDescriptionMethod(Object preAuthVar) {

    }

    @PostAuthorize("hasPermission({'postAuthVar': #postAuthVar}, 'TEST.POST.AUTHORIZE.WITHOUT.DESCRIPTION')")
    public void testPostAuthorizeWithoutDescriptionMethod(Object postAuthVar) {

    }

    @PostFilter("hasPermission({'postFilter': #postFilter}, 'TEST.POST.FILTER.WITHOUT.DESCRIPTION')")
    public void testPostFilterWithoutDescriptionMethod(Object postFilter) {

    }

    @FindWithPermission("TEST.MANY.PRIVILEGES")
    @PreAuthorize("hasPermission({'preAuthVar': #preAuthVar}, 'TEST.MANY.PRIVILEGES')")
    @PrivilegeDescription("test description for method with many privileges annotation")
    public void testManyPrivilegesAnnotationMethod(Object preAuthVar) {

    }

    @FindWithPermission("TEST.MANY.PRIVILEGES.WITHOUT.DESCRIPTION")
    @PostFilter("hasPermission({'postFilter': #postFilter}, 'TEST.MANY.PRIVILEGES.WITHOUT.DESCRIPTION')")
    public void testManyPrivilegesAnnotationWithoutDescriptionMethod(Object postFilter) {

    }
}
