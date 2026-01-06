Endpoint - https://api.miniclickcrm.com/ai_mysql_manager.php

# AI Server Management Guide

Use these endpoints to manage the server files and database directly.

## Authentication
All requests require a Bearer token:
`Authorization: Bearer 567898765678`

---

## 1. File Manager
**Endpoint:** `ai_file_manager.php`
**Methods:** `POST`

### Actions:
- **`help`**: Get usage details.
- **`list`**: `{ "action": "list", "path": "relative/path" }`
- **`read`**: `{ "action": "read", "path": "file.php" }`
- **`create_file`**: `{ "action": "create_file", "path": "new.php", "content": "..." }`
- **`update_file`**: `{ "action": "update_file", "path": "edit.php", "content": "..." }`
- **`delete_file`**: `{ "action": "delete_file", "path": "delete.php" }`
- **`create_folder`**: `{ "action": "create_folder", "path": "new_dir" }`
- **`delete_folder`**: `{ "action": "delete_folder", "path": "dir_to_delete" }`
- **`rename` / `move`**: `{ "action": "rename", "old_path": "a.txt", "new_path": "b.txt" }`

---

## 2. MySQL Manager
**Endpoint:** `ai_mysql_manager.php`
**Methods:** `POST`

### Actions:
- **`help`**: Get usage details.
- **`tables`**: `{ "action": "tables" }` - Lists all table names.
- **`describe`**: `{ "action": "describe", "table": "users" }` - Shows columns.
- **`schema`**: `{ "action": "schema" }` - Returns all `CREATE TABLE` statements.
- **`query`**: `{ "action": "query", "sql": "SELECT * FROM users LIMIT 10" }`


If still Need help read the files in the php backend on server folder in local and look for these files. 