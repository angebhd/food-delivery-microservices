# Food Delivery Platform -- Microservices Architecture

## Table of Contents

1. [Project Overview](#project-overview)
2. [Architecture](#architecture)
3. [Technology Stack](#technology-stack)
4. [Service Catalog](#service-catalog)
5. [API Contracts](#api-contracts)
6. [Event-Driven Messaging](#event-driven-messaging)
7. [Security Model](#security-model)
8. [Database Design](#database-design)
9. [Getting Started](#getting-started)
10. [Migration Decision Log](#migration-decision-log)
11. [End-to-End Test Flow](#end-to-end-test-flow)

---

## Project Overview

This project is a food delivery platform decomposed from a monolithic Spring Boot application into a set of independently deployable microservices. Each service owns its domain, manages its own database, and communicates with other services through REST (OpenFeign) and asynchronous messaging (RabbitMQ).

The platform supports the full food delivery lifecycle: customer registration and authentication, restaurant and menu management, order placement and tracking, and delivery assignment and status updates.

---

## Architecture

```
                          Client (Browser / Postman)
                                    |
                                    v
                    +-------------------------------+
                    |    API Gateway (:8080)         |
                    |  JWT Auth Filter               |
                    |  Route Predicates              |
                    +-------------------------------+
                      |        |        |        |
        +-------------+  +----+----+  +-+------+ +----------+
        |                |          |            |            |
        v                v          v            v            v
  +-----------+  +------------+  +----------+  +-----------+  +-----------+
  | Customer  |  | Restaurant |  |  Order   |  | Delivery  |  | Discovery |
  | Service   |  | Service    |  | Service  |  | Service   |  | Service   |
  | :8081     |  | :8084      |  | :8083    |  | :8082     |  | :8761     |
  +-----------+  +------------+  +----------+  +-----------+  +-----------+
        |              |              |              |
        v              v              v              v
  +-----------+  +------------+  +----------+  +-----------+
  |customer_db|  |restaurant_db| | order_db |  |delivery_db|
  +-----------+  +------------+  +----------+  +-----------+
                                      |              |
                                      v              v
                              +---------------------------+
                              |       RabbitMQ (:5672)    |
                              |   Topic Exchange:         |
                              |     app.exchange          |
                              +---------------------------+
```

### Inter-Service Communication

| Source Service    | Target Service      | Method    | Purpose                                      |
|-------------------|---------------------|-----------|----------------------------------------------|
| API Gateway       | Customer Service    | Feign     | Register, login, fetch customer by username   |
| Restaurant Service| Customer Service    | Feign     | Validate owner, promote role, fetch by ID     |
| Order Service     | Customer Service    | Feign     | Validate customer at order placement          |
| Order Service     | Restaurant Service  | Feign     | Validate restaurant and menu items            |
| Order Service     | Delivery Service    | Feign     | Enrich order responses with delivery info     |
| Delivery Service  | Order Service       | Feign     | Enrich delivery responses with order info     |
| Delivery Service  | Customer Service    | Feign     | Fetch customer details for delivery context   |
| Order Service     | Delivery Service    | RabbitMQ  | Publish OrderPlacedEvent, OrderCancelledEvent |
| Delivery Service  | Order Service       | RabbitMQ  | Publish DeliveryStatusUpdatedEvent            |

---

## Technology Stack

| Component              | Technology                                        |
|------------------------|---------------------------------------------------|
| Language               | Java 21                                           |
| Framework              | Spring Boot 4.0.3                                 |
| Cloud                  | Spring Cloud 2025.1.0                             |
| Service Discovery      | Spring Cloud Netflix Eureka                       |
| API Gateway            | Spring Cloud Gateway (WebMVC)                     |
| Inter-Service REST     | Spring Cloud OpenFeign                            |
| Message Broker         | RabbitMQ 3 (Management)                           |
| Database               | PostgreSQL (one database per service)             |
| ORM                    | Spring Data JPA / Hibernate                       |
| Authentication         | JWT (issued at API Gateway, validated per request)|
| Fault Tolerance        | Resilience4j Circuit Breaker (dependency included)|
| Containerization       | Docker, Docker Compose                            |
| Build Tool             | Apache Maven                                      |

---

## Service Catalog

### Discovery Service

| Property   | Value                    |
|------------|--------------------------|
| Port       | 8761                     |
| Database   | None                     |
| Purpose    | Eureka service registry  |
| Dashboard  | http://localhost:8761    |

All microservices register with Eureka on startup and use logical service names for inter-service calls (e.g., `CUSTOMER-SERVICE`, `RESTAURANT-SERVICE`).

---

### API Gateway

| Property   | Value                         |
|------------|-------------------------------|
| Port       | 8080                          |
| Database   | None                          |
| Purpose    | Single entry point, JWT auth, routing |

**Route Configuration:**

| Route Pattern          | Target Service     |
|------------------------|--------------------|
| `/api/auth/**`         | Handled locally    |
| `/api/customers/**`    | Customer Service   |
| `/api/restaurants/**`  | Restaurant Service |
| `/api/orders/**`       | Order Service      |
| `/api/deliveries/**`   | Delivery Service   |
| `/eureka/**`           | Discovery Service  |

The gateway validates JWT tokens and propagates `X-Auth-User` and `X-Auth-Role` headers to downstream services. Downstream services reconstruct the Spring Security context from these headers.

---

### Customer Service

| Property   | Value              |
|------------|--------------------|
| Port       | 8081               |
| Database   | customer_db        |
| Purpose    | Customer registration, profiles, role management |

**Entities:**

- `CustomerEntity` -- Stores customer profile, credentials, role (`CUSTOMER`, `RESTAURANT_OWNER`, `ADMIN`), and a list of order IDs.

**DTOs:**

| DTO               | Purpose                                          |
|--------------------|--------------------------------------------------|
| `RegisterRequest`  | Customer registration payload with validation    |
| `CustomerResponse` | Customer profile response with order count       |
| `AuthResponse`     | JWT token and basic customer info after login    |

---

### Restaurant Service

| Property   | Value              |
|------------|--------------------|
| Port       | 8084               |
| Database   | restaurant_db      |
| Purpose    | Restaurant CRUD, menu item management            |

**Entities:**

- `RestaurantEntity` -- Stores restaurant details, `ownerId` (references Customer Service), and a list of `MenuItemEntity`.
- `MenuItemEntity` -- Stores menu item details, belongs to a restaurant.

**DTOs:**

| DTO                  | Purpose                                          |
|-----------------------|--------------------------------------------------|
| `RestaurantRequest`   | Create/update restaurant payload                 |
| `RestaurantResponse`  | Restaurant details with owner name (enriched via Feign) |
| `MenuItemRequest`     | Create/update menu item payload                  |
| `MenuItemResponse`    | Menu item details with restaurant name           |
| `CustomerResponse`    | Lightweight DTO for receiving customer data via Feign |
| `RegisterRequest`     | Used for Feign client compatibility              |

---

### Order Service

| Property   | Value              |
|------------|--------------------|
| Port       | 8083               |
| Database   | order_db           |
| Purpose    | Order placement, tracking, status management     |

**Entities:**

- `OrderEntity` -- Stores order details including snapshot fields (`customerName`, `restaurantName`, `restaurantAddress`) captured at order placement time, `customerId`, `restaurantId`, `deliveryId`.
- `OrderItemEntity` -- Stores order line items with `menuItemId`, `itemName` (snapshot), `quantity`, `unitPrice`, `subtotal`.

**DTOs:**

| DTO                    | Purpose                                          |
|-------------------------|--------------------------------------------------|
| `PlaceOrderRequest`     | Order placement payload with restaurant ID and items |
| `OrderItemRequest`      | Individual order item (menu item ID + quantity)  |
| `OrderResponse`         | Full order details with customer/restaurant snapshots, delivery info enriched via Feign |
| `OrderResponse.OrderItemDetail` | Nested DTO for order line item details  |
| `CustomerResponse`      | Lightweight DTO for receiving customer data via Feign |
| `RestaurantResponse`    | Lightweight DTO for receiving restaurant data via Feign |
| `MenuItemResponse`      | Lightweight DTO for receiving menu item data via Feign |
| `OrderRoutingKey`       | Enum defining RabbitMQ routing keys (`order.placed`, `order.updated`, `order.deleted`) |
| `DeliveryUpdateEvent`   | Event received from Delivery Service via RabbitMQ |

**Cross-Domain Data Strategy:**

The Order Service uses a **snapshot approach** for cross-domain data. At order placement time, the customer name, restaurant name, and restaurant address are stored directly on the `OrderEntity`, and the menu item name is stored on each `OrderItemEntity`. This eliminates the need for Feign calls on every read operation and ensures data consistency even if the source service changes the data later.

Delivery information (driver name, driver phone, delivery status) is enriched at read time via a Feign call to the Delivery Service. If the Delivery Service is unavailable, the order is still returned with the delivery fields set to `null`.

---

### Delivery Service

| Property   | Value              |
|------------|--------------------|
| Port       | 8082               |
| Database   | delivery_db        |
| Purpose    | Delivery assignment, tracking, status updates    |

**Entities:**

- `DeliveryEntity` -- Stores delivery details including `orderId`, driver info, pickup/delivery addresses, timestamps, and status (`PENDING`, `ASSIGNED`, `PICKED_UP`, `IN_TRANSIT`, `DELIVERED`, `FAILED`).

**DTOs:**

| DTO                    | Purpose                                          |
|-------------------------|--------------------------------------------------|
| `DeliveryResponse`      | Full delivery details with order/customer/restaurant info enriched via Feign |
| `OrderResponse`         | Lightweight DTO for receiving order data via Feign |
| `CustomerResponse`      | Lightweight DTO for receiving customer data via Feign |
| `OrderDTO`              | Simplified order representation for internal use |
| `RestaurantDTO`         | Simplified restaurant representation (name + address) |
| `DeliveryUpdateEvent`   | Event published to RabbitMQ on delivery status change |
| `DeliveryRoutingKey`    | Enum defining RabbitMQ routing key (`delivery.update`) |

---

## API Contracts

### Authentication (API Gateway -- :8080)

| Method | Endpoint            | Auth     | Request Body                  | Response            |
|--------|---------------------|----------|-------------------------------|---------------------|
| POST   | `/api/auth/register`| Public   | `RegisterRequest`             | `AuthResponse`      |
| POST   | `/api/auth/login`   | Public   | `AuthRequest`                 | `AuthResponse`      |

**RegisterRequest:**
```json
{
  "username": "string (3-50 chars, required)",
  "email": "string (valid email, required)",
  "password": "string (min 6 chars, required)",
  "firstName": "string",
  "lastName": "string",
  "phone": "string",
  "deliveryAddress": "string",
  "city": "string"
}
```

**AuthRequest:**
```json
{
  "username": "string (required)",
  "password": "string (required)"
}
```

**AuthResponse:**
```json
{
  "token": "string (JWT)",
  "customerId": 1,
  "username": "string",
  "role": "CUSTOMER"
}
```

---

### Customer Service (via Gateway -- /api/customers)

| Method | Endpoint                              | Auth      | Description                    |
|--------|---------------------------------------|-----------|--------------------------------|
| GET    | `/api/customers/me`                   | Required  | Get authenticated user profile |
| PUT    | `/api/customers/me`                   | Required  | Update authenticated user profile |
| GET    | `/api/customers/id/{id}`              | Required  | Get customer by ID             |
| GET    | `/api/customers/username/{username}`  | Internal  | Get customer entity by username (used by Feign) |
| POST   | `/api/customers/create`               | Internal  | Create customer (used by Gateway during registration) |
| PUT    | `/api/customers/make-restaurant-owner`| Required  | Promote user to RESTAURANT_OWNER role |

**CustomerResponse:**
```json
{
  "id": 1,
  "username": "string",
  "email": "string",
  "firstName": "string",
  "lastName": "string",
  "phone": "string",
  "deliveryAddress": "string",
  "city": "string",
  "role": "CUSTOMER",
  "createdAt": "2025-01-01T00:00:00",
  "orderCount": 0
}
```

---

### Restaurant Service (via Gateway -- /api/restaurants)

| Method | Endpoint                                | Auth      | Description                         |
|--------|-----------------------------------------|-----------|-------------------------------------|
| GET    | `/api/restaurants/search/city/{city}`   | Public    | Search restaurants by city           |
| GET    | `/api/restaurants/search/cuisine/{type}`| Public    | Search restaurants by cuisine type   |
| GET    | `/api/restaurants/search/all`           | Public    | List all active restaurants          |
| GET    | `/api/restaurants/{id}`                 | Public    | Get restaurant by ID                 |
| GET    | `/api/restaurants/{id}/menu`            | Public    | Get menu for a restaurant            |
| GET    | `/api/restaurants/menu/{id}`            | Public    | Get menu item by ID                  |
| POST   | `/api/restaurants`                      | Required  | Create restaurant (auto-promotes to RESTAURANT_OWNER) |
| POST   | `/api/restaurants/{restaurantId}/menu`  | Required  | Add menu item to restaurant          |
| PUT    | `/api/restaurants/menu/{itemId}`        | Required  | Update menu item                     |
| PATCH  | `/api/restaurants/menu/{itemId}/toggle` | Required  | Toggle menu item availability        |

**RestaurantRequest:**
```json
{
  "name": "string (required)",
  "description": "string",
  "cuisineType": "string (required)",
  "address": "string (required)",
  "city": "string (required)",
  "phone": "string",
  "estimatedDeliveryMinutes": 30
}
```

**RestaurantResponse:**
```json
{
  "id": 1,
  "name": "string",
  "description": "string",
  "cuisineType": "string",
  "address": "string",
  "city": "string",
  "phone": "string",
  "active": true,
  "rating": 4.5,
  "estimatedDeliveryMinutes": 30,
  "menuItemCount": 5,
  "ownerId": 1,
  "ownerName": "John Doe"
}
```

**MenuItemRequest:**
```json
{
  "name": "string (required)",
  "description": "string",
  "price": 12.99,
  "category": "string",
  "imageUrl": "string"
}
```

**MenuItemResponse:**
```json
{
  "id": 1,
  "name": "string",
  "description": "string",
  "price": 12.99,
  "category": "string",
  "available": true,
  "imageUrl": "string",
  "restaurantId": 1,
  "restaurantName": "string"
}
```

---

### Order Service (via Gateway -- /api/orders)

| Method | Endpoint                              | Auth      | Description                         |
|--------|---------------------------------------|-----------|-------------------------------------|
| POST   | `/api/orders`                         | Required  | Place a new order                    |
| GET    | `/api/orders/{id}`                    | Required  | Get order by ID                      |
| GET    | `/api/orders/my-orders`               | Required  | Get all orders for authenticated user|
| GET    | `/api/orders/restaurant/{restaurantId}`| Required | Get all orders for a restaurant      |
| PATCH  | `/api/orders/{id}/status?status=`     | Required  | Update order status                  |
| POST   | `/api/orders/{id}/cancel`             | Required  | Cancel an order (only PLACED/CONFIRMED) |

**PlaceOrderRequest:**
```json
{
  "restaurantId": 1,
  "items": [
    {
      "menuItemId": 1,
      "quantity": 2,
      "specialInstructions": "string"
    }
  ],
  "deliveryAddress": "string (optional, overrides customer default)",
  "specialInstructions": "string"
}
```

**OrderResponse:**
```json
{
  "id": 1,
  "status": "PLACED",
  "totalAmount": 25.98,
  "deliveryFee": 2.99,
  "deliveryAddress": "string",
  "specialInstructions": "string",
  "createdAt": "2025-01-01T12:00:00",
  "estimatedDeliveryTime": "2025-01-01T12:30:00",
  "items": [
    {
      "id": 1,
      "itemName": "Margherita Pizza",
      "quantity": 2,
      "unitPrice": 12.99,
      "subtotal": 25.98
    }
  ],
  "customerId": 1,
  "customerName": "John Doe",
  "restaurantId": 1,
  "restaurantName": "Pizza Place",
  "restaurantAddress": "123 Main St",
  "deliveryStatus": "ASSIGNED",
  "driverName": "Carlos Martinez",
  "driverPhone": "+1-555-0101"
}
```

**Order Statuses:** `PLACED`, `CONFIRMED`, `PREPARING`, `READY_FOR_PICKUP`, `OUT_FOR_DELIVERY`, `DELIVERED`, `CANCELLED`

---

### Delivery Service (via Gateway -- /api/deliveries)

| Method | Endpoint                               | Auth      | Description                         |
|--------|----------------------------------------|-----------|-------------------------------------|
| GET    | `/api/deliveries/{id}`                 | Required  | Get delivery by ID                   |
| GET    | `/api/deliveries/order/{orderId}`      | Required  | Get delivery by order ID             |
| GET    | `/api/deliveries/status/{status}`      | Required  | Get all deliveries by status         |
| PATCH  | `/api/deliveries/{id}/status?status=`  | Required  | Update delivery status               |

**DeliveryResponse:**
```json
{
  "id": 1,
  "status": "ASSIGNED",
  "driverName": "Carlos Martinez",
  "driverPhone": "+1-555-0101",
  "pickupAddress": "123 Main St",
  "deliveryAddress": "456 Oak Ave",
  "assignedAt": "2025-01-01T12:00:05",
  "pickedUpAt": null,
  "deliveredAt": null,
  "createdAt": "2025-01-01T12:00:05",
  "orderId": 1,
  "orderStatus": "CONFIRMED",
  "customerId": 1,
  "customerName": "John Doe",
  "restaurantName": "Pizza Place"
}
```

**Delivery Statuses:** `PENDING`, `ASSIGNED`, `PICKED_UP`, `IN_TRANSIT`, `DELIVERED`, `FAILED`

---

## Event-Driven Messaging

### RabbitMQ Configuration

| Component       | Value                |
|-----------------|----------------------|
| Exchange        | `app.exchange`       |
| Exchange Type   | Topic                |
| Order Queue     | `order.queue`        |
| Delivery Queue  | `delivery.queue`     |

### Message Flows

**Order Placed Flow:**

1. Customer places an order via Order Service.
2. Order Service persists the order and publishes the order entity to `app.exchange` with routing key `order.placed`.
3. Delivery Service consumes from `delivery.queue` (bound to `order.*`).
4. Delivery Service creates a delivery assignment, assigns a driver, and publishes a `DeliveryUpdateEvent` with status `CONFIRMED` to `app.exchange` with routing key `delivery.update`.
5. Order Service consumes from `order.queue` (bound to `delivery.*`) and updates the order status to `CONFIRMED`.

**Delivery Status Update Flow:**

1. Delivery status is updated (e.g., `PICKED_UP`, `DELIVERED`).
2. Delivery Service publishes a `DeliveryUpdateEvent` to `app.exchange` with routing key `delivery.update`.
3. Order Service consumes the event and maps the delivery status to the corresponding order status:
   - `CONFIRMED` / `ASSIGNED` -> Order `CONFIRMED`
   - `PICKED_UP` / `IN_TRANSIT` -> Order `OUT_FOR_DELIVERY`
   - `DELIVERED` -> Order `DELIVERED`
   - `FAILED` -> Order `CANCELLED`

**Order Cancelled Flow:**

1. Customer cancels an order.
2. Order Service publishes the order to `app.exchange` with routing key `order.deleted`.
3. Delivery Service can consume this event to cancel the associated delivery.

### Routing Key Bindings

| Queue            | Binding Pattern | Consumes Events From |
|------------------|-----------------|----------------------|
| `delivery.queue` | `order.*`       | Order Service        |
| `order.queue`    | `delivery.*`    | Delivery Service     |

---

## Security Model

### Authentication Flow

1. The client sends a `POST /api/auth/register` or `POST /api/auth/login` request to the API Gateway.
2. The API Gateway issues a JWT token containing the username and role.
3. For subsequent requests, the client includes the JWT in the `Authorization: Bearer <token>` header.
4. The API Gateway validates the token and injects `X-Auth-User` and `X-Auth-Role` headers into the forwarded request.
5. Each downstream service has a `SecurityContextFilter` that reads these headers and reconstructs the Spring Security `Authentication` object.

### Header Propagation

When a downstream service makes a Feign call to another service, the `FeignConfig` interceptor propagates the `X-Auth-User`, `X-Auth-Role`, and `Authorization` headers from the original request.

### Public Endpoints

The following endpoints do not require authentication:

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/restaurants/search/**`
- `GET /api/restaurants/{id}/menu`
- `GET /api/restaurants/{id}`

---

## Database Design

Each service owns its database. Cross-domain references are stored as ID values rather than foreign keys. Where appropriate, snapshot fields store denormalized data at write time to avoid runtime Feign calls on reads.

### customer_db

| Table      | Key Columns                                                         |
|------------|---------------------------------------------------------------------|
| customers  | id, username, email, password, firstName, lastName, phone, deliveryAddress, city, role, createdAt |

### restaurant_db

| Table       | Key Columns                                                        |
|-------------|---------------------------------------------------------------------|
| restaurants | id, name, description, cuisineType, address, city, phone, active, rating, estimatedDeliveryMinutes, ownerId, createdAt |
| menu_items  | id, name, description, price, category, available, imageUrl, restaurant_id |

### order_db

| Table       | Key Columns                                                        |
|-------------|---------------------------------------------------------------------|
| orders      | id, status, totalAmount, deliveryFee, deliveryAddress, specialInstructions, customerId, customerName, restaurantId, restaurantName, restaurantAddress, deliveryId, createdAt, estimatedDeliveryTime |
| order_items | id, quantity, unitPrice, subtotal, specialInstructions, menuItemId, itemName, order_id |

### delivery_db

| Table       | Key Columns                                                        |
|-------------|---------------------------------------------------------------------|
| deliveries  | id, status, driverName, driverPhone, pickupAddress, deliveryAddress, orderId, assignedAt, pickedUpAt, deliveredAt, createdAt |

---

## Getting Started

### Prerequisites

- Docker and Docker Compose installed
- (Optional) Java 21 and Maven for local development

### Running the Full System

1. Clone the repository:

```bash
git clone <repository-url>
cd food-delivery-microservice
```

2. Start all services with Docker Compose:

```bash
docker compose up --build
```

This starts the following containers:

| Container           | Port(s)              |
|---------------------|----------------------|
| POSTGRES            | 5433:5432            |
| RABBITMQ            | 5672, 15672          |
| DISCOVERY-SERVICE   | 8761                 |
| API-GATEWAY         | 8080                 |
| CUSTOMER-SERVICE    | 8081                 |
| DELIVERY-SERVICE    | 8082                 |
| ORDER-SERVICE       | 8083                 |
| RESTAURANT-SERVICE  | 8084                 |

3. Verify services are registered:
   - Eureka Dashboard: http://localhost:8761
   - RabbitMQ Management: http://localhost:15672 (guest/guest)

4. All API requests go through the gateway at `http://localhost:8080`.

### Stopping the System

```bash
docker compose down
```

To also remove the database volume:

```bash
docker compose down -v
```

---

## Migration Decision Log

### Bounded Context Identification

The monolith was decomposed into four bounded contexts based on domain-driven design principles:

| Bounded Context | Domain Responsibility                        | Rationale                                   |
|-----------------|----------------------------------------------|---------------------------------------------|
| Customer        | User registration, authentication, profiles  | Owns user identity; changes independently of business logic |
| Restaurant      | Restaurant CRUD, menu management             | Restaurant owners manage menus independently; different scaling profile from orders |
| Order           | Order placement, pricing, status tracking    | Core transaction processing; highest complexity and change frequency |
| Delivery        | Delivery assignment, driver management, tracking | Operationally distinct; benefits from asynchronous decoupling from orders |

### Cross-Domain Data Strategy

**Problem:** The monolith used JPA `@ManyToOne` and `@OneToMany` relationships across domains (e.g., `Order.customer`, `Order.restaurant`, `Delivery.order`), creating tight coupling.

**Solution:** Two complementary strategies were adopted:

1. **Snapshot fields (write-time denormalization):** For data that is frequently read but rarely changes (customer name, restaurant name, menu item name), the values are captured and stored on the Order entity at placement time. This eliminates the need for Feign calls on every read and ensures historical accuracy.

2. **Feign enrichment (read-time resolution):** For data that changes frequently and must reflect the current state (delivery status, driver assignment), the response is enriched via a Feign call at read time. A try-catch ensures graceful degradation if the target service is unavailable.

### Synchronous to Asynchronous Delivery

**Problem:** In the monolith, `OrderService.placeOrder()` called `DeliveryService.createDeliveryForOrder()` synchronously, blocking the order response until a driver was assigned.

**Solution:** The Order Service publishes an `OrderPlacedEvent` to RabbitMQ. The Delivery Service consumes this event asynchronously, creates the delivery, and publishes a `DeliveryStatusUpdatedEvent` back. The Order Service listens for this event to update the order status. The order response is returned immediately without waiting for delivery assignment.

### Database per Service

**Problem:** All entities shared a single PostgreSQL database with foreign key constraints across domains.

**Solution:** Four separate databases (`customer_db`, `restaurant_db`, `order_db`, `delivery_db`) are created via an initialization script. Each service connects exclusively to its own database. Cross-domain references are stored as plain `Long` ID fields with no foreign key enforcement.

---

## End-to-End Test Flow

The following sequence validates the complete order lifecycle through the API Gateway:

### 1. Register a Customer

```
POST http://localhost:8080/api/auth/register
Content-Type: application/json

{
  "username": "johndoe",
  "email": "john@example.com",
  "password": "password123",
  "firstName": "John",
  "lastName": "Doe",
  "phone": "+1-555-1234",
  "deliveryAddress": "456 Oak Ave",
  "city": "Accra"
}
```

Save the `token` from the response for subsequent requests.

### 2. Create a Restaurant

```
POST http://localhost:8080/api/restaurants
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "Pizza Place",
  "description": "Best pizza in town",
  "cuisineType": "Italian",
  "address": "123 Main St",
  "city": "Accra",
  "phone": "+1-555-5678",
  "estimatedDeliveryMinutes": 30
}
```

### 3. Add Menu Items

```
POST http://localhost:8080/api/restaurants/1/menu
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "Margherita Pizza",
  "description": "Classic tomato and mozzarella",
  "price": 12.99,
  "category": "Pizza"
}
```

### 4. Browse Restaurants and Menu

```
GET http://localhost:8080/api/restaurants/search/all
GET http://localhost:8080/api/restaurants/1/menu
```

### 5. Place an Order

```
POST http://localhost:8080/api/orders
Authorization: Bearer <token>
Content-Type: application/json

{
  "restaurantId": 1,
  "items": [
    {
      "menuItemId": 1,
      "quantity": 2
    }
  ],
  "specialInstructions": "Extra cheese please"
}
```

### 6. Verify Delivery Was Created Automatically

After a brief delay (asynchronous via RabbitMQ):

```
GET http://localhost:8080/api/deliveries/order/1
Authorization: Bearer <token>
```

The delivery should show status `ASSIGNED` with a driver name and phone number.

### 7. Track Order (Delivery Info Enriched)

```
GET http://localhost:8080/api/orders/1
Authorization: Bearer <token>
```

The order response should include `deliveryStatus`, `driverName`, and `driverPhone`.

### 8. Update Delivery Status

```
PATCH http://localhost:8080/api/deliveries/1/status?status=PICKED_UP
Authorization: Bearer <token>

PATCH http://localhost:8080/api/deliveries/1/status?status=DELIVERED
Authorization: Bearer <token>
```

### 9. Verify Order Status Updated

```
GET http://localhost:8080/api/orders/1
Authorization: Bearer <token>
```

The order status should now be `DELIVERED`.

### 10. Fault Tolerance Verification

Stop the Delivery Service container and verify that order placement and retrieval still work (delivery fields will be `null`):

```bash
docker compose stop delivery-service
```

```
POST http://localhost:8080/api/orders
Authorization: Bearer <token>
Content-Type: application/json

{
  "restaurantId": 1,
  "items": [{"menuItemId": 1, "quantity": 1}]
}
```

The order should be created successfully. The `deliveryStatus`, `driverName`, and `driverPhone` fields will be `null` in the response.

---

## Project Structure

```
food-delivery-microservice/
|-- compose.yml                 # Docker Compose orchestration
|-- sql-scripts/
|   |-- init.sql                # Creates per-service databases
|-- api-gateway/                # Spring Cloud Gateway + JWT auth
|-- discovery-service/          # Eureka service registry
|-- customer-service/           # Customer domain microservice
|-- restaurant-service/         # Restaurant domain microservice
|-- order-service/              # Order domain microservice
|-- delivery-service/           # Delivery domain microservice
|-- food-delivery-platform-monolith/  # Original monolith (reference)
```

