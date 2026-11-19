package com.icthh.xm.commons.metric;

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Counts incoming requests per tenant and per tenant client.
 * <ul>
 *     <li>{@value #TENANT_REQUESTS} with tags {@value #TAG_TENANT_CODE}, {@value #TAG_HTTP_STATUS_CODE}</li>
 *     <li>{@value #TENANT_CLIENT_REQUESTS} with tags {@value #TAG_TENANT_CODE}, {@value #TAG_CLIENT_ID},
 *     {@value #TAG_HTTP_STATUS_CODE}</li>
 * </ul>
 * Blank tenant is reported as {@value #UNKNOWN_TENANT}, blank client id as {@value #ANONYMOUS_CLIENT}.
 * Metric failures are logged and never propagated to the caller.
 */
@Slf4j
@RequiredArgsConstructor
public class TenantClientMetricObserver {

    public static final String TENANT_REQUESTS = "tenant.requests";
    public static final String TENANT_CLIENT_REQUESTS = "tenant.client.requests";

    public static final String TAG_TENANT_CODE = "tenantCode";
    public static final String TAG_CLIENT_ID = "clientId";
    public static final String TAG_HTTP_STATUS_CODE = "httpStatusCode";

    public static final String UNKNOWN_TENANT = "unknown";
    public static final String ANONYMOUS_CLIENT = "anonymous";

    private final MeterRegistry meterRegistry;

    /**
     * Increments request counters
     *
     * @param tenantCode     tenant key, may be null
     * @param clientId       OAuth2 client id from the token, may be null
     * @param httpStatusCode response HTTP status code
     */
    public void increment(String tenantCode, String clientId, int httpStatusCode) {
        try {
            String tenant = defaultIfBlank(tenantCode, UNKNOWN_TENANT);
            String client = defaultIfBlank(clientId, ANONYMOUS_CLIENT);
            String status = String.valueOf(httpStatusCode);

            Counter.builder(TENANT_REQUESTS)
                .description("Total number of requests per tenant")
                .tag(TAG_TENANT_CODE, tenant)
                .tag(TAG_HTTP_STATUS_CODE, status)
                .register(meterRegistry)
                .increment();

            Counter.builder(TENANT_CLIENT_REQUESTS)
                .description("Total number of requests per tenant and client")
                .tag(TAG_TENANT_CODE, tenant)
                .tag(TAG_CLIENT_ID, client)
                .tag(TAG_HTTP_STATUS_CODE, status)
                .register(meterRegistry)
                .increment();
        } catch (Exception e) {
            log.error("Tenant request metrics increment failed", e);
        }
    }
}
