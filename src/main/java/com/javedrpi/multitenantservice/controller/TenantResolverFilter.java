package com.javedrpi.multitenantservice.controller;

import com.javedrpi.multitenantservice.service.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Servlet filter that extracts the tenant ID from the X-Tenant-Id header
 * and sets it in TenantContext for downstream use by DynamicRoutingDataSource.
 */
@Component
public class TenantResolverFilter extends OncePerRequestFilter {

    private static final String TENANT_HEADER = "X-Tenant-Id";
    private static final String DEFAULT_TENANT = "tenant1";

    private final Environment env;
    private final Set<String> validTenants;

    public TenantResolverFilter(Environment env) {
        this.env = env;
        String tenantList = env.getProperty("multitenancy.tenants", "tenant1");
        this.validTenants = Set.of(tenantList.split(","));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String tenantId = request.getHeader(TENANT_HEADER);

        if (tenantId == null || tenantId.isBlank()) {
            tenantId = DEFAULT_TENANT;
        } else if (!validTenants.contains(tenantId)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Unknown tenant: " + tenantId + "\"}");
            return;
        }

        try {
            TenantContext.setCurrentTenant(tenantId);
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
