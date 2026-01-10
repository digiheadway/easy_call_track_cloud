# Database Structure and Queries Documentation

This document describes the database schema for both the Android application (Local) and the PHP Server (MySQL), their differences, and how data is queried and synchronized.

## 1. Local Database (Room/SQLite)

The Android app uses Room Persistence Library. The database name is `callcloud_database`.

### Table: `call_data`
Stores every individual call tracked by the system.

| Field | Type | Description |
|-------|------|-------------|
| `compositeId` (PK) | STRING | Unique ID for the call, usually `phoneNumber_timestamp`. |
| `systemId` | STRING | Original ID from the Android system `CallLog`. |
| `phoneNumber` | STRING | The contact's phone number (normalized). |
| `contactName` | STRING? | Name of the contact. |
| `callType` | INTEGER | Enum: INCOMING(1), OUTGOING(2), MISSED(3), REJECTED(5), etc. |
| `callDate` | LONG | Timestamp of the call. |
| `duration` | LONG | Duration in seconds. |
| `localRecordingPath` | STRING? | Path to the recording file on the device. |
| `metadataSyncStatus` | STRING | Local-only status: `PENDING`, `SYNCED`, `UPDATE_PENDING`, `FAILED`. |
| `recordingSyncStatus`| STRING | Local-only status: `PENDING`, `COMPLETED`, `FAILED`, `NOT_APPLICABLE`, `UPLOADING`, `NOT_FOUND`. |
| `reviewed` | BOOLEAN | Whether the user has reviewed this call in the app. |
| `callNote` | STRING? | User-added note for the call. |
| `serverUpdatedAt` | LONG? | Last timestamp modification from server (for bidirectional sync). |
| `processingStatus` | STRING? | Internal lock for workers (e.g., `UPLOADING_METADATA`). |

### Table: `person_data`
Stores contact-level information and aggregate statistics.

| Field | Type | Description |
|-------|------|-------------|
| `phoneNumber` (PK) | STRING | Normalized phone number. |
| `contactName` | STRING? | Custom name set by the user (synced). |
| `personNote` | STRING? | Note about the person. |
| `label` | STRING? | User-defined label (e.g., "Leads", "Follow-up"). |
| `totalCalls` | INTEGER | Count of all calls. |
| `totalDuration` | LONG | Sum of all call durations. |
| `excludeFromSync` | BOOLEAN | If true, calls from this number are NOT tracked/uploaded. |
| `excludeFromList` | BOOLEAN | If true, calls are tracked but hidden from the main UI list. |
| `needsSync` | BOOLEAN | Flag if person metadata needs to be pushed to server. |

---

## 2. Remote Database (MySQL) - LIVE SCHEMA

Located on the server. These are the **production tables** currently live on the database.

### Table: `call_log`
The central ledger for all individual call events.

| Field | Type | Description |
|-------|------|-------------|
| `id` (PK) | INT AUTO_INCREMENT | Primary key. |
| `unique_id` | VARCHAR(100) | Maps to App's `compositeId`. |
| `org_id` | VARCHAR(20) | Organization/Company ID. |
| `employee_id` | VARCHAR(20) | ID of the staff member's device. |
| `caller_phone` | VARCHAR(20) | Normalized digits only. |
| `caller_name` | VARCHAR(100) | Contact name. |
| `duration` | INT | Duration in seconds. |
| `type` | ENUM | `incoming`, `outgoing`, `missed`, `rejected`, `blocked`, `unknown`. |
| `call_time` | DATETIME | Timestamp when the call occurred. |
| `file_status` | ENUM | Recording status: `pending`, `completed`, `not_found`, `failed`, `not_applicable`, `not_allowed`, `disabled`. |
| `recording_url` | TEXT | Cloud path to the audio file. |
| `reviewed` | TINYINT(1) | 0=New, 1=Reviewed by Manager. |
| `note` | TEXT | Note attached to this specific call. |
| `created_at` | TIMESTAMP | Auto-set on insert. |
| `updated_at` | TIMESTAMP | Auto-updates on any change (for sync). |

### Table: `call_log_phones`
CRM contact profiles and aggregated statistics (Speed Layer).

| Field | Type | Description |
|-------|------|-------------|
| `id` (PK) | INT AUTO_INCREMENT | Primary key. |
| `org_id` | VARCHAR(20) | Organization ID. |
| `phone` | VARCHAR(20) | Normalized digits only. |
| `name` | VARCHAR(255) | Custom contact name. |
| `label` | VARCHAR(50) | Label (e.g., Lead, Customer). |
| `person_note` | TEXT | Main note for the contact. |
| `fully_reviewed` | TINYINT(1) | 1=Manager finished checking this contact. |
| `last_employee_id` | VARCHAR(20) | Last staff to handle this contact. |
| `first_call_time` | DATETIME | When this lead first entered the system. |
| `total_calls` | INT | Total call count. |
| `total_duration` | BIGINT | Total seconds across all calls. |
| `total_connected` | INT | Calls with duration > 0. |
| `total_not_answered` | INT | Missed + Rejected + 0-sec calls. |
| `total_incoming` | INT | Incoming call count. |
| `total_outgoing` | INT | Outgoing call count. |
| `total_missed` | INT | Missed call count. |
| `total_rejected` | INT | Rejected call count. |
| `last_call_time` | DATETIME | Most recent call time. |
| `last_call_type` | ENUM | Type of most recent call. |
| `last_call_duration` | INT | Duration of most recent call. |
| `exclude_from_sync` | TINYINT(1) | 1=Stop tracking this contact. |
| `exclude_from_list` | TINYINT(1) | 1=Hide from main UI list. |
| `created_at` | TIMESTAMP | Auto-set on insert. |
| `updated_at` | TIMESTAMP | Auto-updates on any change. |

---

## 3. Live MySQL Schema SQL (Copy-Paste Ready)

```sql
-- TABLE 1: call_log
CREATE TABLE IF NOT EXISTS call_log (
  id INT(11) NOT NULL AUTO_INCREMENT,
  unique_id VARCHAR(100) NOT NULL,
  org_id VARCHAR(20) NOT NULL,
  employee_id VARCHAR(20) NOT NULL,
  caller_phone VARCHAR(20) NOT NULL,
  caller_name VARCHAR(100) DEFAULT NULL,
  duration INT(11) DEFAULT 0,
  type ENUM('incoming', 'outgoing', 'missed', 'rejected', 'blocked', 'unknown') DEFAULT 'incoming',
  call_time DATETIME NOT NULL,
  file_status ENUM('pending', 'completed', 'not_found', 'failed') DEFAULT 'pending',
  recording_url TEXT DEFAULT NULL,
  reviewed TINYINT(1) DEFAULT 0,
  note TEXT DEFAULT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY idx_uid (unique_id),
  INDEX idx_sync (org_id, employee_id, updated_at),
  INDEX idx_lookup (caller_phone, call_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- TABLE 2: call_log_phones
CREATE TABLE IF NOT EXISTS call_log_phones (
  id INT(11) NOT NULL AUTO_INCREMENT,
  org_id VARCHAR(20) NOT NULL,
  phone VARCHAR(20) NOT NULL,
  name VARCHAR(255) DEFAULT NULL,
  label VARCHAR(50) DEFAULT NULL,
  person_note TEXT DEFAULT NULL,
  fully_reviewed TINYINT(1) DEFAULT 0,
  last_employee_id VARCHAR(20) DEFAULT NULL,
  first_call_time DATETIME DEFAULT NULL,
  total_calls INT(11) DEFAULT 0,
  total_duration BIGINT(20) DEFAULT 0,
  total_connected INT(11) DEFAULT 0,
  total_not_answered INT(11) DEFAULT 0,
  total_incoming INT(11) DEFAULT 0,
  total_outgoing INT(11) DEFAULT 0,
  total_missed INT(11) DEFAULT 0,
  total_rejected INT(11) DEFAULT 0,
  last_call_time DATETIME DEFAULT NULL,
  last_call_type ENUM('incoming', 'outgoing', 'missed', 'rejected', 'blocked') DEFAULT NULL,
  last_call_duration INT(11) DEFAULT 0,
  exclude_from_sync TINYINT(1) DEFAULT 0,
  exclude_from_list TINYINT(1) DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY idx_org_phone (org_id, phone),
  INDEX idx_review_queue (org_id, fully_reviewed, last_call_time),
  INDEX idx_list_sort (org_id, last_call_time),
  INDEX idx_sync_pull (org_id, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- TRIGGER: Auto-update call_log_phones when new call inserted
DELIMITER $$
CREATE TRIGGER after_call_log_insert 
AFTER INSERT ON call_log 
FOR EACH ROW 
BEGIN
  INSERT INTO call_log_phones (
    org_id, phone, name, last_employee_id, first_call_time,
    total_calls, total_duration, total_connected, total_not_answered,
    total_incoming, total_outgoing, total_missed, total_rejected,
    last_call_time, last_call_type, last_call_duration
  ) VALUES (
    NEW.org_id, NEW.caller_phone, NEW.caller_name, NEW.employee_id, NEW.call_time,
    1, NEW.duration,
    IF(NEW.duration > 0, 1, 0),
    IF(NEW.duration = 0 OR NEW.type IN ('missed', 'rejected'), 1, 0),
    IF(NEW.type = 'incoming', 1, 0),
    IF(NEW.type = 'outgoing', 1, 0),
    IF(NEW.type = 'missed', 1, 0),
    IF(NEW.type = 'rejected', 1, 0),
    NEW.call_time, NEW.type, NEW.duration
  )
  ON DUPLICATE KEY UPDATE
    name = IF(NEW.caller_name IS NOT NULL AND NEW.caller_name != '', NEW.caller_name, name),
    last_employee_id = NEW.employee_id,
    total_calls = total_calls + 1,
    total_duration = total_duration + NEW.duration,
    total_connected = total_connected + IF(NEW.duration > 0, 1, 0),
    total_not_answered = total_not_answered + IF(NEW.duration = 0 OR NEW.type IN ('missed', 'rejected'), 1, 0),
    total_incoming = total_incoming + IF(NEW.type = 'incoming', 1, 0),
    total_outgoing = total_outgoing + IF(NEW.type = 'outgoing', 1, 0),
    total_missed = total_missed + IF(NEW.type = 'missed', 1, 0),
    total_rejected = total_rejected + IF(NEW.type = 'rejected', 1, 0),
    last_call_time = NEW.call_time,
    last_call_type = NEW.type,
    last_call_duration = NEW.duration,
    fully_reviewed = 0;
END$$
DELIMITER ;
```

---

## 4. Field Mapping (App ↔ Server)

| Local (Room) | Remote (MySQL) | Notes |
|--------------|----------------|-------|
| `compositeId` | `unique_id` | Master sync key. |
| `phoneNumber` | `caller_phone` / `phone` | Digits only normalization. |
| `callType` (Int) | `type` (Enum) | App: 1=incoming, 2=outgoing, 3=missed. |
| `callDate` (Long) | `call_time` (DateTime) | App converts to ISO-8601. |
| `metadataSyncStatus` | Row existence | If row exists, metadata is synced. |
| `recordingSyncStatus` | `file_status` | Maps directly. |
| `reviewed` | `reviewed` | Bidirectional sync supported. |
| `callNote` | `note` | Bidirectional sync supported. |
| `personNote` | `person_note` | Bidirectional sync supported. |
| `excludeFromSync` | `exclude_from_sync` | Maps directly. |
| `excludeFromList` | `exclude_from_list` | Maps directly. |

---

## 4. How Local Queries Work

### Reactive UI with `Flow`
The app uses Kotlin Coroutines `Flow` to provide real-time updates.
- **Observation:** `CallDataDao.getAllCallsFlow()` returns a stream of lists. Whenever any row in `call_data` changes, Room automatically emits a new list.
- **UI Logic:** The `CallLogViewModel` collects this flow and updates the screen immediately without manual refreshes.

### Exclusion Logic
Local queries frequently use `LEFT JOIN person_data` to filter out calls from users who have been "Ignored" or "Excluded".
```sql
SELECT c.* FROM call_data c 
LEFT JOIN person_data p ON c.phoneNumber = p.phoneNumber 
WHERE (p.excludeFromList IS NULL OR p.excludeFromList = 0)
ORDER BY c.callDate DESC
```

### Sync Queue Selection
Workers search for pending items using specific flags to avoid overlapping work:
1. **Metadata Sync:** Looks for `metadataSyncStatus IN ('PENDING', 'UPDATE_PENDING')`.
2. **Recording Sync:** Looks for `recordingSyncStatus = 'PENDING'` but **ONLY IF** `metadataSyncStatus = 'SYNCED'`. 

### Bidirectional Conflict Resolution
- Both DBs track `serverUpdatedAt` and `updatedAt`.
- If a change is made on the admin panel, the server increments its `updated_at`.
- During sync, the app pulls these changes. If `remote.serverUpdatedAt > local.serverUpdatedAt`, the local DB is updated.

---

## 5. Recording Matching & Lookup Logic

One of the most complex "queries" in the app isn't a SQL query, but a multi-tiered file search logic implemented in `RecordingRepository`.

### Search Tiers (Priority Order)
1. **MediaStore Query:** Queries the Android MediaStore for files added within ±5 minutes of the call.
2. **Learned Folder:** Checks folders where previous recordings were successfully found.
3. **Device Defaults:** Searches known paths for Samsung, Xiaomi, OnePlus, etc.
4. **3rd Party Apps:** Searches paths for ACR, Cube Call Recorder, BoldBeast, etc.
5. **Deep Scan:** Recursive search of common parent directories.

### Matching Scoring System
When multiple files are found, they are scored (0-100+). A file needs a score of **100** to be considered a match.

| Factor | Score | Description |
|--------|-------|-------------|
| **Exact Identity** | +40 | Filename contains exact phone number. |
| **Partial Identity**| +20 | Filename contains last 9 digits or contact name. |
| **Time Match (Top)**| +100 | Difference < 5 seconds. |
| **Time Match (Good)**| +60 | Difference < 1 minute. |
| **Folder Bonus** | +30 | File is inside a known "Call" or "Recorder" folder. |
| **Duration Match** | +40 | File duration matches system call duration within 1s. |

### Normalization Logic
To ensure queries work across all levels (DB, API, Filenames), the app uses a strict normalization:
- **Phone Numbers:** `phone.replace(Regex("[^0-9]"), "")`. This removes `+`, `(`, `)`, `-` and spaces.
- **Composite IDs:** The primary key for syncing is always `normalizedPhone_timestamp`.

---

## 6. Future Optimizations & Scale Suggestions

To ensure the system remains fast and reliable as the user base and data volume grow, the following optimizations are recommended:

### 🚀 Speed & Performance
1. **Query Batching:** Currently, some sync operations process calls one-by-one. Implementing **Batch Inserts/Updates** in Room and API can reduce IO overhead by up to 80%.
2. **Local Work Caching:** The `RecordingRepository` should cache MediaStore scan results during a single sync pass to avoid repeated disk/content-resolver queries for the same time window.
3. **Database Maintenance:** Implement a periodic `VACUUM` or `PRAGMA incremental_vacuum` on the local SQLite DB to reclaim space and keep lookup speeds consistent.

### 🔄 Sync Improvements
1. **Delta Sync (Server -> App):** Instead of polling full contact lists, the app should request objects changed since its last known `max(serverUpdatedAt)`.
2. **Prioritized Upload Queue:** Implement a "Priority" field for sync. "New Calls" and "Manual Notes" should be synced before old historical records or background recordings.
3. **Conflict Resolution:** Use **Vector Clocks** or a more robust "Source of Truth" hierarchy for notes to prevent data loss in offline editing scenarios.

### 📈 Scalability
1. **Asynchronous Aggregation (Server):** Move the MySQL triggers (which update the `contacts` table) to an **Asynchronous Queue** (e.g., Redis/RabbitMQ). This prevents table locking during high-concurrency uploads.
2. **Table Partitioning:** Partition the server `calls` table by `org_id` and `month` to keep indices small and queries fast for dashboards.
3. **Read Replicas:** Use a read-only MySQL replica to handle heavy dashboard/report queries, keeping the primary instance free for high-speed write syncs from devices.

### 🛠️ Ease of Use & Reliability
1. **Self-Healing Normalization:** Add a background worker that identifies and merges duplicate `person_data` entries caused by inconsistent phone number formatting from different sources.
2. **Sync Pulse Visualization:** Provide a detailed "Sync Health" log in the app settings for users to see exactly why a specific recording failed (e.g., "File too large", "Invalid Format", "Checksum Mismatch").
3. **Smart Retry:** Implement exponential backoff for failed uploads, with specific logic to skip "Unrecoverable" errors (like `403 Forbidden` due to expired plan) to save battery.
