# Domain And HTTPS Follow-Up

This phase starts only after the Jenkins -> ECR -> EC2 deployment path is stable.

## Recommended Path

Use:

- a new domain managed in Route 53
- the existing app EC2 instance as the backend
- a reverse proxy on the app host for TLS termination

Recommended rollout:

1. Buy or transfer the domain into Route 53
2. Create a hosted zone
3. Point a subdomain such as `staging.<your-domain>` to the app EC2 public IP
4. Add a reverse proxy on the app EC2 host
5. Expose only `80` and `443` publicly
6. Route requests to:
   - `ingestion-service:8081`
   - `query-service:8082`
7. Enable automatic TLS certificates

## Why This Phase Is Separate

The current staging host is already running:

- Postgres
- Redis
- Kafka
- Ollama
- `ingestion-service`
- `query-service`

Separating domain work from deployment work keeps the first stable release path simple:

- first make Jenkins deployments repeatable
- then add public DNS, TLS, and reverse-proxy routing

## Reverse Proxy Choice

For the simplest next step, use Caddy on the app EC2 host because it can:

- request and renew Let's Encrypt certificates automatically
- proxy multiple services with minimal config
- reduce manual TLS management

## Security Changes To Make In That Phase

When domain mapping is added, tighten exposure:

- keep `22` open only for SSH administration
- open `80` and `443`
- close direct public access to:
  - `5432`
  - `6379`
  - `9092`
  - `11434`
- decide whether `8081` and `8082` remain temporarily public or move fully behind the proxy

## Suggested End State

```text
Internet -> Route 53 -> app-domain -> Caddy on EC2 -> Spring services
```

## Suggested Hostnames

- `staging.<your-domain>` -> query-service entrypoint
- `ingest.<your-domain>` -> ingestion-service entrypoint

or use one host with path routing if you prefer:

- `staging.<your-domain>/api/v1/query/...`
- `staging.<your-domain>/api/v1/ingestion/...`

## Before Starting This Phase

Confirm all of the following:

- Jenkins can deploy the exact ECR SHA tag to the app EC2 host
- the app EC2 host passes post-deploy health checks
- security groups are understood and under control
- you are ready to expose the app publicly beyond raw EC2 IP access
