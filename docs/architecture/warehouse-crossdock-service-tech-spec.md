# Техническая реализация и Архитектура

**Документ:** `docs/architecture/warehouse-crossdock-service-tech-spec.md`
**Статус:** Approved for Development
**Сервис:** warehouse-crossdock-service (Поддомен: Warehouse Cross-Docking & B2B EDI)

## 1. Технологический Стек и Зависимости

| Компонент | Технология | Назначение / Обоснование |
| :--- | :--- | :--- |
| **Runtime** | Java 21 (Virtual Threads) | Высокая производительность при работе с транзакционной базой данных. |
| **Framework** | Spring Boot 3.3+ | Интеграция со Spring Data JPA, Spring Security, Spring Kafka. |
| **Primary Database** | PostgreSQL 16 | Реляционное транзакционное хранилище (ACID) с жесткой целостностью данных. |
| **Cache & Lock Engine** | Redis (Redisson) | Атомарные распределенные блокировки (RLock) для защиты от состязательного бронирования гейтов. |
| **Messaging** | Apache Kafka (Avro) | Событийно-ориентированный обмен данными с роутингом и сервисом алертов. |
| **Migration Tool** | Flyway | Версионирование SQL-миграций структуры таблиц складов и слотов. |

## 2. Архитектура Бронирования и Защита от Double Booking

Критический инженерный вызов сервиса — исключить конкурентное бронирование одного слота двумя разными машинами (Double Booking).

```text
 [Запрос на бронирование slot_id]
                │
                ▼
 [Redis Distributed Lock: lock:gate:{gateId}:{timeSlot}]
                │
                ├───► (Заблокировано другим процессом) ──► Return HTTP 409 Conflict / Retry
                │
                ▼ (Блокировка захвачена)
 [Postgres ACID Transaction]
   ├── 1. SELECT * FROM gate_booking_slots WHERE gate_id = X AND time_slot OVERLAPS Y FOR UPDATE
   ├── 2. Check Overlap Condition
   └── 3. INSERT INTO gate_booking_slots ...
                │
                ▼
 [Release Redis Lock & Commit Transaction]
```

*   **Redis Redisson Lock:** Захватывается атомарная распределенная блокировка с именем `lock:gate:{gate_id}:{time_slot}`.
*   **Pessimistic Locking in DB:** Дополнительная защита на уровне базы через `SELECT ... FOR UPDATE` внутри Spring `@Transactional`.
*   **Pessimistic Overlap Index:** На уровне PostgreSQL накладывается исключающий индекс `EXCLUDE USING GIST (gate_id WITH =, booking_interval WITH &&)`, который делает физически невозможным создание пересекающихся во времени интервалов в БД.

## 3. Межсервисное Взаимодействие и Интеграции

### 3.1 Схема интеграционных связей

```text
┌─────────────────────────┐          ┌────────────────────────────────┐
│     routing-service     │          │  edi-carrier-integration-serv  │
└────────────┬────────────┘          └───────────────▲────────────────┘
             │                                       │
  REST: ReserveDockSlot                   Kafka: CargoCrossDockedEvent
             │                                       │
             ▼                                       │
┌─────────────────────────┐                          │
│warehouse-crossdock-serv ├──────────────────────────┘
└────────────┬────────────┘
             │
  Kafka: DockSlotBookedEvent / ShipmentReadyForDepartureEvent
             │
             ▼
┌─────────────────────────┐
│ geofencing-alerting-serv│
└─────────────────────────┘
```

### 3.2 Описание контрактов взаимодействия

**Входящие интерфейсы (REST / gRPC):**
*   `POST /api/v1/hubs/{hubId}/slots/reserve` — резервирование слота под рейс.
*   `POST /api/v1/crossdock/scan` — сканирование SSCC-паллеты при приемке/погрузке.

**Исходящие события (Kafka Producers):**
*   `DockSlotBookedEvent`: Содержит данные о забронированном гейте и координатах склада для отправки водительской навигации.
*   `CargoCrossDockedEvent`: Публикуется при завершении перегрузки паллеты в целевой автотранспорт.
*   `ShipmentReadyForDepartureEvent`: Сигнализирует, что исходящий грузовик полностью укомплектован и готов к отправке.

## 4. Требования к Хранению Данных (Database Guidelines)

### 4.1 Пространственные и временные индексы в PostgreSQL
Для быстрой проверки пересечений временных интервалов бронирования применяется расширение `btree_gist`:

```sql
-- Включение расширения для комбинирования B-Tree и GiST
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- Таблица бронирования гейтов
CREATE TABLE gate_booking_slots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    gate_id UUID NOT NULL,
    route_id UUID NOT NULL,
    booking_interval TSTZRANGE NOT NULL, -- Временной диапазон (start_time, end_time)
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    
    -- Исключающий индекс: Запрещает пересечение интервалов TSTZRANGE для одного gate_id
    CONSTRAINT no_overlapping_slots EXCLUDE USING GIST (
        gate_id WITH =,
        booking_interval WITH &&
    )
);
```

## 5. Observability и Эксплуатация

*   **Metrics (Micrometer + Prometheus):**
    *   `dock_slot_reservation_duration_seconds` (histogram) — время выполнения резервирования слота.
    *   `dock_gate_utilization_percentage` (gauge) — процент загруженности гейтов хаба.
    *   `crossdock_discrepancy_total` (counter) — количество расхождений при сканировании паллет.
*   **Logging:** Структурированное логирование операций сканирования SSCC-кодов с фиксацией ID оператора склада и `trace_id`.
