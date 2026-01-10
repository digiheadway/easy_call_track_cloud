# Device Admin - Simple API

## Database (2 Tables)

### Import Schema
```bash
mysql -u u542940820_protectloan -p'@?/9eG6t&' u542940820_protectloan < database/schema.sql
```

### Tables

**users** (managers)
| Field | Type | Description |
|-------|------|-------------|
| id | INT | Primary key |
| name | VARCHAR | Manager name |
| email | VARCHAR | Login email |
| password | VARCHAR | Hashed password |
| phone | VARCHAR | Phone number |
| api_key | VARCHAR | API access key |

**customers** (with device info)
| Field | Type | Description |
|-------|------|-------------|
| id | INT | Primary key |
| user_id | INT | Owner manager |
| name | VARCHAR | Customer name |
| phone | VARCHAR | Customer phone |
| loan_amount | DECIMAL | Total loan |
| pending_amount | DECIMAL | Pending dues |
| pairing_code | VARCHAR | Device pairing code |
| device_name | VARCHAR | Phone model |
| fcm_token | TEXT | Firebase token |
| is_freezed | TINYINT | 1=locked |
| is_protected | TINYINT | 1=can't uninstall |
| freeze_message | VARCHAR | Lock screen message |
| call_to | VARCHAR | Manager phone on lock screen |
| last_seen_at | TIMESTAMP | Last device check-in |

---

## API Endpoints

### 1. Device Check-in (Android app calls this)
```
GET /api/check.php?pairingcode=1120&fcm_token=xxx
```

Response:
```json
{
  "success": true,
  "data": {
    "amount": 25000,
    "message": "Payment Pending",
    "is_freezed": true,
    "call_to": "9068062563",
    "is_protected": true
  }
}
```

---

### 2. Push Commands (Manager uses this)
```
GET /api/pushchanges.php?pairingcode=1120&command=LOCK_DEVICE&api_key=sk_demo_xxx
```

Commands:
- `LOCK_DEVICE` - Lock device
- `UNLOCK_DEVICE` - Unlock device  
- `REMOVE_PROTECTION` - Allow uninstall
- `SYNC` - Sync current status

Push to all:
```
GET /api/pushchanges.php?all=1&command=SYNC&api_key=xxx
```

---

### 3. Customers API (CRUD)

**List all:**
```
GET /api/customers.php?api_key=xxx
```

**Get one:**
```
GET /api/customers.php?id=1&api_key=xxx
```

**Create:**
```
POST /api/customers.php
Header: X-Api-Key: xxx
Body: {"name": "John", "phone": "9876543210", "loan_amount": 50000}
```

**Update:**
```
PUT /api/customers.php?id=1
Header: X-Api-Key: xxx
Body: {"is_freezed": 1, "freeze_message": "Pay now!"}
```

**Delete:**
```
DELETE /api/customers.php?id=1
Header: X-Api-Key: xxx
```

---

## File Structure

```
/api/
├── config/
│   ├── database.php    # MySQL connection
│   └── firebase.php    # FCM setup
├── database/
│   └── schema.sql      # 2 tables only
├── check.php           # Device check-in
├── pushchanges.php     # Send commands
└── customers.php       # Customer CRUD
```

---

## Default Login

- **API Key:** `sk_demo_abc123xyz789`
- **Email:** manager@demo.com
- **Password:** password
