# Device Admin API Documentation

## Hosted Path
```
https://api.miniclickcrm.com/admin/
```

## Files to Upload
Upload all files from this `php/` folder to your server's `/admin/` directory:
- `check.php` - Device registration & status check
- `pushchanges.php` - Send push notifications to devices
- `device_tokens.json` - Auto-generated token storage (created automatically)

---

## API Endpoints

### 1. Check Device Status & Register Token
**Endpoint:** `GET /admin/check.php`

| Parameter | Required | Description |
|-----------|----------|-------------|
| `pairingcode` | Yes | Device pairing code (e.g., 1120) |
| `fcm_token` | No | Firebase Cloud Messaging token from device |

**Example:**
```
https://api.miniclickcrm.com/admin/check.php?pairingcode=1120&fcm_token=xxxxx
```

**Response:**
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

---

### 2. Push Changes to Devices
**Endpoint:** `GET /admin/pushchanges.php`

| Parameter | Required | Description |
|-----------|----------|-------------|
| `command` | Yes | Command to execute (see below) |
| `pairingcode` | No* | Target device (required if not using `all=1`) |
| `all` | No | Set to `1` to push to ALL registered devices |
| `duration` | No | Minutes for TEMPORAL_UNLOCK command |
| `message` | No | Custom message to send with command |

**Available Commands:**

| Command | Effect |
|---------|--------|
| `LOCK_DEVICE` | Shows full-screen lock on device |
| `UNLOCK_DEVICE` | Dismisses lock screen |
| `TEMPORAL_UNLOCK` | Unlocks for X minutes, then re-locks |
| `REMOVE_PROTECTION` | Removes admin rights, allows uninstall |

---

## Usage Examples

### Lock a Single Device
```
https://api.miniclickcrm.com/admin/pushchanges.php?command=LOCK_DEVICE&pairingcode=1120
```

### Lock ALL Registered Devices
```
https://api.miniclickcrm.com/admin/pushchanges.php?command=LOCK_DEVICE&all=1
```

### Unlock a Device
```
https://api.miniclickcrm.com/admin/pushchanges.php?command=UNLOCK_DEVICE&pairingcode=1120
```

### Temporal Unlock (2 hours)
```
https://api.miniclickcrm.com/admin/pushchanges.php?command=TEMPORAL_UNLOCK&pairingcode=1120&duration=120
```

### Remove Protection (Allow Uninstall)
```
https://api.miniclickcrm.com/admin/pushchanges.php?command=REMOVE_PROTECTION&pairingcode=1120
```

---

## Response Format

**Success:**
```json
{
  "success": true,
  "command": "LOCK_DEVICE",
  "results": {
    "1120": {
      "sent": true,
      "fcm_response": {
        "success": 1,
        "failure": 0
      }
    }
  }
}
```

**Error:**
```json
{
  "success": false,
  "error": "Device with pairing code not found",
  "registered_devices": ["1120", "1121", "1122"]
}
```

---

## Device Data Configuration

Edit the `$devices` array in `check.php` to add/modify devices:

```php
$devices = [
    "1120" => [
        "amount" => 2000,
        "message" => "Your custom message",
        "is_freezed" => true,      // true = device locked
        "call_to" => "9068062563", // Manager phone number
        "is_protected" => true      // true = can't uninstall
    ],
    // Add more devices...
];
```

---

## Firebase Configuration

The FCM Server Key is configured in `pushchanges.php`:
```php
$FCM_SERVER_KEY = "your-server-key-here";
```

To get your Server Key:
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project → Project Settings → Cloud Messaging
3. Copy the **Server Key** (not the Web Push certificate)

---

## Security Recommendations

1. **Add Authentication**: Protect `pushchanges.php` with API key or admin login
2. **Use HTTPS**: Always use HTTPS for API calls
3. **IP Whitelist**: Restrict `pushchanges.php` access to your admin IPs
4. **Rate Limiting**: Add rate limiting to prevent abuse

---

## File Permissions

```bash
chmod 644 check.php pushchanges.php
chmod 666 device_tokens.json  # Needs write access
```
