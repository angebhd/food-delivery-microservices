# Runbook

## Prerequisites

- Docker and Docker Compose
- Ports free: `5433`, `5672`, `15672`, `8080`

---

## Running with Docker Compose

```bash
# Build and start all services
docker compose up --build

# Start in background
docker compose up --build -d

# Stop all services
docker compose down

# Stop and remove volumes (wipes database data)
docker compose down -v
```

### Startup Order

Docker Compose respects `depends_on`, so services start in this order:

1. PostgreSQL + RabbitMQ (infrastructure)
2. Discovery Service (Eureka)
3. All microservices (after discovery is up)

Allow ~60 seconds for all services to register with Eureka before making requests.

### Network Isolation

Only the API Gateway and the infrastructure management UIs are exposed to the host. All microservices communicate over the internal Docker network and are not reachable from outside.

| Container | Host Port | Notes |
|-----------|-----------|-------|
| API Gateway | 8080 | Single external entry point |
| PostgreSQL | 5433 | Direct DB access for local dev |
| RabbitMQ | 5672, 15672 | Broker + management UI |
| All microservices | — | Internal only, accessed via gateway |
| Discovery Service | — | Internal only |

---

## Running a Single Service Locally

Each service has a local profile (`application.yaml`) that points to `localhost`. To run a service outside Docker:

1. Start infrastructure manually or use Docker for just infra:
   ```bash
   docker compose up postgres rabbitmq discovery-service
   ```

2. Run the service with the local profile:
   ```bash
   cd customer-service
   ./mvnw spring-boot:run
   ```

---

## Environment Configuration

Each service has two config files:

| File | Used when |
|------|-----------|
| `application.yaml` | Local development (localhost URLs) |
| `application-docker.yml` | Docker Compose (`SPRING_PROFILES_ACTIVE=docker`) |

### Key Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_PROFILES_ACTIVE` | — | Set to `docker` in compose.yml |
| `app.jwt.secret` | `mysecretkeymysecretkeymysecretkey...` | JWT signing key (change in production) |
| `app.jwt.expiration-ms` | `3600000` | Token TTL in ms (1 hour) |

---

**Verifying Services**

Since microservices are not exposed to the host, health checks go through the gateway or directly via `docker exec`:

```bash
# Via gateway (only gateway-routed paths work externally)
curl http://localhost:8080/api/actuator/health

# Direct container access for debugging
docker exec CUSTOMER-SERVICE curl -s http://localhost:8081/api/customers/actuator/health
docker exec ORDER-SERVICE curl -s http://localhost:8083/api/orders/actuator/health
docker exec RESTAURANT-SERVICE curl -s http://localhost:8084/api/restaurants/actuator/health
docker exec DELIVERY-SERVICE curl -s http://localhost:8082/api/deliveries/actuator/health
docker exec DISCOVERY-SERVICE curl -s http://localhost:8761/actuator/health
```

---

## Monitoring

### Circuit Breaker State

```bash
# Check all circuit breakers on a service
GET http://localhost:8080/api/actuator/circuitbreakers

# Check events (transitions, calls, failures)
GET http://localhost:8080/api/actuator/circuitbreakerevents
```

States: `CLOSED` (normal) → `OPEN` (failing, rejecting calls) → `HALF_OPEN` (testing recovery)

### Prometheus Metrics

Each service exposes `/actuator/prometheus`. Point your Prometheus scrape config at:

Since microservices are not exposed to the host, Prometheus must run inside the same Docker network and target containers by name:

```yaml
scrape_configs:
  - job_name: api-gateway
    static_configs:
      - targets: ['api-gateway:8080']
    metrics_path: /api/actuator/prometheus

  - job_name: customer-service
    static_configs:
      - targets: ['customer-service:8081']
    metrics_path: /api/customers/actuator/prometheus

  - job_name: order-service
    static_configs:
      - targets: ['order-service:8083']
    metrics_path: /api/orders/actuator/prometheus

  - job_name: restaurant-service
    static_configs:
      - targets: ['restaurant-service:8084']
    metrics_path: /api/restaurants/actuator/prometheus

  - job_name: delivery-service
    static_configs:
      - targets: ['delivery-service:8082']
    metrics_path: /api/deliveries/actuator/prometheus
```

---

## Docker Image Details

All services use a two-stage Dockerfile:

- **Stage 1 (builder):** `eclipse-temurin:21.0.8_9-jdk-jammy` — builds the JAR with Maven
- **Stage 2 (final):** `eclipse-temurin:21.0.8_9-jre-jammy` — lean runtime image

Dependencies are cached in a separate layer (`dependency:go-offline`) to speed up rebuilds.

---

## Common Issues

**Services not appearing in Eureka**
- Wait longer — services can take 30–60s to register
- Check that `SPRING_PROFILES_ACTIVE=docker` is set
- Verify `discovery-service` container is healthy first

**Database connection errors**
- Ensure `postgres` container is fully ready before services start
- Check `sql-scripts/init.sql` ran correctly (databases created)

**RabbitMQ connection refused**
- `rabbitmq` container may still be starting; services will retry
- Check RabbitMQ management UI at http://localhost:15672

**JWT 401 errors**
- Token may be expired (1h TTL) — re-login to get a fresh token
- Ensure `Authorization: Bearer <token>` header is present
