import requests
import json

URL = "https://calltrack.mylistings.in/ai_mysql_manager.php"
TOKEN = "567898765678"

def query_db(sql):
    headers = {
        "Authorization": f"Bearer {TOKEN}",
        "Content-Type": "application/json"
    }
    payload = {"sql": sql}
    try:
        response = requests.post(URL, headers=headers, json=payload)
        return response.json()
    except Exception as e:
        return {"error": str(e)}

def check_exclusions():
    sql = "SELECT * FROM excluded_contacts ORDER BY id DESC LIMIT 5"
    result = query_db(sql)
    print("--- Current Excluded Contacts (Last 5) ---")
    if result.get('status') and 'rows' in result:
        for row in result['rows']:
            print(f"ID: {row.get('id')}, Phone: {row.get('phone')}, Active: {row.get('is_active')}")
    else:
        print(result)

if __name__ == "__main__":
    check_exclusions()
