# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Spring Boot 4.1.0 (Java 25) multi-tenant service with per-schema database isolation using `X-Tenant-Id` header routing. MariaDB backend via Spring Data JPA and Spring Web MVC. Uses dual tenant schemas (`mydb`, `mydb2`) on a single MariaDB instance.

## Develop

| Task | Command |
|------|---------|
| Build | `./mvnw compile` |
| Package (executable jar) | `./mvnw package` |
| Run locally | `./mvnw spring-boot:run` |
| Run tests | `./mvnw test` |
| Docker MariaDB | `docker-compose up -d` / `docker-compose down` |

## Architecture Notes

- **Package**: `com.javedrpi.multitenantservice`
- **Entry Point**: `MultitenantserviceApplication` — excludes `DataSourceAutoConfiguration` (routing is manual in `HibernateConfig`).

### Package Structure

```
src/main/java/com/javedrpi/multitenantservice/
├── MultitenantserviceApplication.java    # @SpringBootApplication entry point
├── config/
│   ├── HibernateConfig.java              # Dual DataSource beans + DynamicRoutingDataSource + EMF + TxManager
│   └── SchemaResolverService.java        # Static tenant→schema mapping (tenant1→mydb, tenant2→mydb2)
├── controller/
│   ├── GreetingController.java           # REST CRUD: GET /api/greetings, POST /api/greetings
│   ├── TenantResolverFilter.java         # OncePerRequestFilter extracts X-Tenant-Id header
│   └── TenantResolverFilter.java NOT FOUND (likely deleted or needs creation)
├── model/
│   └── Greeting.java                     # Entity: id, message, createdAt
├── repository/
│   └── GreetingRepository.java           # JpaRepository<Greeting, Long>
├── service/
│   ├── TenantContext.java                # ThreadLocal<String> for current tenant ID
│   └── DynamicRoutingDataSource.java     # AbstractRoutingDataSource override (determineCurrentLookupKey)
```

### Multi-Tenancy Flow

```
HTTP Request (X-Tenant-Id header)
  → TenantResolverFilter extracts tenant, sets TenantContext.setCurrentTenant()
  → GreetingController / GreetingRepository operate normally (unaware of multi-tenancy)
  → DynamicRoutingDataSource.determineCurrentLookupKey() returns TenantContext.getCurrentTenant()
  → Routing DataSource resolves to correct HikariCP connection pool (mydb or mydb2)
  → Hibernate uses resolved schema for all DDL/DML
```

### Key Configuration

- **`application.yaml`**: Per-tenant datasource URLs, usernames, passwords under `multitenancy.*` prefix
- **`HibernateConfig`**: Creates two named DataSource beans (`mydbDs`, `mydb2Ds`), wires them into a `@Primary` `DynamicRoutingDataSource`, configures `EntityManagerFactory` and `PlatformTransactionManager` manually (auto-config excluded)
- **`SchemaResolverService`**: Static mapping table (`tenant1→mydb`, `tenant2→mydb2`) with fallback to `mydb`

### Database

MariaDB via docker-compose. Default credentials: root / `my_root_password`, database `mydb`, user `javed0863`.
Tenant schemas: `mydb` (tenant1), `mydb2` (tenant2). Schema auto-created with `?createDatabaseIfNotExist=true`.

### Test

Single baseline test (`MultitenantserviceApplicationTests`) — verifies Spring context loads. Uses `spring-boot-starter-data-jpa-test` and `spring-boot-starter-webmvc-test`.

### Dependencies

- spring-boot-starter-actuator
- spring-boot-starter-data-jpa
- spring-boot-starter-webmvc
- mariadb-java-client (runtime)
- lombok (optional)
