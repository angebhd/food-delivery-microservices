# Food Delivery Platform -- Microservices Architecture

A production-grade food delivery platform built with Spring Boot 4.0.3 and Spring Cloud 2025.1.0, decomposed into independently deployable microservices communicating via REST (OpenFeign) and asynchronous messaging (RabbitMQ).

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Technology Stack](#technology-stack)
3. [Services](#services)
   - [Discovery Service](#discovery-service)
   - [API Gateway](#api-gateway)
   - [Customer Service](#customer-service)
   - [Restaurant Service](#restaurant-service)
   - [Order Service](#order-service)
   - [Delivery Service](#delivery-service)
4. [Inter-Service Communication](#inter-service-communication)
5. [Circuit Breaker and Resilience](#circuit-breaker-and-resilience)
6. [Authentication and Security](#authentication-and-security)
7. [Asynchronous Messaging](#asynchronous-messaging)
8. [API Reference](#api-reference)
9. [Running the Application](#running-the-application)
10. [Monitoring and Observability](#monitoring-and-observability)
11. [Database Schema](#database-schema)
12. [Project Structure](#project-structure)

---

## Architecture Overview

```
                         +-------------------+
                         | Discovery Service |
                         |  (Eureka Server)  |
                         |    Port: 8761     |
                         +---------+---------+
                                   |
              Registry/Discovery   |
       +----------+----------+----+----+----------+
       |          |          |         |          |
+------+------+ +-+--------+ +--------+-+ +------+------+
|  API        | | Customer | | Order     | | Restaurant  |
|  Gateway    | | Service  | | Service   | | Service     |
|  Port: 8080 | | Port: 8081| | Port: 8083| | Port: 8084 |
+------+------+ +----------+ +-----+-----+ +------+------+
       |                            |              |
       |                     +------+------+       |
       |                     | Delivery    |       |
       |                     | Service     |       |
       |                     | Port: 8082  |       |
       |                     +------+------+       |
       |                            |              |
       +------- RabbitMQ -----------+--------------+
                (Port: 5672)
```

All services register with the Eureka Discovery Service. The API Gateway acts as the single entry point, routing external requests to the appropriate downstream service. Inter-service calls use OpenFeign with Resilience4j circuit breakers. Order-to-Delivery communication is event-driven via RabbitMQ.

---

## Technology Stack

| Component              | Technology                                      |
|------------------------|--------------------------------------------------|
| Runtime                | Java 21                                          |
| Framework              | Spring Boot 4.0.3                                |
| Cloud Framework        | Spring Cloud 2025.1.0                            |
| Service Discovery      | Netflix Eureka                                   |
| API Gateway            | Spring Cloud Gateway (WebMVC)                    |
| Inter-Service Calls    | Spring Cloud OpenFeign                           |
| Circuit Breaker        | Resilience4j (via Spring Cloud CircuitBreaker)    |
| Messaging              | RabbitMQ with Spring AMQP                        |
| Persistence            | Spring Data JPA with PostgreSQL                  |
| Security               | Spring Security with JWT (JJWT 0.13.0)          |
| Monitoring             | Spring Boot Actuator, Micrometer, Prometheus     |
| Build Tool             | Apache Maven                                     |
| Containerization       | Docker, Docker Compose                           |

---

## Services

### Discovery Service

| Property   | Value                          |
|------------|--------------------------------|
| Port       | 8761                           |
| Role       | Eureka Server for service registration and discovery |

All microservices register themselves with the Discovery Service on startup. Feign clients resolve service names (e.g., `CUSTOMER-SERVICE`) to physical addresses through the Eureka registry.

### API Gateway

| Property   | Value                          |
|------------|--------------------------------|
| Port       | 8080                           |
| Role       | Single entry point, JWT authentication, request routing |

The API Gateway handles all external traffic. It validates JWT tokens via a custom `JwtAuthenticationFilter`, injects `X-Auth-User` and `X-Auth-Role` headers into forwarded requests, and routes to downstream services using Spring Cloud Gateway with Eureka-based load balancing.

**Route Configuration:**

| Route Pattern          | Target Service      |
|------------------------|---------------------|
| `/api/auth/**`         | Handled locally     |
| `/api/customers/**`    | Customer Service    |
| `/api/orders/**`       | Order Service       |
| `/api/restaurants/**`  | Restaurant Service  |
| `/api/deliveries/**`   | Delivery Service    |

### Customer Service

| Property   | Value                          |
|------------|--------------------------------|
| Port       | 8081                           |
| Database   | `customer_db` (PostgreSQL)     |
| Role       | Customer registration, profile management, role promotion |

Manages customer accounts, including registration (with password hashing performed at the API Gateway), profile retrieval, profile updates, and promotion to `RESTAURANT_OWNER` role.

**Key DTOs:**

- `RegisterRequest` -- validated registration payload with username, email, password, and optional profile fields.
- `CustomerResponse` -- customer profile data including order count.
- `AuthResponse` -- JWT token with customer metadata.

### Restaurant Service

| Property   | Value                          |
|------------|--------------------------------|
| Port       | 8084                           |
| Database   | `restaurant_db` (PostgreSQL)   |
| Role       | Restaurant and menu management, ownership validation |

Manages restaurants and their menu items. Validates restaurant ownership by calling Customer Service via Feign. Enriches restaurant responses with owner name data.

**Key DTOs:**

- `RestaurantRequest` -- restaurant creation payload.
- `RestaurantResponse` -- restaurant data enriched with owner info and menu item count.
- `MenuItemRequest` -- menu item creation/update payload with validation.
- `MenuItemResponse` -- menu item details including restaurant association.

### Order Service

| Property   | Value                          |
|------------|--------------------------------|
| Port       | 8083                           |
| Database   | `order_db` (PostgreSQL)        |
| Role       | Order placement, order lifecycle management |

The central orchestration service. During order placement, it:

1. Validates the customer via Customer Service (Feign).
2. Validates the restaurant and menu items via Restaurant Service (Feign).
3. Computes pricing from menu item data.
4. Persists the order with snapshot fields (customer name, restaurant name/address, item names).
5. Publishes an `OrderPlacedEvent` to RabbitMQ for asynchronous delivery assignment.

When retrieving orders, delivery information is enriched via a Feign call to Delivery Service. If Delivery Service is unavailable, the order is still returned with delivery status marked as `UNAVAILABLE`.

**Key DTOs:**

- `PlaceOrderRequest` -- order placement payload with restaurant ID, item list, and optional delivery address override.
- `OrderItemRequest` -- individual order item with menu item ID, quantity, and special instructions.
- `OrderResponse` -- full order data including snapshot fields, item details, and delivery enrichment.
- `CustomerResponse` -- lightweight DTO mirroring customer data from Customer Service.
- `RestaurantResponse` -- lightweight DTO mirroring restaurant data from Restaurant Service.
- `MenuItemResponse` -- lightweight DTO mirroring menu item data from Restaurant Service.
- `DeliveryUpdateEvent` -- event consumed from RabbitMQ when delivery status changes.

### Delivery Service

| Property   | Value                          |
|------------|--------------------------------|
| Port       | 8082                           |
| Database   | `delivery_db` (PostgreSQL)     |
| Role       | Delivery assignment, driver management, delivery lifecycle |

Listens for `OrderPlacedEvent` messages from RabbitMQ to create delivery assignments asynchronously. Assigns a driver from a simulated pool, publishes `DeliveryUpdateEvent` messages back to RabbitMQ so the Order Service can track delivery progress.

**Key DTOs:**

- `DeliveryResponse` -- delivery data enriched with order/customer info via Feign.
- `OrderResponse` -- lightweight DTO mirroring order data from Order Service.
- `CustomerResponse` -- lightweight DTO mirroring customer data from Customer Service.
- `DeliveryUpdateEvent` -- event published to RabbitMQ on delivery status changes.

---

## Inter-Service Communication

### Synchronous (OpenFeign)

| Caller              | Target              | Feign Client             | Purpose                                     |
|---------------------|----------------------|--------------------------|---------------------------------------------|
| API Gateway         | Customer Service     | `CustomerInterface`      | Registration, login credential lookup        |
| Order Service       | Customer Service     | `CustomerInterface`      | Customer validation during order placement   |
| Order Service       | Restaurant Service   | `RestaurantInterface`    | Restaurant/menu validation and pricing       |
| Order Service       | Delivery Service     | `DeliveryInterface`      | Delivery info enrichment on order retrieval  |
| Restaurant Service  | Customer Service     | `CustomerInterface`      | Owner validation, role promotion             |
| Delivery Service    | Order Service        | `OrderInterface`         | Order info enrichment on delivery retrieval  |
| Delivery Service    | Customer Service     | `CustomerInterface`      | Customer info enrichment                     |

All Feign clients propagate `X-Auth-User`, `X-Auth-Role`, and `Authorization` headers via a shared `FeignConfig` interceptor.

### Asynchronous (RabbitMQ)

| Event                   | Producer       | Consumer         | Exchange         | Routing Key       |
|-------------------------|----------------|------------------|------------------|-------------------|
| Order Placed            | Order Service  | Delivery Service | `app.exchange`   | `order.placed`    |
| Order Cancelled         | Order Service  | Delivery Service | `app.exchange`   | `order.deleted`   |
| Delivery Status Update  | Delivery Service | Order Service  | `app.exchange`   | `delivery.update` |

Both services define their own queue and binding configurations:

- **Order Service** binds `order.queue` to `delivery.*` routing pattern (receives delivery updates).
- **Delivery Service** binds `delivery.queue` to `order.*` routing pattern (receives order events).

---

## Circuit Breaker and Resilience

All inter-service Feign calls are protected by Resilience4j circuit breakers integrated through Spring Cloud CircuitBreaker with `FallbackFactory` implementations.

### Configuration

Circuit breaker configuration is externalized in each service's `application-docker.yaml`:

| Parameter                                      | Value   |
|------------------------------------------------|---------|
| `slidingWindowSize`                            | 10      |
| `minimumNumberOfCalls`                         | 5       |
| `failureRateThreshold`                         | 50%     |
| `waitDurationInOpenState`                      | 10s     |
| `permittedNumberOfCallsInHalfOpenState`        | 3       |
| `automaticTransitionFromOpenToHalfOpenEnabled`  | true    |
| `timelimiter.timeoutDuration`                  | 3s      |

### Circuit Breaker Instances

| Service            | Instance Name        | Protected Feign Client    |
|--------------------|----------------------|---------------------------|
| API Gateway        | `customerService`    | `CustomerInterface`       |
| Order Service      | `customerService`    | `CustomerInterface`       |
| Order Service      | `restaurantService`  | `RestaurantInterface`     |
| Order Service      | `deliveryService`    | `DeliveryInterface`       |
| Restaurant Service | `customerService`    | `CustomerInterface`       |
| Delivery Service   | `orderService`       | `OrderInterface`          |
| Delivery Service   | `customerService`    | `CustomerInterface`       |

### Fallback Behavior

| Scenario                                 | Behavior                                                                                    |
|------------------------------------------|---------------------------------------------------------------------------------------------|
| Restaurant Service is DOWN               | Order placement is rejected with HTTP 503 and a clear business error message.               |
| Customer Service is DOWN                 | Order placement, login, and registration are rejected with HTTP 503.                        |
| Delivery Service is DOWN                 | Order creation succeeds; delivery is assigned asynchronously via RabbitMQ when the service recovers. Order retrieval returns delivery status as `UNAVAILABLE`. |
| Order Service is DOWN (from Delivery)    | Delivery retrieval succeeds; order enrichment fields are omitted.                           |
| Customer Service is DOWN (from Restaurant) | Restaurant creation/ownership validation fails with HTTP 503. Restaurant listing still returns data with owner name omitted. |

### Fallback Factories

Each Feign client has a corresponding `FallbackFactory` class that provides structured logging and domain-appropriate error handling:

| Service            | FallbackFactory Class                         |
|--------------------|-----------------------------------------------|
| API Gateway        | `CustomerInterfaceFallbackFactory`            |
| Order Service      | `CustomerInterfaceFallbackFactory`            |
| Order Service      | `RestaurantInterfaceFallbackFactory`          |
| Order Service      | `DeliveryInterfaceFallbackFactory`            |
| Restaurant Service | `CustomerInterfaceFallbackFactory`            |
| Delivery Service   | `OrderInterfaceFallbackFactory`               |
| Delivery Service   | `CustomerInterfaceFallbackFactory`            |

### Monitoring Circuit Breaker State

Circuit breaker states (CLOSED, OPEN, HALF_OPEN) are exposed via Spring Boot Actuator:

```
GET http://localhost:{port}/actuator/health
GET http://localhost:{port}/actuator/circuitbreakers
GET http://localhost:{port}/actuator/circuitbreakerevents
GET http://localhost:{port}/actuator/metrics/resilience4j.circuitbreaker.state
```

---

## Authentication and Security

### JWT Flow

1. Client sends `POST /api/auth/register` or `POST /api/auth/login` to the API Gateway.
2. The API Gateway authenticates the user (password verified via BCrypt), generates a JWT token signed with HMAC-SHA, and returns it.
3. For subsequent requests, the client includes the token in the `Authorization: Bearer <token>` header.
4. The `JwtAuthenticationFilter` in the API Gateway validates the token, extracts the username and role, and injects `X-Auth-User` and `X-Auth-Role` headers into the forwarded request.
5. Downstream services use a `SecurityContextFilter` to reconstruct the Spring Security `Authentication` object from the forwarded headers.

### Security Configuration

- The API Gateway permits unauthenticated access to `/api/auth/**`, restaurant search endpoints, and actuator endpoints.
- Downstream services trust the gateway's forwarded headers. The `SecurityContextFilter` populates the `SecurityContext` from `X-Auth-User` and `X-Auth-Role` headers.
- Customer Service permits unauthenticated access to `/api/customers/create` and `/api/customers/username/{username}` (used by the gateway during registration and login).

---

## Asynchronous Messaging

### RabbitMQ Configuration

| Resource     | Value             |
|--------------|-------------------|
| Exchange     | `app.exchange` (Topic Exchange) |
| Order Queue  | `order.queue` (bound to `delivery.*`) |
| Delivery Queue | `delivery.queue` (bound to `order.*`) |

Messages are serialized as JSON using `JacksonJsonMessageConverter`.

### Event Flow: Order Placement

1. Order Service saves the order and publishes to `app.exchange` with routing key `order.placed`.
2. Delivery Service's `DeliveryListener` consumes the message from `delivery.queue`.
3. Delivery Service creates a delivery assignment with a randomly selected driver.
4. Delivery Service publishes a `DeliveryUpdateEvent` to `app.exchange` with routing key `delivery.update`.
5. Order Service's `OrderListener` consumes the message from `order.queue` and updates the order status.

### Event Flow: Order Cancellation

1. Order Service publishes to `app.exchange` with routing key `order.deleted`.
2. Delivery Service consumes the cancellation event and updates the delivery status to `FAILED`.

---

## API Reference

### Authentication (API Gateway -- Port 8080)

| Method | Endpoint             | Auth     | Description               |
|--------|----------------------|----------|---------------------------|
| POST   | `/api/auth/register` | No       | Register a new customer   |
| POST   | `/api/auth/login`    | No       | Authenticate and get JWT  |

### Customers (Customer Service -- Port 8081)

| Method | Endpoint                              | Auth     | Description                    |
|--------|---------------------------------------|----------|--------------------------------|
| POST   | `/api/customers/create`               | No       | Create customer (internal)     |
| GET    | `/api/customers/me`                   | Yes      | Get authenticated user profile |
| GET    | `/api/customers/id/{id}`              | Yes      | Get customer by ID             |
| GET    | `/api/customers/username/{username}`  | No       | Get customer by username       |
| PUT    | `/api/customers/me`                   | Yes      | Update profile                 |
| PUT    | `/api/customers/make-restaurant-owner`| Yes      | Promote to restaurant owner    |

### Restaurants (Restaurant Service -- Port 8084)

| Method | Endpoint                              | Auth     | Description                      |
|--------|---------------------------------------|----------|----------------------------------|
| GET    | `/api/restaurants/search/city/{city}` | No       | Search restaurants by city       |
| GET    | `/api/restaurants/search/cuisine/{type}` | No    | Search by cuisine type           |
| GET    | `/api/restaurants/search/all`         | No       | List all active restaurants      |
| GET    | `/api/restaurants/{id}`               | Yes      | Get restaurant by ID             |
| GET    | `/api/restaurants/{id}/menu`          | No       | Get restaurant menu              |
| GET    | `/api/restaurants/menu/{id}`          | Yes      | Get menu item by ID              |
| POST   | `/api/restaurants`                    | Yes      | Create restaurant                |
| POST   | `/api/restaurants/{id}/menu`          | Yes      | Add menu item                    |
| PUT    | `/api/restaurants/menu/{itemId}`      | Yes      | Update menu item                 |
| PATCH  | `/api/restaurants/menu/{itemId}/toggle` | Yes    | Toggle menu item availability    |

### Orders (Order Service -- Port 8083)

| Method | Endpoint                              | Auth     | Description                    |
|--------|---------------------------------------|----------|--------------------------------|
| POST   | `/api/orders`                         | Yes      | Place a new order              |
| GET    | `/api/orders/{id}`                    | Yes      | Get order by ID                |
| GET    | `/api/orders/my-orders`               | Yes      | Get authenticated user orders  |
| GET    | `/api/orders/restaurant/{restaurantId}` | Yes    | Get orders for a restaurant    |
| PATCH  | `/api/orders/{id}/status`             | Yes      | Update order status            |
| POST   | `/api/orders/{id}/cancel`             | Yes      | Cancel an order                |

### Deliveries (Delivery Service -- Port 8082)

| Method | Endpoint                              | Auth     | Description                    |
|--------|---------------------------------------|----------|--------------------------------|
| GET    | `/api/deliveries/{id}`                | Yes      | Get delivery by ID             |
| GET    | `/api/deliveries/order/{orderId}`     | Yes      | Get delivery by order ID       |
| GET    | `/api/deliveries/status/{status}`     | Yes      | Get deliveries by status       |
| PATCH  | `/api/deliveries/{id}/status`         | Yes      | Update delivery status         |

---

## Running the Application

### Prerequisites

- Docker and Docker Compose installed.
- Ports 5433, 5672, 15672, 8080, 8081, 8082, 8083, 8084, and 8761 available.

### Start All Services

```bash
docker compose up --build
```

This starts the following containers:

| Container           | Port(s)          |
|---------------------|------------------|
| PostgreSQL          | 5433:5432        |
| RabbitMQ            | 5672, 15672      |
| Discovery Service   | 8761             |
| API Gateway         | 8080             |
| Customer Service    | 8081             |
| Delivery Service    | 8082             |
| Order Service       | 8083             |
| Restaurant Service  | 8084             |

### Stop All Services

```bash
docker compose down
```

### Verify Services

- Eureka Dashboard: `http://localhost:8761`
- RabbitMQ Management: `http://localhost:15672` (guest/guest)
- API Gateway Health: `http://localhost:8080/actuator/health`

---

## Monitoring and Observability

### Actuator Endpoints

Each service exposes the following actuator endpoints:

| Endpoint                          | Description                                      |
|-----------------------------------|--------------------------------------------------|
| `/actuator/health`                | Application health including circuit breaker state |
| `/actuator/circuitbreakers`       | All circuit breaker instances and their states    |
| `/actuator/circuitbreakerevents`  | Circuit breaker event log                        |
| `/actuator/metrics`               | Application metrics                              |
| `/actuator/prometheus`            | Prometheus-compatible metrics export             |

### Prometheus Integration

All services include `micrometer-registry-prometheus` for metrics export. Prometheus can scrape each service's `/actuator/prometheus` endpoint.

---

## Database Schema

The application uses four separate PostgreSQL databases, created automatically by `sql-scripts/init.sql`:

| Database         | Service            | Tables                      |
|------------------|--------------------|-----------------------------|
| `customer_db`   | Customer Service   | `customers`                 |
| `restaurant_db` | Restaurant Service | `restaurants`, `menu_items` |
| `order_db`      | Order Service      | `orders`, `order_items`     |
| `delivery_db`   | Delivery Service   | `deliveries`                |

All schemas are managed by Hibernate with `ddl-auto: create` (development mode). Each service maintains referential integrity within its own domain only. Cross-domain references are stored as ID fields (e.g., `customerId`, `orderId`, `restaurantId`).

---

## Project Structure

```
food-delivery-microservice/
|-- compose.yml
|-- sql-scripts/
|   +-- init.sql
|-- discovery-service/             # Eureka Server
|-- api-gateway/                   # Gateway + JWT Auth
|   +-- client/                    # Feign clients + fallback factories
|   +-- controller/                # AuthController
|   +-- dto/                       # AuthRequest, AuthResponse, CustomerDTO, RegisterRequest
|   +-- exception/                 # Global exception handling
|   +-- security/                  # JWT filter, SecurityConfig
|   +-- service/                   # AuthService (circuit-breaker protected)
|-- customer-service/
|   +-- controller/                # CustomerController
|   +-- dto/                       # RegisterRequest, CustomerResponse, AuthResponse
|   +-- entity/                    # CustomerEntity
|   +-- exception/                 # Global exception handling
|   +-- repository/                # CustomerRepository
|   +-- security/                  # SecurityContextFilter
|   +-- service/                   # CustomerService
|-- restaurant-service/
|   +-- client/                    # CustomerInterface + fallback factory
|   +-- controller/                # RestaurantController
|   +-- dto/                       # RestaurantRequest/Response, MenuItemRequest/Response, CustomerResponse
|   +-- entity/                    # RestaurantEntity, MenuItemEntity
|   +-- exception/                 # Global exception handling + ServiceUnavailableException
|   +-- repository/                # RestaurantRepository, MenuItemRepository
|   +-- security/                  # SecurityContextFilter
|   +-- service/                   # RestaurantService
|-- order-service/
|   +-- client/                    # CustomerInterface, RestaurantInterface, DeliveryInterface + fallback factories
|   +-- config/                    # FeignConfig, RabbitMQConfig, OrderQueueConfig
|   +-- controller/                # OrderController
|   +-- dto/                       # PlaceOrderRequest, OrderItemRequest, OrderResponse, CustomerResponse, RestaurantResponse, MenuItemResponse, DeliveryUpdateEvent
|   +-- entity/                    # OrderEntity, OrderItemEntity
|   +-- exception/                 # Global exception handling + ServiceUnavailableException
|   +-- repository/                # OrderRepository, OrderItemRepository
|   +-- security/                  # SecurityContextFilter
|   +-- service/                   # OrderService, OrderListener
|-- delivery-service/
|   +-- client/                    # OrderInterface, CustomerInterface + fallback factories
|   +-- config/                    # FeignConfig, RabbitMQConfig, DeliveryQueueConfig
|   +-- controller/                # DeliveryController
|   +-- dto/                       # DeliveryResponse, OrderResponse, CustomerResponse, DeliveryUpdateEvent, OrderDTO, RestaurantDTO
|   +-- entity/                    # DeliveryEntity
|   +-- exception/                 # Global exception handling + ServiceUnavailableException
|   +-- repository/                # DeliveryRepository
|   +-- security/                  # SecurityContextFilter
|   +-- service/                   # DeliveryService, DeliveryListener
```
