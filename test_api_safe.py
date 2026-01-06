import urllib.request
import urllib.parse
import urllib.error
import json

URL = "https://api.miniclickcrm.com/api/sync_app.php"

def test_action(action, **kwargs):
    data = {
        "action": action,
        **kwargs
    }
    encoded_data = urllib.parse.urlencode(data).encode('utf-8')
    req = urllib.request.Request(URL, data=encoded_data, method='POST')
    
    print(f"--- Testing {action} ---")
    try:
        with urllib.request.urlopen(req) as f:
            print(f.read().decode('utf-8'))
    except urllib.error.HTTPError as e:
        print(f"HTTP Error: {e.code}")
        try:
            print(e.read().decode('utf-8'))
        except:
            print("Could not read error body")
    except Exception as e:
        print(f"Error: {e}")
    print("\n")

# Test fetch_config SAFE (no device update)
test_action(
    "fetch_config",
    org_id="UPTOWN",
    user_id="1"
)
