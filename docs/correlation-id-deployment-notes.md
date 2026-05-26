# Deployment notes: correlation IDs and structured logging

## Overview

This change adds end-to-end **request correlation** and **structured JSON logging** across Online Boutique services. It is independent of OpenTelemetry tracing and works with the default Kubernetes manifests (tracing off).

## Contract

| Mechanism | Value |
|-----------|--------|
| HTTP request header | `X-Correlation-ID` |
| gRPC metadata key | `x-correlation-id` |
| Log fields | `correlation_id`, `service` |
| ID format | UUID v4 when not supplied by client |

The **frontend** accepts an incoming `X-Correlation-ID`, generates one if missing, echoes it on HTTP responses, and propagates it to downstream gRPC and HTTP calls.

## What changed

### New shared libraries

- `src/shared/correlation/` — Go (context, gRPC interceptors, logrus helpers)
- `src/shared/python/correlation/` — Python (contextvars, JSON logger, gRPC interceptors)
- `src/shared/nodejs/correlation/` — Node.js (AsyncLocalStorage, pino mixin, gRPC wrappers)

### Services updated

| Service | Logging | Correlation propagation |
|---------|---------|-------------------------|
| frontend | logrus JSON + `correlation_id` | HTTP entry; gRPC client interceptor |
| checkoutservice | logrus JSON | gRPC server + client interceptors |
| productcatalogservice | logrus JSON | gRPC server interceptor |
| shippingservice | logrus JSON | gRPC server interceptor |
| paymentservice | pino JSON | gRPC handler wrapper |
| currencyservice | pino JSON | gRPC handler wrapper |
| emailservice | python-json-logger | gRPC server interceptor |
| recommendationservice | python-json-logger | gRPC server + catalog client interceptors |
| shoppingassistantservice | python-json-logger | Flask `before_request` |
| cartservice | `ILogger` JSON console | gRPC `CorrelationIdInterceptor` |
| adservice | log4j + ThreadContext | gRPC server interceptor |

## Deployment requirements

### Rebuild all modified service images

Docker build contexts changed to include shared libraries:

- **Go services** (`frontend`, `checkoutservice`, `productcatalogservice`, `shippingservice`): Dockerfiles copy `../shared/correlation`.
- **Node services** (`paymentservice`, `currencyservice`): Dockerfiles copy `../shared/nodejs`.
- **Python services** (`emailservice`, `recommendationservice`, `shoppingassistantservice`): Dockerfiles copy `../shared/python` and set `PYTHONPATH`.

No Kubernetes manifest or environment variable changes are required.

### Recommended rollout order

1. Deploy **backend gRPC services** (cart, checkout, catalog, currency, payment, shipping, email, recommendation, ad).
2. Deploy **frontend** last (or together with backends) so outbound calls propagate IDs immediately.

Rolling update per Deployment is sufficient; no data migration.

## Operational notes

### Verifying correlation

```bash
# Health check with a fixed correlation ID
curl -s -D - -o /dev/null -H "X-Correlation-ID: deploy-test-001" http://<frontend-url>/_healthz

# Expect response header:
# X-Correlation-ID: deploy-test-001
```

During checkout, grep logs across pods:

```bash
kubectl logs -l app=frontend --tail=200 | grep deploy-test-001
kubectl logs -l app=checkoutservice --tail=200 | grep deploy-test-001
```

### Log format examples

**Go / Node (JSON stdout):**

```json
{"correlation_id":"…","service":"checkoutservice","message":"[PlaceOrder] user_id=…","severity":"info","timestamp":"…"}
```

**Python:**

```json
{"correlation_id": "…", "service": "emailservice", "severity": "INFO", "message": "listening on port: 8080", …}
```

### Compatibility

- **Backward compatible**: Clients that do not send `X-Correlation-ID` receive a generated ID.
- **OpenTelemetry**: Unchanged; `ENABLE_TRACING` behavior is not modified.
- **loadgenerator**: Does not send correlation IDs yet; each simulated request gets a new ID at the frontend.

## Rollback

Revert to previous image tags for affected Deployments. No persistent state or schema changes.

## Risk areas

- **Image build failures** if shared library paths are missing from Docker context (verify Skaffold/build uses service directory under `src/`).
- **cartservice**: Requires .NET SDK 10+ in CI/CD (unchanged from existing project target).
- **Python gRPC interceptors**: If an older `grpcio` is pinned without server interceptors, validate `emailservice` and `recommendationservice` startup in staging.

## Monitoring suggestions

- Create log-based filters on `jsonPayload.correlation_id` (Cloud Logging) or equivalent.
- Optionally alert on requests missing `correlation_id` in frontend access logs (should not occur after this change).
