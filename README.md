# Food Delivery Platform

A production-grade food delivery platform built with **Spring Boot 4.0.3** and **Spring Cloud 2025.1.0**, decomposed into independently deployable microservices.

## Quick Start

```bash
docker compose up --build
```

Services will be available at:
- API Gateway: http://localhost:8080
- Eureka Dashboard: http://localhost:8761
- RabbitMQ Management: http://localhost:15672 (guest/guest)

## Documentation

| Doc | Description |
|-----|-------------|
| [Architecture](docs/architecture.md) | System design, diagrams, service map, communication patterns |
| [API Contracts](docs/api-contracts.md) | All endpoints, request/response schemas, auth requirements |
| [Database Schema](docs/database-schema.md) | Tables, fields, cross-domain reference strategy |
| [Runbook](docs/runbook.md) | Running locally, Docker, environment config, monitoring |
| [Migration Log](docs/migration-log.md) | Changelog, architectural decisions, known issues |

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Runtime | Java 21 |
| Framework | Spring Boot 4.0.3 |
| Cloud | Spring Cloud 2025.1.0 |
| Service Discovery | Netflix Eureka |
| API Gateway | Spring Cloud Gateway (WebMVC) |
| Inter-Service Calls | OpenFeign + Resilience4j |
| Messaging | RabbitMQ (Spring AMQP) |
| Persistence | Spring Data JPA + PostgreSQL |
| Security | Spring Security + JWT (JJWT 0.13.0) |
| Observability | Actuator + Micrometer + Prometheus |
| Containerization | Docker + Docker Compose |

## Services at a Glance

| Service | Port | Role |
|---------|------|------|
| Discovery Service | 8761 | Eureka server — service registry |
| API Gateway | 8080 | Single entry point, JWT auth, routing |
| Customer Service | 8081 | Customer accounts, profiles, roles |
| Delivery Service | 8082 | Delivery assignment, driver management |
| Order Service | 8083 | Order placement and lifecycle |
| Restaurant Service | 8084 | Restaurant and menu management |
