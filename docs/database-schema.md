# Database Schema

Four isolated PostgreSQL databases — one per domain service. Schemas are auto-created by Hibernate (`ddl-auto: create`). Cross-domain references are stored as plain ID fields, never as foreign keys across databases.

Databases are initialized by `sql-scripts/init.sql`:
```sql
CREATE DATABASE customer_db;
CREATE DATABASE delivery_db;
CREATE DATABASE order_db;
CREATE DATABASE restaurant_db;
```

---

## customer_db — Customer Service

### `customers`

| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK, auto-increment |
| username | VARCHAR | UNIQUE, NOT NULL |
| email | VARCHAR | UNIQUE, NOT NULL |
| password | VARCHAR | NOT NULL (BCrypt hashed) |
| first_name | VARCHAR | |
| last_name | VARCHAR | |
| phone | VARCHAR | UNIQUE |
| delivery_address | VARCHAR | |
| city | VARCHAR | |
| role | VARCHAR | NOT NULL — `CUSTOMER`, `RESTAURANT_OWNER`, `ADMIN` |
| created_at | TIMESTAMP | NOT NULL, immutable |
| updated_at | TIMESTAMP | |
| order_ids | BIGINT[] | Cross-domain reference — order IDs from Order Service |

---

## restaurant_db — Restaurant Service

### `restaurants`

| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK, auto-increment |
| name | VARCHAR | NOT NULL |
| description | TEXT | |
| cuisine_type | VARCHAR | |
| address | VARCHAR | |
| city | VARCHAR | |
| phone | VARCHAR | |
| active | BOOLEAN | default `true` |
| rating | DOUBLE | NOT NULL, default `0.0` |
| estimated_delivery_minutes | INT | |
| created_at | TIMESTAMP | NOT NULL, immutable |
| owner_id | BIGINT | Cross-domain reference — Customer Service |
| order_ids | BIGINT[] | Cross-domain reference — Order Service |

### `menu_items`

| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK, auto-increment |
| name | VARCHAR | NOT NULL |
| description | VARCHAR | |
| price | DECIMAL | NOT NULL |
| category | VARCHAR | |
| available | BOOLEAN | default `true` |
| image_url | VARCHAR | |
| restaurant_id | BIGINT | FK → restaurants.id (same domain) |

---

## order_db — Order Service

### `orders`

| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK, auto-increment |
| status | VARCHAR | NOT NULL — see statuses below |
| total_amount | DECIMAL | NOT NULL |
| delivery_fee | DECIMAL | default `2.99` |
| delivery_address | VARCHAR | |
| special_instructions | VARCHAR | |
| created_at | TIMESTAMP | NOT NULL, immutable |
| updated_at | TIMESTAMP | |
| estimated_delivery_time | TIMESTAMP | |
| customer_id | BIGINT | Cross-domain reference — Customer Service |
| customer_name | VARCHAR | Snapshot at order time |
| restaurant_id | BIGINT | Cross-domain reference — Restaurant Service |
| restaurant_name | VARCHAR | Snapshot at order time |
| restaurant_address | VARCHAR | Snapshot at order time |
| delivery_id | BIGINT | Cross-domain reference — Delivery Service |

**Order statuses:** `PLACED` → `CONFIRMED` → `PREPARING` → `READY_FOR_PICKUP` → `OUT_FOR_DELIVERY` → `DELIVERED` / `CANCELLED`

### `order_items`

| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK, auto-increment |
| quantity | INT | NOT NULL |
| unit_price | DECIMAL | NOT NULL |
| subtotal | DECIMAL | NOT NULL |
| special_instructions | VARCHAR | |
| order_id | BIGINT | FK → orders.id (same domain) |
| menu_item_id | BIGINT | Cross-domain reference — Restaurant Service |
| item_name | VARCHAR | Snapshot at order time |

---

## delivery_db — Delivery Service

### `deliveries`

| Column | Type | Constraints |
|--------|------|-------------|
| id | BIGINT | PK, auto-increment |
| status | VARCHAR | NOT NULL — see statuses below |
| driver_name | VARCHAR | |
| driver_phone | VARCHAR | |
| pickup_address | VARCHAR | |
| delivery_address | VARCHAR | |
| assigned_at | TIMESTAMP | |
| picked_up_at | TIMESTAMP | |
| delivered_at | TIMESTAMP | |
| created_at | TIMESTAMP | NOT NULL, immutable |
| order_id | BIGINT | Cross-domain reference — Order Service |

**Delivery statuses:** `PENDING` → `ASSIGNED` → `PICKED_UP` → `IN_TRANSIT` → `DELIVERED` / `FAILED`

---

## Cross-Domain Reference Strategy

Services never share a database or use foreign keys across domain boundaries. Instead:

- IDs are stored as plain `BIGINT` columns
- Snapshot fields (e.g., `customer_name`, `restaurant_name`, `item_name`) are captured at write time to avoid runtime coupling
- Live enrichment (e.g., delivery info on order response) is done via Feign calls with circuit breaker fallbacks

This means data can become stale if a customer changes their name after placing an order — which is intentional and acceptable for an order history record.
