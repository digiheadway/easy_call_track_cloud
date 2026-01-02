import requests

FILE_MANAGER_URL = "https://calltrack.mylistings.in/ai_file_manager.php"

def read_log(remote_path):
    print(f"Reading {remote_path}...")
    
    payload = {
        "action": "read",
        "path": remote_path
    }
    
    try:
        resp = requests.post(FILE_MANAGER_URL, json=payload, headers={"Content-Type": "application/json"})
        data = resp.json()
        if data.get("status"):
            print("--- LOG CONTENT START ---")
            print(data.get("content"))
            print("--- LOG CONTENT END ---")
        else:
            print("File not found or empty.")
    except Exception as e:
        print(f"Failed: {e}")

if __name__ == "__main__":
    read_log("api/debug_calls.log")
