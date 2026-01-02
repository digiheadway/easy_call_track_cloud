import json
import sys

def create_request(file_path, server_path, action="update_file"):
    with open(file_path, 'r') as f:
        content = f.read()
    
    # If the file doesn't exist on server, 'update_file' might create it or fail depending on backend.
    # The PHP file manager has 'create_file' and 'update_file'. 
    # Let's check if we should use 'create_file' for add_columns.
    # Actually, let's just use 'create_file' if the path is add_columns.php
    
    if "add_columns.php" in server_path:
        action = "create_file"
    
    data = {
        "action": action,
        "path": server_path,
        "content": content
    }
    
    with open('upload_payload.json', 'w') as f:
        json.dump(data, f)

if __name__ == "__main__":
    create_request(sys.argv[1], sys.argv[2])
