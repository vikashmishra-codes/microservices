# Microservices Demo Architecture Overview

This repository is a Spring Boot-based microservices sample for a lightweight e-commerce workflow. It demonstrates how independent services can collaborate through a gateway, service discovery, synchronous HTTP calls, and asynchronous event-driven messaging.

## 1. Project purpose

The application models a simple shopping flow:

- Product catalog management
- Inventory validation before order placement
- Order creation and persistence
- Order confirmation events published to Kafka
- Notification handling for order confirmations
- Central API routing and service discovery

The project is meant to illustrate microservice design patterns in a practical, easy-to-follow setup rather than a production-grade platform.

## 2. Architecture at a glance

```text
                      ┌──────────────────────┐
                      │                     │
                      │   Client / Browser  │
                      │                     │
                      └──────────┬──────────┘
                                 │ HTTP
                                 ▼
                    ┌──────────────────────────┐
                    │   API Gateway            │
                    │   Spring Cloud Gateway   │
                    │   :8098                  │
                    └──────────┬───────────────┘
                               │
         ┌─────────────────────┼─────────────────────┐
         │                     │                     │
         ▼                     ▼                     ▼
┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│ Product Service  │  │ Order Service    │  │ Inventory Service│
│ MongoDB          │  │ MySQL            │  │ MySQL            │
│ :8081           │  │ :8801            │  │ :8082            │
└─────────┬────────┘  └─────────┬────────┘  └─────────┬────────┘
          │                       │                       │
          │                       │ HTTP / WebClient      │
          │                       └──────────────┬────────
          │                                      │
          │                                      ▼
          │                            ┌──────────────────┐
          │                            │ Kafka Topic      │
          │                            │ notificationTopic│
          │                            └─────────┬────────┘
          │                                      │
          │                                      ▼
          │                           ┌────────────────────┐
          │                           │ Notification Service│
          │                           │ :8083              │
          │                           └────────────────────┘
          │
          └──────────────────────────────────────────────────────────────┐
                                                                         │
                                                                         ▼
                                                          ┌────────────────────┐
                                                          │ Eureka Discovery   │
                                                          │ Service :8761      │
                                                          └────────────────────┘
```

## 3. Service breakdown

| Service | Purpose | Tech stack | Port |
| --- | --- | --- | --- |
| DiscoveryService | Service registry and discovery server | Spring Boot + Eureka + Actuator | 8761 |
| ApiGateway | Entry point for clients and route distribution | Spring Cloud Gateway + Eureka client | 8098 |
| productService | Manages catalog products | Spring Boot + MongoDB + Actuator | 8081 |
| inventoryService | Checks whether requested SKUs are in stock | Spring Boot + MySQL + JPA | 8082 |
| orderService | Creates orders and validates stock before saving | Spring Boot + MySQL + JPA + WebClient + Kafka | 8801 |
| NotificationService | Consumes order events and records confirmations | Spring Boot + Kafka + REST API | 8083 |

## 4. Tech stack analysis

### Java and Spring Boot

The project is built on Java and Spring Boot 3.5.0, with a Maven multi-module setup defined in the root pom.xml. The root parent project bundles all services as modules and manages shared Spring Cloud dependency versions.

This makes the codebase easy to expand with additional services, while keeping common configuration and plugin management centralized.

### Spring Cloud ecosystem

The architecture heavily uses Spring Cloud components:

- Netflix Eureka for service discovery
- Spring Cloud Gateway for API routing
- Spring Cloud Load Balancer style service-to-service discovery behavior
- Service registration through `@EnableEurekaServer` and `@EnableDiscoveryClient`

This is a classic microservice pattern where instead of hard-coding service addresses, services discover each other through the registry.

### Data persistence patterns

The repository demonstrates different persistence backends depending on the domain:

- productService uses MongoDB for product catalog storage
- orderService uses MySQL for transactional order records
- inventoryService uses MySQL for stock tracking

This is a good example of polyglot persistence, where each service owns its own database and avoids sharing a single global relational database across the whole system.

### Event-driven communication

The order flow is not only synchronous; it also emits an event to Kafka:

- OrderService validates stock through InventoryService
- If successful, it persists the order
- Then it publishes an `OrderPlacedEvent` to the `notificationTopic`
- NotificationService consumes the event and records a confirmation message

This illustrates the common microservices pattern of combining synchronous orchestration with asynchronous side effects.

### Observability and monitoring

The project includes tracing and metrics support:

- Zipkin for distributed tracing
- Prometheus for metrics collection
- Grafana for dashboards
- Spring Boot Actuator endpoints for health and monitoring

The Docker Compose file brings up:

- Kafka + Zookeeper
- Zipkin
- Prometheus
- Grafana

This makes it suitable for diagnosing latency, service health, and message flow between components.

## 5. Request flow in the application

A typical order flow looks like this:

1. Client sends an HTTP request to the API Gateway
2. Gateway routes the request to the relevant service based on path rules
3. OrderService receives the order request
4. OrderService calls InventoryService using WebClient
5. InventoryService checks stock for requested SKUs
6. If inventory is available, the order is saved in MySQL
7. OrderService publishes an `OrderPlacedEvent` to Kafka
8. NotificationService consumes the message and stores a confirmation record
9. The client or internal consumer can query notification status through the notification API

## 6. Service responsibilities

### Product Service

- Stores and retrieves product information
- Exposes product endpoints under `/api/product`
- Uses MongoDB as the backing database
- Registers with Eureka at startup

### Inventory Service

- Checks whether items are in stock
- Accepts a list of `skuCode` values via query parameter
- Returns a list of `InventoryResponse` objects
- Uses a simulated delay in `InventoryService` to model real-world latency

### Order Service

- Accepts incoming orders via `/api/order`
- Creates an order number with UUID
- Calls InventoryService synchronously to validate availability
- Saves the order to MySQL
- Publishes a Kafka event after successful placement
- Includes resilience tuning with Resilience4j properties, even though the sample is intentionally lightweight

### Notification Service

- Uses a Kafka listener on the `notificationTopic`
- Records order confirmations in memory
- Exposes endpoints to read notifications by order number or list all notifications

### Discovery Service

- Runs the Eureka server on port 8761
- Enforces service registration and lookup centralization
- Allows all services to discover each other without explicit hostnames in code

### API Gateway

- Routes requests to service instances through Spring Cloud Gateway
- Exposes a unified HTTP layer at port 8098
- Maps endpoints such as:
  - `/api/order/**`
  - `/api/product/**`
  - `/api/inventory/**`
  - `/eureka/**`

## 7. Key endpoints

### Product endpoints

- `POST /api/product` — create a product
- `GET /api/product` — get all products
- `GET /api/product/hi` — health-style greeting endpoint

### Order endpoints

- `POST /api/order` — place an order

### Inventory endpoints

- `GET /api/inventory?skuCode=SKU1&skuCode=SKU2` — check stock availability

### Notification endpoints

- `GET /api/notifications` — fetch all notifications
- `GET /api/notifications/{orderNumber}` — fetch one notification by order number

## 8. Local setup and run instructions

### Prerequisites

- JDK 24 (the build configuration is set to Java 24)
- Maven installed and available on PATH
- Docker Desktop or Docker Engine for Kafka, Zipkin, Prometheus, and Grafana
- Local MySQL and MongoDB instances running if you want to execute the services directly

### 1) Start the infrastructure containers

From the project root:

```bash
docker-compose up -d
```

This starts:

- Kafka at `localhost:9092`
- Zookeeper at `localhost:2181`
- Zipkin at `localhost:9411`
- Prometheus at `localhost:9090`
- Grafana at `localhost:3000`

### 2) Build the project

```bash
mvn clean install
```

### 3) Start the services in order

Start the registry first:

```bash
java -jar DiscoveryService/target/DiscoveryService-1.0-SNAPSHOT.jar
```

Then start the rest:

```bash
java -jar productService/target/productService-1.0-SNAPSHOT.jar
java -jar inventoryService/target/inventoryService-1.0-SNAPSHOT.jar
java -jar orderService/target/orderService-1.0-SNAPSHOT.jar
java -jar NotificationService/target/NotificationService-1.0-SNAPSHOT.jar
java -jar ApiGateway/target/ApiGateway-1.0-SNAPSHOT.jar
```

### 4) Test the flow

1. Create a product in ProductService
2. Check inventory via InventoryService
3. Submit an order through the gateway or OrderService
4. Verify the order confirmation event in NotificationService

## 9. Observability and dashboards

The project is designed to expose operational visibility:

- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000` (admin / password)
- Zipkin: `http://localhost:9411`
- Eureka dashboard: `http://localhost:8761`
- API Gateway: `http://localhost:8098`

This makes it useful for learning how distributed tracing, service health, and metrics fit into a microservice ecosystem.

## 10. Design strengths of this project

- Clear separation of concerns by business capability
- Independent service discovery and routing
- Polyglot persistence aligned to each service's domain
- Example of event-driven integration with Kafka
- Practical observability setup for distributed systems
- Good teaching example for microservices architecture fundamentals

## 11. Important caveats and production considerations

This is a solid learning project, but it is not yet production-ready in the strict enterprise sense. A few areas to improve for production include:

- Externalized environment configuration instead of hard-coded localhost values
- Secrets management for database credentials and Kafka configuration
- Real authentication/authorization at the gateway and service boundaries
- Persistent notification storage rather than in-memory storage
- Better resilience patterns, retry/backoff tuning, and dead-letter handling for Kafka consumers
- Container orchestration and deployment automation with Kubernetes or Docker Swarm
- More robust schema management and migrations for MySQL and MongoDB

The current configuration is intentionally simple and local-first, which makes it easier to learn and experiment with.

## 12. Summary

This project demonstrates the core ideas of a modern microservice architecture:

- independent services
- API gateway fronting the system
- service registry for dynamic discovery
- database per service
- event-driven communication for async workflows
- monitoring and tracing for operational insight

It is a great starting point for understanding how Java/Spring microservices work together in a real-world distributed application.
