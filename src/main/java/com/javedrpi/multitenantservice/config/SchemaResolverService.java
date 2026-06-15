package com.javedrpi.multitenantservice.config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility that maps tenant IDs to their database schema names.
 * Used by HibernateConfig for DDL default-schema resolution at startup,
 * and available as a lookup reference for any code that needs tenant→schema mapping.
 */
public final class SchemaResolverService {

    private static final Map<String, String> SCHEMA_MAPPING = new ConcurrentHashMap<>();

    static {
        SCHEMA_MAPPING.put("tenant1", "mydb");
        SCHEMA_MAPPING.put("tenant2", "mydb2");
    }

    private SchemaResolverService() {}

    public static String resolveSchemaName(String tenantId) {
        return SCHEMA_MAPPING.getOrDefault(tenantId, "mydb");
    }

    public static Map<String, String> getMapping() {
        return SCHEMA_MAPPING;
    }
}
