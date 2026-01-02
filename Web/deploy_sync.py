import requests
import os
import sys

FILE_MANAGER_URL = "https://calltrack.mylistings.in/ai_file_manager.php"

def upload_file(local_path, remote_path):
    print(f"Uploading {local_path} to {remote_path}...")
    try:
        with open(local_path, 'r') as f:
            content = f.read()
    except FileNotFoundError:
        print(f"Error: File not found at {local_path}")
        return
    
    payload = {
        "action": "update_file",
        "path": remote_path,
        "content": content
    }
    
    try:
        resp = requests.post(FILE_MANAGER_URL, json=payload, headers={"Content-Type": "application/json"})
        print(f"Status: {resp.status_code}")
        print(f"Response: {resp.text}")
    except Exception as e:
        print(f"Failed: {e}")

if __name__ == "__main__":
    base_dir = "/Users/ygs/Documents/Code/Android/CallCloud/Web/Php Backend on Server"
    upload_file(f"{base_dir}/api/sync_app.php", "api/sync_app.php")
