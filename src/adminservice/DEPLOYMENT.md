# Admin Service — Deployment Notes

Standalone internal admin API for Online Boutique. This service is **not** wired into the repo’s Kubernetes manifests, Skaffold, or Helm charts in the current release.

## Deployment scope

| Environment | Status |
|-------------|--------|
| Local development | Supported (this document) |
| GKE / `kubernetes-manifests/` | Not included — add manifests in a follow-up if needed |
| Skaffold / CI image build | Not included |

## Prerequisites

- **JDK 21+** (matches project `java.version`; Java 17+ may work)
- **Maven 3.9+**
- Network access to Maven Central for first build

## Build

From the repository root:

```bash
cd src/adminservice
mvn -q test package
```

Artifact: `target/adminservice-0.1.0-SNAPSHOT.jar`

## Configuration

| Variable / property | Required | Default | Description |
|-------------------|----------|---------|-------------|
| `ADMIN_API_KEY` | Recommended (required in prod) | `dev-change-me` | Secret for `X-Admin-Api-Key` header on `/api/v1/admin/*` |
| `SERVER_PORT` | No | `8080` | HTTP listen port (Spring Boot standard) |

Example production-style run:

```bash
export ADMIN_API_KEY="$(openssl rand -hex 32)"
export SERVER_PORT=8080
java -jar target/adminservice-0.1.0-SNAPSHOT.jar
```

**Do not** use the default `dev-change-me` API key outside local development.

## Run locally

```bash
cd src/adminservice
export ADMIN_API_KEY=dev-change-me
mvn spring-boot:run
```

## Health and readiness

Use Spring Boot Actuator (no API key):

| Endpoint | Use |
|----------|-----|
| `GET /actuator/health` | Liveness / readiness probe candidate |
| `GET /actuator/info` | Build metadata |

Admin API (requires `X-Admin-Api-Key`):

| Endpoint | Use |
|----------|-----|
| `GET /api/v1/admin/status` | Service uptime + current in-memory config |

## Post-deploy verification

```bash
# Health (no auth)
curl -sf http://localhost:8080/actuator/health

# Admin config (auth required)
curl -sf -H "X-Admin-Api-Key: $ADMIN_API_KEY" \
  http://localhost:8080/api/v1/admin/config
```

## Security notes

- **Internal only**: Do not expose port 8080 on a public load balancer without additional controls (mTLS, VPN, or private cluster networking).
- **API key**: Store `ADMIN_API_KEY` in a secrets manager (e.g. GCP Secret Manager, K8s Secret) when deploying to a cluster.
- **State**: Configuration is **in-memory**; pod restarts reset `maintenanceMode` and `bannerMessage` to defaults.
- **No integration**: This service does not call other Boutique microservices; it does not affect checkout or frontend traffic until other services are wired to read its config (future work).

## Future cluster deployment (optional follow-up)

When adding Kubernetes support, suggested defaults:

- **Service type**: `ClusterIP` (no public ingress)
- **Port**: `8080`
- **Probes**: `GET /actuator/health` on port 8080
- **Secret**: mount `ADMIN_API_KEY` from a K8s Secret into env
- **Resources** (starting point): `requests: cpu 100m, memory 256Mi`; `limits: cpu 500m, memory 512Mi`
- **NetworkPolicy**: allow ingress only from operator namespaces or a bastion; deny from `frontend` / public tiers unless explicitly required

Add to `skaffold.yaml`, `kubernetes-manifests/`, and `kustomization.yaml` in a separate change to keep this PR focused on the application.

## Rollback

- **Local/JAR**: stop the process and redeploy the previous JAR tag.
- **Config**: no persistent store; rollback of admin flags is a `PUT /api/v1/admin/config` with prior values or process restart.

## Troubleshooting

| Symptom | Likely cause | Action |
|---------|--------------|--------|
| `401` on admin API | Missing/wrong `X-Admin-Api-Key` | Set `ADMIN_API_KEY` and pass matching header |
| `release version 21 not supported` | JDK &lt; 21 | Use JDK 21+ or adjust `java.version` in `pom.xml` |
| Port in use | Another process on 8080 | Set `SERVER_PORT` to a free port |
