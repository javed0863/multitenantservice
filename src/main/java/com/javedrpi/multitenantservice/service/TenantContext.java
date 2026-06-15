package com.javedrpi.multitenantservice.service;

/**
 * ThreadLocal holder for the current tenant ID.
 * Each servlet request gets its own thread, so there is no cross-request leakage.
 * Falls back to "tenant1" when no tenant is set (e.g., during startup).
 */
public final class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();
    private static final String DEFAULT_TENANT = "tenant1";

    private TenantContext() {}

    public static void setCurrentTenant(String tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static String getCurrentTenant() {
        String tenant = CURRENT_TENANT.get();
        return (tenant != null) ? tenant : DEFAULT_TENANT;
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
