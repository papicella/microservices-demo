# Admin Service

Internal admin REST API for Online Boutique. Standalone Spring Boot 3.x service with in-memory configuration (no calls to other microservices).

## Requirements

- Java 21+ (Java 17+ may work; built and tested with Java 21)
- Maven 3.9+

## Run locally

```bash
cd src/adminservice
export ADMIN_API_KEY=dev-change-me   # optional; this is the default in application.yml
mvn spring-boot:run
```

Or build and run the JAR:

```bash
mvn -q test package
java -jar target/adminservice-0.1.0-SNAPSHOT.jar
```

## API

All `/api/v1/admin/*` endpoints require the `X-Admin-Api-Key` header.

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/admin/status` | Service name, version, uptime, config snapshot |
| GET | `/api/v1/admin/config` | Read in-memory admin config |
| PUT | `/api/v1/admin/config` | Replace admin config |

Actuator (no API key):

| Method | Path |
|--------|------|
| GET | `/actuator/health` |
| GET | `/actuator/info` |

### Examples

```bash
export ADMIN_API_KEY=dev-change-me

curl -H "X-Admin-Api-Key: $ADMIN_API_KEY" http://localhost:8080/api/v1/admin/config

curl -X PUT \
  -H "Content-Type: application/json" \
  -H "X-Admin-Api-Key: $ADMIN_API_KEY" \
  -d '{"maintenanceMode":true,"bannerMessage":"Deploy in progress","updatedBy":"ops"}' \
  http://localhost:8080/api/v1/admin/config
```

## Tests

```bash
mvn test
```

## Deployment

See [DEPLOYMENT.md](DEPLOYMENT.md) for build, environment variables, health checks, security guidance, and future Kubernetes notes.
