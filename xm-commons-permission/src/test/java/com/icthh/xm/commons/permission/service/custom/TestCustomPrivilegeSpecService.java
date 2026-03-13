package com.icthh.xm.commons.permission.service.custom;

import com.icthh.xm.commons.config.client.repository.CommonConfigRepository;
import java.util.List;

public class TestCustomPrivilegeSpecService extends AbstractCustomPrivilegeSpecService {
    TestCustomPrivilegeSpecService(CommonConfigRepository commonConfigRepository,
                                   List<CustomPrivilegesExtractor> privilegesExtractors) {
        super(commonConfigRepository, privilegesExtractors);
    }
}
