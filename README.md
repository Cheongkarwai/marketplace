# Marketplace - Transactional Outbox Pattern with Debezium & PostgreSQL

This project implements the **Transactional Outbox Pattern** using **PostgreSQL 15**, **Debezium Change Data Capture (CDC)**, **Apache Kafka (KRaft)**, and **Spring Boot (WebFlux & R2DBC)**.

---

## 🏛 Architecture Overview

```
                          Atomic ACID Transaction
                   ┌──────────────────────────────────────┐
                   │                                      │
   Client Request  │   ┌───────────────┐                  │
───────────────────┼──►│  Customer     │ (INSERT/UPDATE)  │
                   │   │  Table        │                  │
                   │   └───────────────┘                  │
                   │                                      │
                   │   ┌───────────────┐                  │
                   │   │  Outbox       │ (INSERT Event)   │
                   │   │  Table        │                  │
                   │   └───────┬───────┘                  │
                   └───────────┼──────────────────────────┘
                               │
                               ▼
                   ┌───────────────────────┐
                   │   PostgreSQL WAL      │
                   │  (Logical Decoding)   │
                   └───────────┬───────────┘
                               │
                               ▼
                   ┌───────────────────────┐
                   │  Debezium Connector   │
                   │  (Postgres pgoutput)  │
                   └───────────┬───────────┘
                               │
                               ▼
                   ┌───────────────────────┐
                   │   Outbox EventRouter  │
                   │ (Debezium SMT Filter) │
                   └───────────┬───────────┘
                               │
                               ▼
                   ┌───────────────────────┐
                   │      Apache Kafka     │
                   │   `customer.events`   │
                   └───────────┬───────────┘
                               │
            ┌──────────────────┴──────────────────┐
            ▼                                     ▼
  Order / Billing Service              Search / Analytics Service
```

### Why Transactional Outbox?
In distributed systems, writing to a database and publishing to a message broker in two distinct steps leads to the **Dual-Write Problem**:
- If database commit succeeds but message publishing fails, downstream services miss the event.
- If message publishing succeeds but database transaction rolls back, downstream services act on ghost data.

With the **Transactional Outbox Pattern**:
1. Business entity updates (`customer`) and the domain event (`outbox`) are saved inside the **same local database transaction** via Spring R2DBC `@Transactional`.
2. PostgreSQL writes changes to its Write-Ahead Log (WAL).
3. Debezium captures inserts into `outbox` via logical decoding (`pgoutput`) and publishes them to Kafka with **at-least-once delivery guarantees**.

---

## ⚙️ Components & Infrastructure

| Service | Technology | Port | Purpose |
|---|---|---|---|
| `postgres_server` | PostgreSQL 15 (`wal_level=logical`) | `5432` | Primary database with logical replication enabled |
| `kafka` | Apache Kafka 7.6.1 (KRaft Mode) | `9092` (internal), `29092` (host) | Event streaming broker without Zookeeper |
| `debezium` | Debezium Connect 2.6.2 | `8083` | Kafka Connect worker running Debezium Postgres connector & Outbox SMT |
| `debezium_init` | Auto-registration container | - | Automatically posts connector configuration upon startup |
| `kafka_ui` | Provectus Kafka UI | `8085` | Web dashboard for monitoring topics, messages, and connectors |
| `redis` | Redis 7 Alpine | `6379` | In-memory key-value store and reactive cache |

---

## 📦 Outbox Table Schema

Created via Liquibase changeset (`db/changelog/changesets/002-create-outbox-table.yaml`):

```sql
CREATE TABLE public.outbox (
    id VARCHAR(255) PRIMARY KEY,
    aggregatetype VARCHAR(255) NOT NULL,
    aggregateid VARCHAR(255) NOT NULL,
    type VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    timestamp TIMESTAMP WITHOUT TIME ZONE NOT NULL
);
```

### Outbox Event Router SMT Configuration
Debezium's `io.debezium.transforms.outbox.EventRouter` transforms outbox records as follows:
- **Topic Routing**: `${routedByValue}.events` dynamically routes rows with `aggregatetype = "customer"` to topic `customer.events`.
- **Partition Key**: `aggregateid` is set as the Kafka message key, preserving strict per-aggregate ordering.
- **Kafka Headers**: Column `type` is placed in record header `eventType: CUSTOMER_CREATED`.
- **Payload Unwrapping**: Column `payload` is unwrapped directly into the Kafka message value as clean JSON.
- **Tombstones Disabled**: `tombstones.on.delete = false` prevents empty records if outbox rows are purged.

---

## 🚀 Quick Start

### 1. Start Infrastructure
Launch all services using Docker Compose:
```bash
docker compose up -d
```
All containers will start, pass health checks, and the Debezium connector will auto-register.

### 2. Verify Connector Status
```bash
./debezium/register-connector.sh status
```
Expected output:
```json
{
    "name": "users-outbox-connector",
    "connector": {
        "state": "RUNNING",
        "worker_id": "172.19.0.4:8083"
    },
    "tasks": [
        {
            "id": 0,
            "state": "RUNNING",
            "worker_id": "172.19.0.4:8083"
        }
    ],
    "type": "source"
}
```

### 3. Open Kafka UI
Open your browser at:
**[http://localhost:8085](http://localhost:8085)**

Navigate to **Topics -> `customer.events` -> Messages** to see captured events in real time.

---

## 💻 Application Usage

### Reusable Outbox Module (`common-r2dbc`)
`common-r2dbc` provides:
- [`Outbox`](file:///Users/cheongkarwai/IdeaProjects/marketplace/common-r2dbc/src/main/java/com/cheong/common/r2dbc/outbox/Outbox.java): Entity mapping to `outbox` table.
- [`OutboxEvent`](file:///Users/cheongkarwai/IdeaProjects/marketplace/common-r2dbc/src/main/java/com/cheong/common/r2dbc/outbox/OutboxEvent.java): Interface for domain events.
- [`OutboxService`](file:///Users/cheongkarwai/IdeaProjects/marketplace/common-r2dbc/src/main/java/com/cheong/common/r2dbc/outbox/OutboxService.java): Reactive service persisting outbox entries using `R2dbcEntityTemplate`.
- Auto-configured via `CommonR2dbcAutoConfiguration`.

### Creating Customers with Outbox Events (`user-service`)
In [`CustomerService.java`](file:///Users/cheongkarwai/IdeaProjects/marketplace/user-service/src/main/java/com/cheong/userservice/service/CustomerService.java):

```java
@Transactional
public Mono<CustomerDTO> createCustomer(CustomerCreationDTO customerCreationDTO) {
    return Mono.justOrEmpty(customerCreationDTO)
            .map(customerMapper::mapToCustomer)
            .flatMap(customerRepository::save)
            .flatMap(savedCustomer -> {
                CustomerCreatedEvent event = new CustomerCreatedEvent(
                        UUID.randomUUID().toString(),
                        savedCustomer.getId(),
                        savedCustomer.getFirstName(),
                        savedCustomer.getLastName(),
                        savedCustomer.getBirthDate(),
                        savedCustomer.getContact() != null ? savedCustomer.getContact().getEmailAddress() : null,
                        savedCustomer.getContact() != null ? savedCustomer.getContact().getMobileNumber() : null,
                        Instant.now()
                );

                return outboxService.saveEvent("customer", savedCustomer.getId(), "CUSTOMER_CREATED", event, this::serializeToJson)
                        .thenReturn(savedCustomer);
            })
            .map(customerMapper::mapToCustomerDTO);
}
```

---

## 🧪 Testing

Run all unit and integration tests across the services:
```bash
cd user-service && ./gradlew test
```

### Inspect Messages from CLI
Listen to the `customer.events` Kafka topic directly:
```bash
docker exec marketplace_kafka kafka-console-consumer \
  --bootstrap-server localhost:29092 \
  --topic customer.events \
  --from-beginning \
  --property print.key=true \
  --property print.headers=true
```

Sample captured record:
```text
id:e600a543-a4dd-49bb-9270-d0b206352a90,eventType:CUSTOMER_CREATED   57ad0b40-2a87-4548-8a87-5a407ac173af   {"eventId":"8a64cb78-b016-4d84-b061-4f860c2fea46","customerId":"57ad0b40-2a87-4548-8a87-5a407ac173af","firstName":"Debezium","lastName":"Tester","birthDate":"1995-05-20","emailAddress":"cdc_test_f24d8aa6@example.com","mobileNumber":"6019249899","occurredAt":"2026-08-28T08:07:13.303096Z"}
```
