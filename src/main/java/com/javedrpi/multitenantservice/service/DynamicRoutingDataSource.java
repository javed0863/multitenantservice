package com.javedrpi.multitenantservice.service;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * Routes database connections to the correct tenant's DataSource
 * based on the current tenant ID stored in TenantContext.
 */
public class DynamicRoutingDataSource extends AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
        return TenantContext.getCurrentTenant();
    }
}
