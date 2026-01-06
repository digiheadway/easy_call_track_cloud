#!/usr/bin/env python3
"""
Script to extract phone numbers and names from WhatsApp HTML export
Usage: python extract_contacts.py input.html output.csv
"""

import re
import sys
import csv
from collections import defaultdict

def extract_contacts(html_file, csv_file):
    """
    Extract phone numbers and associated names from WhatsApp HTML export
    """
    with open(html_file, 'r', encoding='utf-8') as f:
        content = f.read()

    # Extract phone numbers
    phone_pattern = r'\+91\s+\d+\s+\d+'
    phone_numbers = re.findall(phone_pattern, content)

    # Extract names (those in title attributes starting with ~)
    name_pattern = r'title="~([^"]*)"'
    names = re.findall(name_pattern, content)

    # Create a list of contacts
    contacts = []

    # Pair names with phone numbers (first names with first phone numbers, etc.)
    for i, phone in enumerate(phone_numbers[:len(names)]):
        clean_name = names[i].strip()
        contacts.append({
            'name': clean_name,
            'phone': phone
        })
        print(f"Found: {clean_name} - {phone}")

    # Add remaining phone numbers with "Unknown" names
    for phone in phone_numbers[len(names):]:
        contacts.append({
            'name': 'Unknown',
            'phone': phone
        })
        print(f"Found: Unknown - {phone}")

    # Write to CSV
    with open(csv_file, 'w', newline='', encoding='utf-8') as f:
        writer = csv.DictWriter(f, fieldnames=['name', 'phone'])
        writer.writeheader()
        writer.writerows(contacts)

    print(f"\nExtracted {len(contacts)} contacts to {csv_file}")
    return contacts

def main():
    if len(sys.argv) != 3:
        print("Usage: python extract_contacts.py input.html output.csv")
        sys.exit(1)

    html_file = sys.argv[1]
    csv_file = sys.argv[2]

    try:
        contacts = extract_contacts(html_file, csv_file)
        print(f"\nSuccessfully created {csv_file} with {len(contacts)} entries")
    except FileNotFoundError:
        print(f"Error: File '{html_file}' not found")
        sys.exit(1)
    except Exception as e:
        print(f"Error: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()
