import json
import urllib.request
import urllib.error
import os

TOKEN = "567898765678"
URL = "https://api.miniclickcrm.com/ai_file_manager.php"

def upload(action, remote_path, local_path):
    print(f"Uploading {local_path} to {remote_path} via {action}...")
    try:
        with open(local_path, 'r', encoding='utf-8') as f:
            content = f.read()
    except Exception as e:
        print(f"Error reading {local_path}: {e}")
        return

    data = {
        "action": action,
        "path": remote_path,
        "content": content
    }
    
    req = urllib.request.Request(URL)
    req.add_header('Authorization', f'Bearer {TOKEN}')
    req.add_header('Content-Type', 'application/json')
    req.add_header('User-Agent', 'AgentUploadScript/1.0')
    
    try:
        json_data = json.dumps(data).encode('utf-8')
        response = urllib.request.urlopen(req, data=json_data)
        res_body = response.read().decode('utf-8')
        print(f"Response for {remote_path}: {res_body}")
    except urllib.error.HTTPError as e:
        print(f"HTTP Error uploading {remote_path}: {e.code} {e.reason}")
        try:
            print(e.read().decode('utf-8'))
        except:
            pass
    except Exception as e:
        print(f"Error sending request for {remote_path}: {e}")

base_dir = "/Users/ygs/Documents/Code/Android/miniclick-trackcalls/Web/Php Backend on Server/api"

# Upload test_hello.php
upload("create_file", "api/test_hello.php", os.path.join(base_dir, "test_hello.php"))
