# API Contracts

All requests go through the API Gateway at `http://localhost:8080`.

Authenticated endpoints require:
```
Authorization: Bearer <jwt_token>
```

---

## Authentication — `/api/auth`

### POST /api/auth/register

Register a new customer account.

**Auth:** None

**Request:**
```json
{
  "username": "johndoe",
  "email": "john@example.com",
  "password": "secret123",
  "firstName": "John",
  "lastName": "Doe",
  "phone": "0201234567",
  "deliveryAddress": "123 Main St",
  "city": "Accra"
}
```

**Response `201`:**
```json
{
  "token": "<jwt>",
  "username": "johndoe",
  "email": "john@example.com",
  "role": "CUSTOMER"
}
```

---

### POST /api/auth/login

**Auth:** None

**Request:**
```json
{
  "username": "johndoe",
  "password": "secret123"
}
```

**Response `200`:**
```json
{
  "token": "<jwt>",
  "username": "johndoe",
  "email": "john@example.com",
  "role": "CUSTOMER"
}
```

---

## Customers — `/api/customers`

### GET /api/customers/me

**Auth:** Required

**Response `200`:**
```json
{
  "id": 1,
  "username": "johndoe",
  "email": "john@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "phone": "0201234567",
  "deliveryAddress": "123 Main St",
  "city": "Accra",
  "role": "CUSTOMER",
  "orderCount": 3
}
```

---

### GET /api/customers/id/{id}

**Auth:** Required

**Response `200`:** Same as `/me`

---

### GET /api/customers/username/{username}

**Auth:** None (internal use by gateway)

**Response `200`:** Full `CustomerEntity` including hashed password

---

### PUT /api/customers/me

**Auth:** Required

**Request:** Same fields as register (partial update supported)

**Response `200`:** Updated `CustomerResponse`

---

### PUT /api/customers/make-restaurant-owner

**Auth:** Required

Promotes the authenticated customer to `RESTAURANT_OWNER` role.

**Response `200`:** Updated `CustomerResponse`

---

### POST /api/customers/create

**Auth:** None (called internally by API Gateway during registration)

**Request:** `RegisterRequest` with pre-hashed password

**Response `201`:** `CustomerEntity`

---

## Restaurants — `/api/restaurants`

### GET /api/restaurants/search/all

**Auth:** None

**Response `200`:**
```json
[
  {
    "id": 1,
    "name": "Burger Palace",
    "description": "Best burgers in town",
    "cuisineType": "American",
    "address": "456 Oak Ave",
    "city": "Accra",
    "phone": "0301234567",
    "active": true,
    "rating": 4.5,
    "estimatedDeliveryMinutes": 30,
    "ownerName": "Jane Smith",
    "menuItemCount": 12
  }
]
```

---

### GET /api/restaurants/search/city/{city}

**Auth:** None

**Response `200`:** Array of `RestaurantResponse`

---

### GET /api/restaurants/search/cuisine/{type}

**Auth:** None

**Response `200`:** Array of `RestaurantResponse`

---

### GET /api/restaurants/{id}

**Auth:** Required

**Response `200`:** Single `RestaurantResponse`

---

### GET /api/restaurants/{id}/menu

**Auth:** None

**Response `200`:**
```json
[
  {
    "id": 1,
    "name": "Classic Burger",
    "description": "Beef patty with lettuce and tomato",
    "price": 12.99,
    "category": "Burgers",
    "available": true,
    "imageUrl": "https://...",
    "restaurantId": 1,
    "restaurantName": "Burger Palace"
  }
]
```

---

### GET /api/restaurants/menu/{id}

**Auth:** Required

**Response `200`:** Single `MenuItemResponse`

---

### POST /api/restaurants

**Auth:** Required (RESTAURANT_OWNER role)

**Request:**
```json
{
  "name": "Burger Palace",
  "description": "Best burgers in town",
  "cuisineType": "American",
  "address": "456 Oak Ave",
  "city": "Accra",
  "phone": "0301234567",
  "estimatedDeliveryMinutes": 30
}
```

**Response `201`:** `RestaurantResponse`

---

### POST /api/restaurants/{restaurantId}/menu

**Auth:** Required (restaurant owner)

**Request:**
```json
{
  "name": "Classic Burger",
  "description": "Beef patty with lettuce and tomato",
  "price": 12.99,
  "category": "Burgers",
  "imageUrl": "https://..."
}
```

**Response `201`:** `MenuItemResponse`

---

### PUT /api/restaurants/menu/{itemId}

**Auth:** Required (restaurant owner)

**Request:** Same as add menu item

**Response `200`:** Updated `MenuItemResponse`

---

### PATCH /api/restaurants/menu/{itemId}/toggle

**Auth:** Required (restaurant owner)

Toggles the `available` flag on a menu item.

**Response `204`:** No content

---

## Orders — `/api/orders`

### POST /api/orders

**Auth:** Required

**Request:**
```json
{
  "restaurantId": 1,
  "deliveryAddress": "123 Main St",
  "specialInstructions": "No onions please",
  "items": [
    {
      "menuItemId": 1,
      "quantity": 2,
      "specialInstructions": "Extra sauce"
    }
  ]
}
```

**Response `201`:**
```json
{
  "id": 1,
  "status": "PLACED",
  "totalAmount": 28.97,
  "deliveryFee": 2.99,
  "deliveryAddress": "123 Main St",
  "specialInstructions": "No onions please",
  "createdAt": "2026-03-17T10:00:00",
  "customerName": "John Doe",
  "restaurantName": "Burger Palace",
  "restaurantAddress": "456 Oak Ave",
  "items": [
    {
      "menuItemId": 1,
      "itemName": "Classic Burger",
      "quantity": 2,
      "unitPrice": 12.99,
      "subtotal": 25.98,
      "specialInstructions": "Extra sauce"
    }
  ],
  "delivery": {
    "id": 99,
    "status": "PENDING",
    "driverName": null,
    "driverPhone": null
  }
}
```

---

### GET /api/orders/{id}

**Auth:** Required

**Response `200`:** `OrderResponse` (delivery enriched if available)

---

### GET /api/orders/my-orders

**Auth:** Required

**Response `200`:** Array of `OrderResponse`

---

### GET /api/orders/restaurant/{restaurantId}

**Auth:** Required

**Response `200`:** Array of `OrderResponse`

---

### PATCH /api/orders/{id}/status

**Auth:** Required

**Query param:** `status` — one of `CONFIRMED`, `PREPARING`, `READY_FOR_PICKUP`, `OUT_FOR_DELIVERY`, `DELIVERED`, `CANCELLED`

**Response `200`:** Updated `OrderResponse`

---

### POST /api/orders/{id}/cancel

**Auth:** Required

Cancels the order and publishes `order.deleted` event to RabbitMQ.

**Response `200`:** Updated `OrderResponse` with status `CANCELLED`

---

## Deliveries — `/api/deliveries`

### GET /api/deliveries/{id}

**Auth:** Required

**Response `200`:**
```json
{
  "id": 99,
  "status": "ASSIGNED",
  "driverName": "Alex Driver",
  "driverPhone": "0551234567",
  "pickupAddress": "456 Oak Ave",
  "deliveryAddress": "123 Main St",
  "assignedAt": "2026-03-17T10:00:05",
  "pickedUpAt": null,
  "deliveredAt": null,
  "orderId": 1,
  "orderStatus": "CONFIRMED",
  "customerName": "John Doe"
}
```

---

### GET /api/deliveries/order/{orderId}

**Auth:** Required

**Response `200`:** `DeliveryResponse`

---

### GET /api/deliveries/status/{status}

**Auth:** Required

**Path param:** `status` — one of `PENDING`, `ASSIGNED`, `PICKED_UP`, `IN_TRANSIT`, `DELIVERED`, `FAILED`

**Response `200`:** Array of `DeliveryResponse`

---

### PATCH /api/deliveries/{id}/status

**Auth:** Required

**Query param:** `status` — delivery status value

Publishes `DeliveryUpdateEvent` to RabbitMQ after updating.

**Response `200`:** Updated `DeliveryResponse`

---

## Error Responses

All services return a consistent error envelope:

```json
{
  "timestamp": "2026-03-17T10:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Customer not found with id: 99",
  "path": "/api/customers/id/99"
}
```

| HTTP Status | Condition |
|-------------|-----------|
| 400 | Validation failure (missing/invalid fields) |
| 401 | Missing or invalid JWT |
| 403 | Authenticated but insufficient role |
| 404 | Resource not found |
| 409 | Duplicate resource (username/email already exists) |
| 503 | Downstream service unavailable (circuit breaker open) |

---

## Actuator Endpoints

Each service exposes health and metrics under its own base path:

| Service | Base Path |
|---------|-----------|
| API Gateway | `/api/actuator` |
| Customer Service | `/api/customers/actuator` |
| Order Service | `/api/orders/actuator` |
| Restaurant Service | `/api/restaurants/actuator` |
| Delivery Service | `/api/deliveries/actuator` |
| Discovery Service | `/api/discovery/actuator` |

Available endpoints: `health`, `circuitbreakers`, `circuitbreakerevents`, `metrics`, `prometheus`
