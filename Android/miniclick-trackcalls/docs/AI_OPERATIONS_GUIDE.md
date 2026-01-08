# 🤖 AI Self-Operation Guide: PHP & MySQL (CallCloud)

This guide documents how the AI assistant can directly interact with the **CallCloud** backend and database. Use this in future sessions to quickly restore "self-operating" capabilities.

---

## 🔑 Connection Credentials

### 1. Direct MySQL Access (Local/Client)
The AI uses the installed `mysql-client` via the full path (required on this system).
- **Host**: `82.25.121.79`
- **Port**: `3306`
- **User**: `u542940820_easycalls`
- **Database**: `u542940820_easycalls`
- **Command Path**: `/opt/homebrew/opt/mysql-client/bin/mysql`

**Example Command (Run in Terminal):**
```bash
/opt/homebrew/opt/mysql-client/bin/mysql -h 82.25.121.79 -P 3306 -u u542940820_easycalls -p'v7D5;Xsz!~I' u542940820_easycalls -e "SHOW TABLES;"
```

### 2. Remote PHP Server (API)
The AI can talk to the live production server at `miniclickcrm.com`.
- **Base URL**: `https://api.miniclickcrm.com`
- **SQL Manager**: `https://api.miniclickcrm.com/ai_mysql_manager.php`
- **File Manager**: `https://api.miniclickcrm.com/ai_file_manager.php`
- **Auth Token**: `567898765678` (Bearer Token)

**Example API Query (via Curl):**
```bash
curl -X POST https://api.miniclickcrm.com/ai_mysql_manager.php \
-H "Authorization: Bearer 567898765678" \
-H "Content-Type: application/json" \
-d '{"sql": "SELECT COUNT(*) FROM calls"}'
```

---

## 🛠️ Supported Capabilities

1. **Database Maintenance**: Run migrations, clean duplicate contacts, or update call statuses.
2. **Backend Logic**: Audit and update PHP files in `Web/Php Backend on Server/`.
3. **Data Analysis**: Generate reports on employee performance and call volumes.
4. **Sync Debugging**: Compare data between the local development environment and the remote cloud.

---

## 📋 Session Snapshot (Jan 8, 2026)
- **Total Employees**: 2
- **Total Calls in Cloud**: ~4,271
- **Status**: MySQL Client installed via Homebrew. Full connectivity verified.

*Guide created by Antigravity AI.*
