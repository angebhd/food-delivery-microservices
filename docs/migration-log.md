# Migration Log

Tracks architectural decisions, refactors, and known issues.

---

## Services

| Service | Port | Database |
|---------|------|----------|
| discovery-service | 8761 | — |
| api-gateway | 8080 | — |
| customer-service | 8081 | customer_db |
| delivery-service | 8082 | delivery_db |
| order-service | 8083 | order_db |
| restaurant-service | 8084 | restaurant_db |

---

## Architectural Decisions

**ADR-001: JWT at the gateway, not per-service**
Authentication is handled exclusively at the API Gateway. Downstream services trust `X-Auth-User` and `X-Auth-Role` headers injected by the gateway. This avoids duplicating JWT validation logic across services and keeps the JWT secret in one place.

**ADR-002: Snapshot fields on orders**
`OrderEntity` stores `customerName`, `restaurantName`, `restaurantAddress`, and `itemName` as snapshot columns captured at order placement time. This decouples order history from live customer/restaurant data and avoids cross-domain joins.

**ADR-003: Async delivery assignment via RabbitMQ**
Delivery assignment is event-driven. The Order Service publishes `OrderPlacedEvent`; the Delivery Service consumes it and assigns a driver asynchronously. This prevents order placement from blocking on delivery availability.

**ADR-004: Cross-domain IDs, not foreign keys**
Services store cross-domain references as plain `BIGINT` ID columns. No shared database, no cross-database foreign keys. Live enrichment is done via Feign calls with circuit breaker fallbacks.

**ADR-005: FallbackFactory per Feign client**
Each Feign client has a dedicated `FallbackFactory` (not a simple `Fallback` class) to allow access to the root cause exception for structured logging and differentiated error handling.

**ADR-006: Rate limiting at the gateway**
IP-based rate limiting (2 req/10s) is applied at the API Gateway via Resilience4j RateLimiter before requests reach downstream services.

**ADR-007: Network isolation in Docker Compose**
Only the API Gateway (`:8080`), PostgreSQL (`:5433`), and RabbitMQ (`:5672`, `:15672`) are exposed to the host. All microservices and the Discovery Service communicate exclusively over the internal Docker network. Use `docker exec <CONTAINER> curl ...` for direct container debugging.

**ADR-008: `ddl-auto: update` for schema persistence**
Hibernate DDL mode is set to `update` so schema changes are applied incrementally and data is preserved across container restarts. For production, replace with Flyway or Liquibase managed migrations.

---

## Known Issues / Tech Debt

| ID | Description | Severity |
|----|-------------|----------|
| TD-001 | JWT secret is hardcoded in `application.yaml` — must be externalized via env var or secrets manager before production | High |
| TD-002 | Driver assignment in Delivery Service uses a hardcoded simulated pool — no real driver management | Medium |
| TD-003 | No distributed tracing (e.g., Zipkin/Micrometer Tracing) — cross-service request correlation is manual | Medium |
| TD-004 | No Grafana dashboard configured — Prometheus metrics are exported but not visualized | Low |
| TD-005 | `GET /api/customers/username/{username}` returns the full `CustomerEntity` including hashed password — should return a DTO | Medium |
| TD-006 | No pagination on list endpoints (`/my-orders`, `/search/all`, etc.) | Low |
| TD-007 | RabbitMQ credentials are hardcoded as `guest/guest` | Medium |

---

## Planned Improvements

- Replace `ddl-auto: update` with Flyway or Liquibase migrations
- Externalize all secrets via environment variables or a secrets manager
- Add distributed tracing with Micrometer Tracing + Zipkin
- Add Grafana dashboards for circuit breaker and request metrics
- Implement proper driver management in Delivery Service
- Add pagination to all list endpoints
- Harden `GET /api/customers/username/{username}` to return a safe DTO
