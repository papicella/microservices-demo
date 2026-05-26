# Correlation ID contract

All services in this demo use a shared request correlation contract:

| Item | Value |
|------|--------|
| HTTP header | `X-Correlation-ID` |
| gRPC metadata | `x-correlation-id` |
| JSON log field | `correlation_id` |
| Service field | `service` |

If a client does not send a correlation ID, the receiving service generates a UUID v4.

Language-specific helpers live alongside this package:

- Go: `src/shared/correlation` (this module)
- Python: `src/shared/python/correlation`
- Node.js: `src/shared/nodejs/correlation`
