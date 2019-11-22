package com.icthh.xm.commons.tenantendpoint;

import com.icthh.xm.commons.gen.api.TenantsApiDelegate;
import com.icthh.xm.commons.gen.model.Tenant;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Configuration
public class MockTenantApiConfiguration  {

    @ConditionalOnMissingBean(TenantsApiDelegate.class)
    @Bean
    public TenantsApiDelegate tenantsApiDelegate() {
        return new TenantsApiDelegate() {
            @Override
            public ResponseEntity<Void> addTenant(Tenant body) {
                return null;
            }

            @Override
            public ResponseEntity<Void> deleteTenant(String tenantKey) {
                return null;
            }

            @Override
            public ResponseEntity<List<Tenant>> getAllTenantInfo() {
                return null;
            }

            @Override
            public ResponseEntity<Tenant> getTenant(String tenantKey) {
                return null;
            }

            @Override
            public ResponseEntity<Void> manageTenant(String tenantKey, String body) {
                return null;
            }
        };
    }

}
