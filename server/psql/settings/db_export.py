#!/usr/bin/env python3
"""
Database Export Script for Team Sharing

This script exports the current database state to a SQL file for sharing with teammates.
Run this after making changes to the database that need to be shared.

Usage:
    python settings/db_export.py [--compress]
"""

import subprocess
import sys
import os
from datetime import datetime
import argparse
from pathlib import Path
import re

# Load environment variables from .env file if it exists
env_file = Path(__file__).parent / '.env'
if env_file.exists():
    with open(env_file) as f:
        for line in f:
            if line.strip() and not line.startswith('#'):
                key, value = line.strip().split('=', 1)
                os.environ.setdefault(key, value)

def run_command(cmd, description, env=None):
    """Run a shell command and handle errors."""
    print(f"Running: {description}")
    print(f"Command: {' '.join(cmd)}")
    
    try:
        subprocess.run(cmd, check=True, capture_output=True, text=True, env=env)
        print(f"✅ {description} completed successfully")
        return True
    except subprocess.CalledProcessError as e:
        print(f"❌ {description} failed:")
        print(f"Error: {e.stderr}")
        return False

def reorder_db_menus_columns(sql_content):
    """Reorder db_menus INSERT statements to place restaurant_id at the end."""
    
    # Pattern to match db_menus INSERT statements
    # This handles both single and multi-line INSERT statements
    pattern = r"INSERT INTO public\.db_menus\s*\([^)]+\)\s*VALUES[^;]+;"
    
    def reorder_insert(match):
        insert_statement = match.group(0)
        
        # Extract column list and values
        columns_match = re.search(r"INSERT INTO public\.db_menus\s*\(([^)]+)\)", insert_statement)
        if not columns_match:
            return insert_statement
        
        columns_str = columns_match.group(1)
        columns = [col.strip() for col in columns_str.split(',')]
        
        # Find restaurant_id column index
        restaurant_id_idx = None
        for i, col in enumerate(columns):
            if 'restaurant_id' in col:
                restaurant_id_idx = i
                break
        
        if restaurant_id_idx is None or restaurant_id_idx == len(columns) - 1:
            # restaurant_id not found or already at the end
            return insert_statement
        
        # Reorder columns: move restaurant_id to the end
        restaurant_id_col = columns.pop(restaurant_id_idx)
        columns.append(restaurant_id_col)
        
        # Extract VALUES part
        values_match = re.search(r"VALUES\s*(.+);", insert_statement, re.DOTALL)
        if not values_match:
            return insert_statement
        
        values_part = values_match.group(1).strip()
        
        # Handle multiple value rows
        if values_part.startswith('(') and values_part.endswith(')'):
            # Parse individual value rows
            value_rows = []
            current_row = ""
            paren_count = 0
            in_quotes = False
            quote_char = None
            
            for char in values_part:
                if char in ('"', "'") and not in_quotes:
                    in_quotes = True
                    quote_char = char
                elif char == quote_char and in_quotes:
                    in_quotes = False
                    quote_char = None
                elif char == '(' and not in_quotes:
                    paren_count += 1
                elif char == ')' and not in_quotes:
                    paren_count -= 1
                
                current_row += char
                
                if paren_count == 0 and char == ')' and not in_quotes:
                    # End of a row
                    value_rows.append(current_row.strip())
                    current_row = ""
                    # Skip comma and whitespace
                    continue
                elif paren_count == 0 and char == ',' and not in_quotes:
                    # Between rows
                    current_row = ""
                    continue
            
            # Reorder values in each row
            reordered_rows = []
            for row in value_rows:
                if row.startswith('(') and row.endswith(')'):
                    row_values_str = row[1:-1]  # Remove parentheses
                    row_values = parse_value_list(row_values_str)
                    
                    if len(row_values) == len(columns) + 1:  # +1 because we moved restaurant_id
                        # Move restaurant_id value to the end
                        restaurant_id_value = row_values.pop(restaurant_id_idx)
                        row_values.append(restaurant_id_value)
                        
                        reordered_row = '(' + ', '.join(row_values) + ')'
                        reordered_rows.append(reordered_row)
                    else:
                        reordered_rows.append(row)
            
            # Reconstruct the INSERT statement
            new_columns_str = ', '.join(columns)
            new_values_str = ',\n'.join(reordered_rows)
            
            return f"INSERT INTO public.db_menus ({new_columns_str}) VALUES\n{new_values_str};"
        
        return insert_statement
    
    # Apply the reordering to all db_menus INSERT statements
    return re.sub(pattern, reorder_insert, sql_content, flags=re.MULTILINE | re.DOTALL)

def parse_value_list(values_str):
    """Parse a comma-separated list of SQL values, handling quotes and escapes."""
    values = []
    current_value = ""
    in_quotes = False
    quote_char = None
    i = 0
    
    while i < len(values_str):
        char = values_str[i]
        
        if char in ('"', "'") and not in_quotes:
            in_quotes = True
            quote_char = char
            current_value += char
        elif char == quote_char and in_quotes:
            # Check if it's escaped
            if i + 1 < len(values_str) and values_str[i + 1] == quote_char:
                # Escaped quote
                current_value += char + char
                i += 1
            else:
                # End quote
                in_quotes = False
                quote_char = None
                current_value += char
        elif char == ',' and not in_quotes:
            # End of value
            values.append(current_value.strip())
            current_value = ""
        else:
            current_value += char
        
        i += 1
    
    # Add the last value
    if current_value.strip():
        values.append(current_value.strip())
    
    return values

def export_database(compress=True, restaurant_id_last=False):
    """Export the database to SQL file with configurable column ordering."""
    
    # Ensure we're in the psql directory
    script_dir = os.path.dirname(os.path.abspath(__file__))
    psql_dir = os.path.dirname(script_dir)
    os.chdir(psql_dir)
    
    # Create db directory if it doesn't exist
    os.makedirs('db', exist_ok=True)
    
    # Database connection parameters
    use_docker = os.getenv('USE_DOCKER', 'true').lower() == 'true'
    container_name = os.getenv('POSTGRES_CONTAINER', 'foodigram_db')
    
    db_params = {
        'user': 'postgres',
        'host': 'localhost',
        'database': 'foodigram',
        'password': 'postgres'  # Default password for local development
    }
    
    # Set environment variables to avoid password prompts
    if not use_docker:
        env = os.environ.copy()
        env['PGPASSWORD'] = db_params['password']
    else:
        env = os.environ.copy()
    
    # Output file with timestamp
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    output_file = f"db/foodigram_data_{timestamp}.sql"
    
    # Choose export method based on Docker usage
    if use_docker:
        # Docker export command
        dump_cmd = [
            'docker', 'exec', container_name,
            'pg_dump',
            '-U', db_params['user'],
            '-d', db_params['database'],
            '--no-owner',
            '--no-privileges',
            '--data-only'  # Only export data, not schema
        ]
    else:
        # Native PostgreSQL export command
        dump_cmd = [
            'pg_dump',
            '-U', db_params['user'],
            '-h', db_params['host'],
            '-d', db_params['database'],
            '--no-owner',
            '--no-privileges',
            '--data-only',  # Only export data, not schema
            '-f', output_file
        ]
    
    # Export database
    if use_docker:
        # For Docker, we need to capture output and write to file
        try:
            print(f"Running: Exporting database")
            print(f"Command: {' '.join(dump_cmd)}")
            result = subprocess.run(dump_cmd, check=True, capture_output=True, text=True, env=env)
            
            # Process output for column ordering if needed
            output_content = result.stdout
            if restaurant_id_last:
                output_content = reorder_db_menus_columns(output_content)
            
            # Write Docker output to file
            with open(output_file, 'w') as f:
                f.write(output_content)
            print(f"✅ Exporting database completed successfully")
        except subprocess.CalledProcessError as e:
            print(f"❌ Exporting database failed:")
            print(f"Error: {e.stderr}")
            return False
    else:
        # For native PostgreSQL, use the existing method
        if not run_command(dump_cmd, "Exporting database", env):
            return False
        
        # Process output for column ordering if needed
        if restaurant_id_last:
            with open(output_file, 'r') as f:
                content = f.read()
            content = reorder_db_menus_columns(content)
            with open(output_file, 'w') as f:
                f.write(content)
    
    print(f"📁 Database exported to: {output_file}")
    
    # Compress if requested
    if compress:
        gzip_cmd = ['gzip', '-k', output_file]
        if run_command(gzip_cmd, "Compressing database file", env):
            compressed_file = f"{output_file}.gz"
            print(f"📦 Compressed file: {compressed_file}")
            
            # Create symlink to latest compressed version
            latest_link = 'db/foodigram_data.sql.gz'
            if os.path.exists(latest_link):
                os.remove(latest_link)
            os.symlink(os.path.basename(compressed_file), latest_link)
            print(f"🔗 Latest compressed version linked as: {latest_link}")
    else:
        # Create symlink to latest uncompressed version
        latest_link = 'db/foodigram_data.sql'
        if os.path.exists(latest_link):
            os.remove(latest_link)
        os.symlink(os.path.basename(output_file), latest_link)
        print(f"🔗 Latest version linked as: {latest_link}")
    
    # Show file sizes
    file_size = os.path.getsize(output_file) / (1024 * 1024)  # MB
    print(f"📊 Export size: {file_size:.1f} MB")
    
    if compress and os.path.exists(f"{output_file}.gz"):
        compressed_size = os.path.getsize(f"{output_file}.gz") / (1024 * 1024)  # MB
        compression_ratio = (1 - compressed_size / file_size) * 100
        print(f"📊 Compressed size: {compressed_size:.1f} MB ({compression_ratio:.1f}% reduction)")
    
    return True

def main():
    parser = argparse.ArgumentParser(description='Export database for team sharing')
    parser.add_argument('--no-compress', action='store_true', 
                       help='Do not compress the exported file (compression is default)')
    parser.add_argument('--restaurant-id-first', action='store_true',
                       help='Place restaurant_id as second column (default: restaurant_id last)')
    
    args = parser.parse_args()
    
    print("🔄 Starting database export...")
    print("=" * 50)
    
    # Compression is default, only disable if --no-compress is specified
    compress = not args.no_compress
    
    # Column ordering: restaurant_id is last by default (matches current DB structure)
    restaurant_id_last = not args.restaurant_id_first
    
    if restaurant_id_last:
        print("📋 Column ordering: restaurant_id will be placed LAST in db_menus (matches current structure)")
    else:
        print("📋 Column ordering: restaurant_id will be placed as SECOND column in db_menus (legacy compatibility)")
    
    if export_database(compress=compress, restaurant_id_last=restaurant_id_last):
        print("=" * 50)
        print("✅ Database export completed successfully!")
        print("\nNext steps for team sharing:")
        print("1. Review the exported file in db/ directory")
        print("2. Share the compressed file with teammates via:")
        print("   - Cloud storage (Google Drive, Dropbox, etc.)")
        print("   - File transfer service")
        print("   - Direct file sharing")
        print("3. Teammates can import using: python settings/team_sync.py import")
    else:
        print("=" * 50)
        print("❌ Database export failed!")
        sys.exit(1)

if __name__ == '__main__':
    main()