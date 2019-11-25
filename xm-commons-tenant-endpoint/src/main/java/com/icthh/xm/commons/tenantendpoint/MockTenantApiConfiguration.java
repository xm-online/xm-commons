package com.icthh.xm.commons.tenantendpoint;

import com.icthh.xm.commons.gen.api.TenantsApiDelegate;
import com.icthh.xm.commons.gen.model.Tenant;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;

import java.util.List;

@Configuration
public class MockTenantApiConfiguration  {

    private static final String ERROR_MESSAGE = "Tenant api controller is not implemented for this microservice";

    @ConditionalOnMissingBean(TenantsApiDelegate.class)
    @Bean
    public TenantsApiDelegate tenantsApiDelegate() {
        return new TenantsApiDelegate() {
            @Override
            public ResponseEntity<Void> addTenant(Tenant body) {
                throw new NotImplementedException(ERROR_MESSAGE);
            }

            @Override
            public ResponseEntity<Void> deleteTenant(String tenantKey) {
                throw new NotImplementedException(ERROR_MESSAGE);
            }

            @Override
            public ResponseEntity<List<Tenant>> getAllTenantInfo() {
                throw new NotImplementedException(ERROR_MESSAGE);
            }

            @Override
            public ResponseEntity<Tenant> getTenant(String tenantKey) {
                throw new NotImplementedException(ERROR_MESSAGE);
            }

            @Override
            public ResponseEntity<Void> manageTenant(String tenantKey, String body) {
                throw new NotImplementedException(ERROR_MESSAGE);
            }
        };
    }

}
