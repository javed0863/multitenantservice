# Multi-Tenant Spring Boot Service

A **Spring Boot 4.1.0** (Java 25) microservice demonstrating **per-schema database multi-tenancy** using MariaDB. Each tenant is isolated in its own database schema on a shared MariaDB instance, with routing handled automatically via the `X-Tenant-Id` HTTP header.

## Features

- **Per-schema multi-tenancy**: Tenant1 → `mydb`, Tenant2 → `mydb2` (both on one MariaDB)
- **Dynamic DataSource routing**: `AbstractRoutingDataSource` resolves the correct connection pool at runtime via a ThreadLocal tenant context
- **Tenant header filter**: `X-Tenant-Id` header extracted by `OncePerRequestFilter`; defaults to `tenant1`, rejects unknown tenants with 400
- **REST CRUD API**: `/api/greetings` — `GET` (list) and `POST` (create)
- **Hibernate auto-DDL** (`ddl-auto: update`) + SQL logging
- **Docker Compose** for MariaDB with healthcheck and persistent volume

## Tech Stack

| Layer          | Technology                        |
|----------------|-----------------------------------|
| Framework      | Spring Boot 4.1.0                 |
| Language       | Java 25                           |
| ORM            | Spring Data JPA / Hibernate       |
| Database       | MariaDB (via `mariadb-java-client`) |
| Connection Pool| HikariCP                          |
| Build          | Maven 3.9.16 (Maven Wrapper)      |

## Project Structure

```
src/main/java/com/javedrpi/multitenantservice/
├── MultitenantserviceApplication.java    # @SpringBootApplication (excludes DataSourceAutoConfiguration)
├── config/
│   ├── HibernateConfig.java              # Dual HikariDataSource beans + DynamicRoutingDataSource + EMF + TxManager
│   ├── NamingStrat.java                   # Snake_case physical naming strategy
│   └── SchemaResolverService.java         # Static tenant→schema mapping (tenant1→mydb, tenant2→mydb2)
├── controller/
│   ├── GreetingController.java            # REST CRUD: GET/POST /api/greetings
│   └── TenantResolverFilter.java          # OncePerRequestFilter extracts & validates X-Tenant-Id
├── model/
│   └── Greeting.java                      # Entity (id, message, createdAt)
├── repository/
│   └── GreetingRepository.java            # JpaRepository<Greeting, Long>
└── service/
    ├── DynamicRoutingDataSource.java      # AbstractRoutingDataSource routing impl
    └── TenantContext.java                 # ThreadLocal<String> for current tenant ID
```

## Multi-Tenancy Flow

```
HTTP Request (X-Tenant-Id header)
  → TenantResolverFilter extracts tenant, sets TenantContext.setCurrentTenant()
  → GreetingController / GreetingRepository operate normally (unaware of multi-tenancy)
  → DynamicRoutingDataSource.determineCurrentLookupKey() returns TenantContext.getCurrentTenant()
  → Routing DataSource resolves to correct HikariCP connection pool (mydb or mydb2)
  → Hibernate uses resolved schema for all DDL/DML
```

## Getting Started

### Prerequisites

- **Java 25+**
- **Maven 3.9+** (or use the included Maven Wrapper `./mvnw`)
- **Docker & Docker Compose** (for MariaDB)

### Quick Start

1. **Start MariaDB**:
   ```bash
   docker-compose up -d
   ```

2. **Build the project**:
   ```bash
   ./mvnw compile
   ```

3. **Run the application**:
   ```bash
   ./mvnw spring-boot:run
   ```

The app starts on `http://localhost:8080`.

### API Usage

| Method | Endpoint         | Header            | Body              | Description   |
|--------|------------------|-------------------|-------------------|---------------|
| GET    | `/api/greetings` | `X-Tenant-Id: tenant1` | _(none)_     | List all greetings for the tenant |
| POST   | `/api/greetings` | `X-Tenant-Id: tenant1` | `"Hello World"` | Create a new greeting |

**Example:**
```bash
# Create a greeting for tenant1
curl -X POST http://localhost:8080/api/greetings \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tenant1" \
  -d '"Hello from tenant1!"'

# List greetings for tenant2
curl http://localhost:8080/api/greetings \
  -H "X-Tenant-Id: tenant2"
```

### Configuration

**`application.yaml`** key settings:

| Property                          | Value                         |
|-----------------------------------|-------------------------------|
| `multitenancy.tenants`            | `tenant1,tenant2`             |
| `multitenancy.default-tenant`     | `tenant1`                     |
| `multitenancy.tenant1.datasource.url`    | `jdbc:mariadb://localhost:3306/mydb?createDatabaseIfNotExist=true` |
| `multitenancy.tenant2.datasource.url`    | `jdbc:mariadb://localhost:3306/mydb2?createDatabaseIfNotExist=true` |
| `spring.jpa.hibernate.ddl-auto`  | `update`                      |
| `spring.jpa.show-sql`            | `true`                        |

**MariaDB credentials** (docker-compose):
- **Root password**: `my_root_password`
- **Tenant schemas**: `mydb`, `mydb2` (auto-created)

## Docker Compose

```yaml
services:
  mariadb:
    image: mariadb:latest
    container_name: mariadb
    restart: unless-stopped
    ports: ["3306:3306"]
    environment:
      MARIADB_ROOT_PASSWORD: my_root_password
      MARIADB_DATABASE: mydb
      MARIADB_USER: javed0863
      MARIADB_PASSWORD: javed@123
    volumes:
      - mariadb_data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "healthcheck.sh", "--connect", "--innodb_initialized"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 30s
```

```bash
# Start
docker-compose up -d

# Stop
docker-compose down
```

## Build & Test

```bash
# Compile
./mvnw compile

# Package (executable jar)
./mvnw package

# Run tests
./mvnw test

# Run locally
./mvnw spring-boot:run
```

The packaged JAR is available at `target/multitenantservice-0.0.1-SNAPSHOT.jar`.

## Architecture Highlights

### DynamicRoutingDataSource

Extends Spring's `AbstractRoutingDataSource`. The `determineCurrentLookupKey()` method returns the current tenant ID from `TenantContext` (a ThreadLocal), which maps to one of two pre-configured HikariCP pools (`mydbDs`, `mydb2Ds`). This means every SQL query automatically targets the correct tenant schema — no manual datasource switching needed.

### TenantResolverFilter

A `OncePerRequestFilter` that:
1. Reads the `X-Tenant-Id` header from each request
2. Falls back to `tenant1` if absent or blank
3. Validates against the configured tenant list (returns 400 for unknown tenants)
4. Stores the tenant in `TenantContext` for the duration of the request

### SchemaResolverService

A lightweight static lookup service mapping tenant IDs to database schema names using a `ConcurrentHashMap`. Currently:

| Tenant ID | Schema Name |
|-----------|-------------|
| `tenant1` | `mydb`      |
| `tenant2` | `mydb2`     |

## Run with Docker (All-in-One)

Both the app and MariaDB can be containerized. Add a service to `docker-compose.yaml`:

```yaml
services:
  mariadb:
    # ... (as above)
  app:
    build: .
    ports: ["8080:8080"]
    environment:
      SPRING_DATASOURCE_URL: jdbc:mariadb://mariadb:3306/mydb?createDatabaseIfNotExist=true
    depends_on:
      mariadb:
        condition: service_healthy
```

## License

Private — Javed Ameen Shaikh

## Author

**Javed Ameen Shaikh** — [javedrpi.com](https://www.javedrpi.com/me)
