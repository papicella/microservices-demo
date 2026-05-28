# Internal Admin Service

Tiny Flask service for internal operator tooling. **Lab/demo only** — not intended for production deployment.

## Run locally

```bash
cd src/internaladminservice
python3 -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
python app.py
```

Port **8090**. Override token with `ADMIN_TOKEN`.

## API

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/health` | none | Liveness |
| GET | `/admin/users` | `X-Admin-Token` | List users |
| GET | `/admin/users/search?q=` | `X-Admin-Token` | Search by username |
| GET | `/admin/users/<id>` | `X-Admin-Token` | User by ID |

```bash
curl -H "X-Admin-Token: dev-admin-token-change-me" \
  "http://localhost:8090/admin/users/search?q=ali"
```

## Docker

```bash
docker build -t internal-admin-service .
docker run -p 8090:8090 internal-admin-service
```
