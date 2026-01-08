# Database System Architecture Report

## 1. Executive Summary
The application employs a **Hybrid Offline-First Architecture** utilizing **Android Room (SQLite)** for local edge storage and **MySQL** for the centralized backend. The system relies on bidirectional synchronization to keep client and server states consistent.

This document outlines the current schema, identified bottlenecks, and proposed solutions for scalability and performance.

---

## 2. Current Architecture

### A. Android Local Database (Room)
*   **Database Name:** `callcloud_database`
*   **Version:** 9
*   **Key Entities:**
    *   `CallDataEntity`: Stores individual call logs with granular sync statuses (`metadataSyncStatus`, `recordingSyncStatus`).
    *   `PersonDataEntity`: Aggregates stats per contact (total calls, duration) and handles exclusion rules.
*   **Strengths:**
    *   **Robust Migration Path**: 9 versions of well-defined migrations ensuring user data preservation.
    *   **Granular Sync State**: Separating metadata sync from large file (recording) sync improves user experience on poor networks.
    *   **Indexed Queries**: Heavy read queries are optimized with indices on `phoneNumber`, `date`, and `exclusion` flags.

### B. Backend Database (MySQL)
*   **Engine:** InnoDB
*   **Key Tables:**
    *   `calls`: The ledger of all calls.
    *   `contacts`: Aggregated state of contacts per organization.
    *   `employees`: User/Agent management.
*   **Logic:**
    *   **Triggers**: Heavy reliance on `after_call_insert` trigger to update the `contacts` table in real-time.
    *   **Sync Optimization**: Recent migrations added functional indices (`idx_calls_org_updated`) to speed up delta syncs.

---

## 3. Identified Problems & Risks

### 🔴 Critical Issues
1.  **Synchronous Triggers for Aggregation**: 
    *   **Issue**: The `after_call_insert` MySQL trigger updates the `contacts` table immediately after every call insertion.
    *   **Risk**: High Concurrency Lock Contention. If multiple agents upload calls for the same contact (or distinct contacts causing page locks) simultaneously, the database will throttle. This is the #1 scalability bottleneck.
    
2.  **No Data Archival Strategy**:
    *   **Issue**: The `calls` table grows indefinitely.
    *   **Risk**: As the table hits millions of rows, query performance for "Last 7 days" dashboards will degrade, even with indices.

### 🟡 Moderate Issues
1.  **Phone Number Normalization**:
    *   **Issue**: Database relies on string matching for phone numbers (`varchar`).
    *   **Risk**: Inconsistent formats (`+1 123` vs `1123`) will cause split records in `PersonDataEntity` and `contacts`.
    
2.  **Sync Conflict Resolution**:
    *   **Issue**: While `serverUpdatedAt` exists, "Last Write Wins" logic without vector clocks can lead to data loss if two devices edit the same note offline.

---

## 4. Proposed Improvements & Solutions

### A. Short-Term Refinements (Next Sprint)
1.  **Optimize Indexes**: Ensure the backend `calls` table has a composite index on `(org_id, caller_phone, call_time)` to speed up history lookups for specific dashboard views.
2.  **Strict Normalization**: Implement a strict phone number normalizer (E.164 format) in the API middleware before data hits the DB.

### B. Long-Term Scalability (The "Project Future" Plan)
1.  **Remove Triggers / Move to Async Queue**:
    *   **Solution**: Disable the `after_call_insert` trigger.
    *   **Implementation**: When an API receives a call log, push a job to a queue (Redis/RabbitMQ). A separate worker process consumes the queue and updates `contacts` stats in batches. This decouples write latency from aggregation logic.
    
2.  **Table Partitioning**:
    *   **Solution**: Partition the `calls` table by Year/Month.
    *   **Benefit**: Old data sits in "cold" partitions, keeping indices for current month data small and fast.

3.  **Read/Write Splitting**:
    *   **Solution**: Use a Read Replica for generating heavy Reports/Analytics, keeping the Primary DB free for real-time Sync writes.

---

## 5. Detailed Schema Documentation

### Android `call_data` Table
| Column | Type | Purpose |
| :--- | :--- | :--- |
| `compositeId` | TEXT (PK) | Unique ID combining device + system ID |
| `syncStatus` | TEXT | **Legacy**. See metadata/recording specific status |
| `metadataSyncStatus` | TEXT | `PENDING`, `SYNCED`, `FAILED` |
| `recordingSyncStatus` | TEXT | `NOT_APPLICABLE`, `PENDING`, `UPLOADING`, `COMPLETED` |
| `reviewed` | INTEGER | Boolean flag synced with server |

### Android `person_data` Table
| Column | Type | Purpose |
| :--- | :--- | :--- |
| `phoneNumber` | TEXT (PK) | Normalized number |
| `excludeFromSync` | INTEGER | If true, calls are NEVER sent to server |
| `excludeFromList` | INTEGER | If true, hidden from local UI lists |

### Backend `calls` Table
| Column | Type | Purpose |
| :--- | :--- | :--- |
| `unique_id` | CHAR(50) | Idempotency key from client |
| `org_id` | VARCHAR | Tenancy isolation |
| `upload_status` | ENUM | Tracking recording file availability |

---

## 6. Migration Plan for Sync Logic
To ensure 100% data reliability:
1.  **Client**: Call ends -> Save to Room (`PENDING`).
2.  **Worker**: Background worker picks `PENDING` items.
3.  **Meta Sync**: POST metadata to `/api/sync/calls`.
    *   Server acknowledges.
    *   Client updates `metadataSyncStatus` -> `SYNCED`.
4.  **Media Sync**: If recording exists:
    *   Calculates Checksum.
    *   Uploads file.
    *   Client updates `recordingSyncStatus` -> `COMPLETED`.
    
**Conflict Handling**:
If server says "Record already exists but different timestamp", the Server timestamp should generally win for metadata (notes), but Client wins for immutable data like Duration/Time.
