import requests
import json
import os

API_URL = "https://api.miniclickcrm.com/ai_file_manager.php"
TOKEN = "567898765678"
HEADERS = {
    "Authorization": f"Bearer {TOKEN}",
    "Content-Type": "application/json"
}

BASE_DIR = "/Users/ygs/Documents/Code/Android/miniclick-trackcalls/Web/Php Backend on Server/"

files_to_sync = [
    {
        "local_path": "api/auth.php",
        "remote_path": "api/auth.php",
        "action": "update_file"
    },
    {
        "local_path": "utils.php",
        "remote_path": "utils.php",
        "action": "update_file"
    }
]

for item in files_to_sync:
    local_path = os.path.join(BASE_DIR, item['local_path'])
    remote_path = item['remote_path']
    action = item['action']
    
    print(f"Processing {local_path} -> {remote_path} ({action})")
    
    try:
        with open(local_path, 'r') as f:
            content = f.read()
            
        payload = {
            "action": action,
            "path": remote_path,
            "content": content
        }
        
        response = requests.post(API_URL, headers=HEADERS, json=payload)
        
        print(f"Status Code: {response.status_code}")
        print(f"Response: {response.text}")
        
    except Exception as e:
        print(f"Error processing {local_path}: {e}")

