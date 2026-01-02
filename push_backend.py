import requests
import json
import os

url = "https://calltrack.mylistings.in/ai_file_manager.php"

files_to_push = [
    "api/calls.php",
    "api/export.php",
    "api/contacts.php"
]

base_dir = "/Users/ygs/Documents/Code/Android/CallCloud/Web/Php Backend on Server"

for rel_path in files_to_push:
    full_path = os.path.join(base_dir, rel_path)
    with open(full_path, 'r') as f:
        content = f.read()
    
    payload = {
        "action": "update_file",
        "path": rel_path,
        "content": content
    }
    
    print(f"Pushing {rel_path}...")
    response = requests.post(url, json=payload)
    print(f"Response: {response.text}")
