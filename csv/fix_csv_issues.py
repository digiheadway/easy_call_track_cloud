#!/usr/bin/env python3
"""
Script to check and fix issues in the CSV file's 'description - p' column.
Identifies issues and updates the 'Status' column to 'done by antigravity' for checked entries.
"""

import csv
import re
import sys

# Input and output file paths
INPUT_FILE = '/Users/ygs/Documents/Code/csv/Copy of Import Propertties to Nnework - Copy of Latest.csv'
OUTPUT_FILE = '/Users/ygs/Documents/Code/csv/Copy of Import Propertties to Nnework - Copy of Latest_FIXED.csv'

def check_description_issues(description):
    """
    Check for issues and misformatting in the description - p column.
    Returns a tuple of (has_issues, issue_list)
    """
    issues = []
    
    if not description or description.strip() == '':
        issues.append("Empty description")
        return (True, issues)
    
    # Check for AI placeholder text
    ai_placeholders = [
        "I'm still learning and can't help with that",
        "I'm still learning",
        "Do you need help with anything else"
    ]
    for placeholder in ai_placeholders:
        if placeholder.lower() in description.lower():
            issues.append(f"Contains AI placeholder text: '{placeholder}'")
    
    # Check for excessive whitespace
    if '  ' in description:
        issues.append("Contains double spaces")
    
    # Check for leading/trailing whitespace
    if description != description.strip():
        issues.append("Has leading/trailing whitespace")
    
    # Check for embedded newlines (multi-line issues)
    if '\n' in description or '\r' in description:
        issues.append("Contains embedded newlines")
    
    # Check for very short descriptions (likely incomplete)
    if len(description.strip()) < 3:
        issues.append("Description too short (less than 3 characters)")
    
    # Check for missing commas between key-value pairs (basic check)
    # Description should generally not end with a comma
    if description.strip().endswith(','):
        issues.append("Description ends with trailing comma")
    
    return (len(issues) > 0, issues)

def normalize_status(status):
    """Normalize status values to consistent capitalization."""
    if not status:
        return status
    
    status_lower = status.strip().lower()
    
    status_mapping = {
        'done': 'Done',
        'sold': 'Sold',
        'pending': 'Pending',
        'panding': 'Pending',  # Fix typo
    }
    
    return status_mapping.get(status_lower, status)

def clean_description(description):
    """Clean up common issues in descriptions."""
    if not description:
        return description
    
    cleaned = description
    
    # Remove AI placeholder text
    ai_patterns = [
        r"I'm still learning and can't help with that\.\s*Do you need help with anything else\?",
        r"I'm still learning and can't help with that\.",
    ]
    for pattern in ai_patterns:
        cleaned = re.sub(pattern, '', cleaned, flags=re.IGNORECASE)
    
    # Replace multiple spaces with single space
    cleaned = re.sub(r'  +', ' ', cleaned)
    
    # Replace embedded newlines with space
    cleaned = re.sub(r'[\n\r]+', ' ', cleaned)
    
    # Strip whitespace
    cleaned = cleaned.strip()
    
    # Remove trailing commas
    cleaned = cleaned.rstrip(',').strip()
    
    return cleaned

def main():
    print(f"Reading CSV file: {INPUT_FILE}")
    print("=" * 80)
    
    issues_found = []
    fixed_rows = []
    
    try:
        with open(INPUT_FILE, 'r', encoding='utf-8') as f:
            # Read entire content first to handle multi-line cells
            content = f.read()
        
        # Use csv reader with proper quoting
        reader = csv.reader(content.splitlines())
        rows = list(reader)
        
        if len(rows) == 0:
            print("ERROR: No rows found in CSV file")
            return
        
        header = rows[0]
        print(f"Columns found: {len(header)}")
        
        # Find the relevant column indices
        try:
            status_idx = header.index('Status')
            description_p_idx = header.index('description - p')
            id_idx = header.index('id')
        except ValueError as e:
            print(f"ERROR: Required column not found: {e}")
            print(f"Available columns: {header}")
            return
        
        print(f"Status column index: {status_idx}")
        print(f"Description - p column index: {description_p_idx}")
        print(f"ID column index: {id_idx}")
        print("=" * 80)
        
        # Add header to fixed rows
        fixed_rows.append(header)
        
        # Process each data row
        for row_num, row in enumerate(rows[1:], start=2):
            if len(row) < max(status_idx, description_p_idx) + 1:
                print(f"WARNING: Row {row_num} has insufficient columns ({len(row)})")
                fixed_rows.append(row)
                continue
            
            row_id = row[id_idx] if len(row) > id_idx else 'N/A'
            description = row[description_p_idx]
            status = row[status_idx]
            
            # Check for issues
            has_issues, issue_list = check_description_issues(description)
            
            if has_issues:
                issues_found.append({
                    'row': row_num,
                    'id': row_id,
                    'status': status,
                    'description': description[:100] + '...' if len(description) > 100 else description,
                    'issues': issue_list
                })
            
            # Create a copy of the row for fixing
            fixed_row = list(row)
            
            # Clean up the description
            cleaned_description = clean_description(description)
            fixed_row[description_p_idx] = cleaned_description
            
            # Normalize status
            fixed_row[status_idx] = normalize_status(status)
            
            # If we fixed issues in this row, mark status as "done by antigravity"
            if has_issues or cleaned_description != description:
                if fixed_row[status_idx] not in ['Sold']:  # Don't change sold status
                    fixed_row[status_idx] = 'done by antigravity'
            
            fixed_rows.append(fixed_row)
        
        # Print summary of issues found
        print(f"\n{'=' * 80}")
        print(f"ISSUES FOUND: {len(issues_found)}")
        print(f"{'=' * 80}")
        
        for issue in issues_found[:50]:  # Show first 50 issues
            print(f"\nRow {issue['row']} (ID: {issue['id']}):")
            print(f"  Current Status: {issue['status']}")
            print(f"  Issues: {', '.join(issue['issues'])}")
            print(f"  Description preview: {issue['description']}")
        
        if len(issues_found) > 50:
            print(f"\n... and {len(issues_found) - 50} more issues")
        
        # Write fixed CSV
        print(f"\n{'=' * 80}")
        print(f"Writing fixed CSV to: {OUTPUT_FILE}")
        
        with open(OUTPUT_FILE, 'w', encoding='utf-8', newline='') as f:
            writer = csv.writer(f)
            writer.writerows(fixed_rows)
        
        print(f"Fixed {len(issues_found)} rows with issues")
        print(f"Total rows processed: {len(fixed_rows) - 1}")
        print("Done!")
        
    except Exception as e:
        print(f"ERROR: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)

if __name__ == '__main__':
    main()
