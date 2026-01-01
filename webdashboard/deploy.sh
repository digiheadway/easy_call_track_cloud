#!/bin/bash

# CallCloud Admin - Backend Deployment Script
# ============================================

echo "CallCloud Admin Backend Deployment"
echo "===================================="
echo ""

# Configuration
REMOTE_URL="https://calltrack.mylistings.in"
FILE_MANAGER_ENDPOINT="${REMOTE_URL}/ai_file_manager.php"
SECRET_TOKEN="CHANGE_THIS_SECRET_TOKEN"  # Must match your actual token
REMOTE_BASE_PATH=""  # Empty = root directory

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to upload file
upload_file() {
    local local_path=$1
    local remote_path=$2
    
    echo -n "Uploading ${local_path}... "
    
    content=$(cat "$local_path")
    
    response=$(curl -s -X POST "$FILE_MANAGER_ENDPOINT" \
        -H "Authorization: Bearer $SECRET_TOKEN" \
        -H "Content-Type: application/json" \
        -d "{
            \"action\": \"create_file\",
            \"path\": \"$remote_path\",
            \"content\": $(jq -Rs . <<< "$content")
        }")
    
    if echo "$response" | grep -q '"status":true'; then
        echo -e "${GREEN}✓${NC}"
        return 0
    else
        echo -e "${RED}✗${NC}"
        echo "   Error: $response"
        return 1
    fi
}

# Function to create folder
create_folder() {
    local folder_path=$1
    
    echo -n "Creating folder ${folder_path}... "
    
    response=$(curl -s -X POST "$FILE_MANAGER_ENDPOINT" \
        -H "Authorization: Bearer $SECRET_TOKEN" \
        -H "Content-Type: application/json" \
        -d "{
            \"action\": \"create_folder\",
            \"path\": \"$folder_path\"
        }")
    
    if echo "$response" | grep -q '"status":true'; then
        echo -e "${GREEN}✓${NC}"
        return 0
    else
        echo -e "${YELLOW}⚠${NC} (may already exist)"
        return 0
    fi
}

# Check if jq is installed
if ! command -v jq &> /dev/null; then
    echo -e "${RED}Error: jq is not installed${NC}"
    echo "Please install jq to use this script:"
    echo "  macOS: brew install jq"
    echo "  Ubuntu: sudo apt-get install jq"
    exit 1
fi

# Check if secret token is set
if [ "$SECRET_TOKEN" == "CHANGE_THIS_SECRET_TOKEN" ]; then
    echo -e "${RED}Error: Please set your SECRET_TOKEN in this script${NC}"
    exit 1
fi

echo "Starting deployment..."
echo ""

# Create folder structure
echo "Creating folder structure..."
create_folder "$REMOTE_BASE_PATH"
create_folder "$REMOTE_BASE_PATH/api"
echo ""

# Upload PHP files
echo "Uploading PHP files..."
upload_file "php/config.php" "$REMOTE_BASE_PATH/config.php"
upload_file "php/utils.php" "$REMOTE_BASE_PATH/utils.php"
upload_file "php/schema.sql" "$REMOTE_BASE_PATH/schema.sql"
upload_file "php/init_database.php" "$REMOTE_BASE_PATH/init_database.php"
echo ""

# Upload API files
echo "Uploading API endpoints..."
upload_file "php/api/auth.php" "$REMOTE_BASE_PATH/api/auth.php"
upload_file "php/api/employees.php" "$REMOTE_BASE_PATH/api/employees.php"
upload_file "php/api/calls.php" "$REMOTE_BASE_PATH/api/calls.php"
upload_file "php/api/recordings.php" "$REMOTE_BASE_PATH/api/recordings.php"
upload_file "php/api/reports.php" "$REMOTE_BASE_PATH/api/reports.php"
echo ""

echo "===================================="
echo -e "${GREEN}Deployment Complete!${NC}"
echo "===================================="
echo ""
echo "Next steps:"
echo "1. Initialize database:"
echo "   ${REMOTE_URL}${REMOTE_BASE_PATH}/init_database.php"
echo ""
echo "2. Update frontend API URL in src/api/client.ts:"
echo "   const API_BASE_URL = '${REMOTE_URL}${REMOTE_BASE_PATH}/api';"
echo ""
echo "3. Test authentication:"
echo "   ${REMOTE_URL}${REMOTE_BASE_PATH}/api/auth.php?action=verify"
echo ""
