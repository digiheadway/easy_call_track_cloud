#!/bin/bash

# Script to extract phone numbers and names from WhatsApp HTML export
# Usage: ./extract_contacts.sh input.html output.csv

INPUT_FILE="$1"
OUTPUT_FILE="$2"

if [ -z "$INPUT_FILE" ] || [ -z "$OUTPUT_FILE" ]; then
    echo "Usage: $0 input.html output.csv"
    exit 1
fi

echo "Extracting contacts from $INPUT_FILE..."

# Extract phone numbers
PHONE_NUMBERS=$(grep -o '+91 [0-9]* [0-9]*' "$INPUT_FILE" | sort | uniq)

# Extract names (those starting with ~)
NAMES=$(grep -o 'title="~[^"]*"' "$INPUT_FILE" | sed 's/title="//;s/"$//' | sort | uniq)

# Create CSV header
echo "name,phone" > "$OUTPUT_FILE"

# Counter for phone numbers
PHONE_COUNT=0

# Process each phone number and try to find associated names
while IFS= read -r phone; do
    PHONE_COUNT=$((PHONE_COUNT + 1))

    # Look for names that might be associated with this phone number
    # For now, we'll assign names from our extracted list in order
    if [ $PHONE_COUNT -le $(echo "$NAMES" | wc -l) ]; then
        NAME=$(echo "$NAMES" | sed -n "${PHONE_COUNT}p" | sed 's/^~//;s/^ //')
    else
        NAME="Unknown"
    fi

    echo "\"$NAME\",\"$phone\"" >> "$OUTPUT_FILE"
    echo "Added: $NAME - $phone"

done <<< "$PHONE_NUMBERS"

echo "Extraction complete! Created $OUTPUT_FILE with $(wc -l < "$OUTPUT_FILE") entries"
