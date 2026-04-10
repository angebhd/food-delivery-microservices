# Architecture

## System Overview

```text
               [ External Clients (Browser/Mobile) ]
                               │
                               ▼ 1. Request enters
                     ┌───────────────────┐               ┌─────────────────────┐
                     │    API Gateway    │◄── 2. Ask ────┤  Discovery Service  │
                     │    (Port: 8080)   │    Where?     │    (Eureka)         │
                     └────────┬──────────┘               └─────────┬───────────┘
                              │                                    │ All apps
                              │ 3. Forward to                      │ register
                              │    correct service                 │ here
                              ▼                                    │
          ┌────────────────────────────────────────────────────────┴────┐
          │                                                             │
          │                   THE MICROSERVICES                         │
          │                                                             │
          │   ┌──────────────┐                  ┌──────────────┐        │
          │   │  Restaurant  ├──────(HTTP)─────▶│   Customer   │        │
          │   │  Service     │                  │   Service    │        │
          │   └──────▲───────┘                  └──────▲───────┘        │
          │          │                                 │                │
          │        (HTTP)                            (HTTP)             │
          │          │                                 │                │
          │   ┌──────┴───────┐                  ┌──────┴───────┐        │
          │   │   Order      │                  |   Delivery   │        │
          │   │   Service    │                  │   Service    │        │
          │   └───┬───────▲──┘                  └───┬───────▲──┘        │
          │       │       │                         │       │           │
          └───────┼───────┼─────────────────────────┼───────┼───────────┘
                  │       │                         │       │
      4. Publish  │       │ 6. Listen & Update      │       │ 5. Listen & 
      Event       ▼       │                         ▼       │    Process
           ┌──────────────┴─────────────────────────┴───────────────┐
           │                     RabbitMQ                           │
           │                 (Event Notice Board)                   │
           └────────────────────────────────────────────────────────┘
```
*(Note: Every microservice also has its own connection to PostgreSQL to store its specific data, but that is omitted here so the communication flow is clearer).*

Inter-Service Relationships (Synchronous OpenFeign):
- Gateway    ➔ Customer
- Order      ➔ Customer, Restaurant, Delivery
- Delivery   ➔ Order, Customer
- Restaurant ➔ Order, Customer
- Customer   ➔ Order

## Request Flow

```text
Client
  │
  ▼
API Gateway (:8080)
  ├── JwtAuthenticationFilter  →  validates Bearer token
  ├── IpRateLimiterFilter      →  2 req / 10s per IP
  ├── injects X-Auth-User, X-Auth-Role headers
  │
  ├── /api/auth/**             →  handled locally (AuthController)
  ├── /api/customers/**        →  lb://customer-service
  ├── /api/orders/**           →  lb://order-service
  ├── /api/restaurants/**      →  lb://restaurant-service
  ├── /api/deliveries/**       →  lb://delivery-service
  └── /eureka/**               →  http://discovery-service:8761 (Eureka dashboard/static)
```

## Synchronous Communication (OpenFeign)

All Feign clients propagate `X-Auth-User`, `X-Auth-Role`, and `Authorization` headers via a shared `FeignConfig` interceptor.

```text
API Gateway        ──Feign──▶  Customer Service   (register / login lookup)
Order Service      ──Feign──▶  Customer Service   (validate customer)
Order Service      ──Feign──▶  Restaurant Service (validate items + pricing)
Order Service      ──Feign──▶  Delivery Service   (enrich order with delivery info)
Restaurant Service ──Feign──▶  Customer Service   (validate owner role)
Restaurant Service ──Feign──▶  Order Service
Delivery Service   ──Feign──▶  Order Service      (enrich delivery with order info)
Delivery Service   ──Feign──▶  Customer Service   (enrich delivery with customer info)
Customer Service   ──Feign──▶  Order Service
```

## Asynchronous Communication (RabbitMQ)

Exchange: `app.exchange` (Topic Exchange)

```text
Order Service
  │
  ├── routing key: order.placed   ──▶  delivery.queue  ──▶  Delivery Service
  │                                     (creates delivery assignment)
  │
  ├── routing key: order.updated  ──▶  delivery.queue  ──▶  Delivery Service
  │                                     (adjusts delivery details)
  │
  └── routing key: order.deleted  ──▶  delivery.queue  ──▶  Delivery Service
                                        (marks delivery FAILED)

Delivery Service
  │
  └── routing key: delivery.update ──▶  order.queue  ──▶  Order Service
                                         (updates order status)
```

### Event Schemas

**OrderPlacedEvent** (order → delivery)
```json
{
  "orderId": 1,
  "customerId": 42,
  "restaurantId": 7,
  "deliveryAddress": "123 Main St",
  "restaurantAddress": "456 Oak Ave"
}
```

**DeliveryUpdateEvent** (delivery → order)
```json
{
  "orderId": 1,
  "deliveryId": 99,
  "status": "ASSIGNED"
}
```

## Circuit Breaker Configuration

All Feign calls are wrapped with Resilience4j circuit breakers.

| Parameter | Value |
|-----------|-------|
| slidingWindowSize | 10 |
| minimumNumberOfCalls | 5 |
| failureRateThreshold | 50% (60% for deliveryService) |
| waitDurationInOpenState | 10s |
| permittedNumberOfCallsInHalfOpenState | 3 |
| timelimiter.timeoutDuration | 3s (30s for Gateway, 5s for deliveryService) |

### Fallback Behavior

| Scenario | Behavior |
|----------|----------|
| Customer Service DOWN | Registration, login, order placement → HTTP 503 |
| Restaurant Service DOWN | Order placement → HTTP 503 |
| Delivery Service DOWN | Order still created; delivery status shown as `UNAVAILABLE` |
| Order Service DOWN (from Delivery) | Delivery returned without order enrichment |
| Customer Service DOWN (from Restaurant) | Restaurant listing returns data with owner name omitted |

## Security Model

```text
1. POST /api/auth/register  →  API Gateway hashes password (BCrypt), calls Customer Service
2. POST /api/auth/login     →  API Gateway verifies BCrypt hash, issues JWT (HMAC-SHA, 1h TTL)
3. Subsequent requests      →  Bearer token validated by JwtAuthenticationFilter
4. Forwarded requests       →  X-Auth-User + X-Auth-Role headers injected
5. Downstream services      →  SecurityContextFilter reconstructs Authentication from headers
```

JWT config:
- Secret: externalized via `app.jwt.secret`
- Expiry: 3600000ms (1 hour)

## Rate Limiting

Configured on the API Gateway via Resilience4j RateLimiter:
- 2 requests per 10 seconds per IP
- Timeout: 10s (threads wait up to 10s for token before rejecting)

## Project Structure

```text
food-delivery-microservice/
├── compose.yml
├── sql-scripts/init.sql
├── docs/                          ← documentation
├── discovery-service/             ← Eureka Server
├── api-gateway/                   ← Gateway + JWT Auth
│   ├── client/                    ← Feign clients + fallback factories
│   ├── controller/                ← AuthController
│   ├── dto/
│   ├── exception/
│   ├── security/                  ← JwtAuthenticationFilter, IpRateLimiterFilter
│   └── service/                   ← AuthService
├── customer-service/
│   ├── controller/
│   ├── dto/
│   ├── entity/                    ← CustomerEntity
│   ├── repository/
│   ├── security/                  ← SecurityContextFilter
│   └── service/
├── restaurant-service/
│   ├── client/
│   ├── controller/
│   ├── dto/
│   ├── entity/                    ← RestaurantEntity, MenuItemEntity
│   ├── repository/
│   ├── security/
│   └── service/
├── order-service/
│   ├── client/
│   ├── config/                    ← FeignConfig, RabbitMQConfig, OrderQueueConfig
│   ├── controller/
│   ├── dto/
│   ├── entity/                    ← OrderEntity, OrderItemEntity
│   ├── repository/
│   ├── security/
│   └── service/                   ← OrderService, OrderListener
└── delivery-service/
    ├── client/
    ├── config/                    ← FeignConfig, RabbitMQConfig, DeliveryQueueConfig
    ├── controller/
    ├── dto/
    ├── entity/                    ← DeliveryEntity
    ├── repository/
    ├── security/
    └── service/                   ← DeliveryService, DeliveryListener
```
