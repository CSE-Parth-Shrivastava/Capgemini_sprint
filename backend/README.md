# FinFlow Backend

Spring Boot microservices for the FinFlow Loan Management Platform.

## Services

| Service               | Port  | Description                                |
|-----------------------|-------|--------------------------------------------|
| `eureka-server`       | 8761  | Service registry                           |
| `config-server`       | 8888  | Centralised configuration                  |
| `api-gateway`         | 8080  | JWT auth + routing                         |
| `auth-service`        | 8081  | User registration, login, JWT issuance     |
| `application-service` | 8082  | Loan applications + credit scoring         |
| `document-service`    | 8083  | Document upload & verification             |
| `admin-service`       | 8084  | Admin decisions + reporting                |
| `notification-service`| 8085  | In-app + email notifications               |

## Prerequisites

- Java 17+
- Maven 3.9+
- MySQL 8.0+ (running locally or via a managed service)

## Running Locally

1. Start a MySQL instance and apply the schema:
   ```bash
   mysql -u root -p < init-db.sql
   ```

2. Copy `.env.example` to `.env` and fill in your credentials.

3. Start services in order:
   ```bash
   # From each service directory:
   mvn spring-boot:run
   ```
   Recommended startup order: eureka-server → config-server → api-gateway → others.

## Building

```bash
# Build all modules
mvn clean package -DskipTests

# Build + run tests
mvn clean verify
```

## Tests

JUnit 5 + Mockito tests are present in every service under `src/test/`.

```bash
# Run all tests across all modules
mvn test

# Run tests for a single service
cd auth-service && mvn test
```

Test configuration uses an **H2 in-memory database** so no external MySQL is required
to run tests. Test resources live in each service's `src/test/resources/application.yml`.

## Code Coverage (JaCoCo)

JaCoCo is configured in the parent POM. After `mvn verify`, per-module HTML reports
are available at `<module>/target/site/jacoco/index.html`.

Minimum enforced line coverage: **70%** (configurable via `jacoco.minimum.coverage`
in the parent POM).

## SonarQube

### Setup

1. Start a local SonarQube instance (requires Docker or a standalone install):
   ```
   http://localhost:9000  (default: admin/admin)
   ```

2. Generate a SonarQube token and export it:
   ```bash
   export SONAR_TOKEN=<your-token>
   ```

### Run Analysis

```bash
# Full build + test + coverage + SonarQube scan
mvn clean verify sonar:sonar -Psonar \
    -Dsonar.host.url=http://localhost:9000 \
    -Dsonar.token=$SONAR_TOKEN
```

Or override the host for a remote server:
```bash
mvn clean verify sonar:sonar -Psonar \
    -Dsonar.host.url=https://sonarcloud.io \
    -Dsonar.organization=<your-org> \
    -Dsonar.token=$SONAR_TOKEN
```

### What SonarQube Checks

- **Bugs** — logic errors detected statically
- **Vulnerabilities** — security hotspots (SQL injection, hardcoded secrets, etc.)
- **Code Smells** — maintainability issues (duplication, complexity)
- **Coverage** — from JaCoCo XML reports
- **Quality Gate** — build fails if gate is not passed (`sonar.qualitygate.wait=true`)

Excluded from analysis: `entity/**`, `dto/**`, `exception/**`, `*Application.java`, `config/**`
