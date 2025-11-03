#!/usr/bin/env python3
"""
Database Import Script for Team Sharing

This script imports a shared database file from teammates.
Run this when you receive a new database export.

Usage:
    python settings/db_import.py [filename]
    python settings/db_import.py  # Uses latest foodigram_data.sql
"""

import subprocess
import sys
import os
import argparse
import glob
from datetime import datetime

def run_command(cmd, description):
    """Run a shell command and handle errors."""
    print(f"Running: {description}")
    print(f"Command: {' '.join(cmd)}")
    
    try:
        subprocess.run(cmd, check=True, capture_output=True, text=True)
        print(f"✅ {description} completed successfully")
        return True
    except subprocess.CalledProcessError as e:
        print(f"❌ {description} failed:")
        print(f"Error: {e.stderr}")
        return False

def find_latest_export():
    """Find the most recent database export file."""
    
    # Look for compressed files first (preferred since compression is default)
    gz_files = glob.glob('db/foodigram_data_*.sql.gz')
    if gz_files:
        latest_gz = max(gz_files, key=os.path.getmtime)
        return latest_gz
    
    # Look for uncompressed files
    sql_files = glob.glob('db/foodigram_data_*.sql')
    if sql_files:
        latest_sql = max(sql_files, key=os.path.getmtime)
        return latest_sql
    
    # Check for the symlink (but prefer compressed version if it exists)
    if os.path.exists('db/foodigram_data.sql.gz'):
        return 'db/foodigram_data.sql.gz'
    elif os.path.exists('db/foodigram_data.sql'):
        return 'db/foodigram_data.sql'
    
    return None

def import_database(filename=None):
    """Import database from SQL file."""
    
    # Ensure we're in the psql directory
    script_dir = os.path.dirname(os.path.abspath(__file__))
    psql_dir = os.path.dirname(script_dir)
    os.chdir(psql_dir)
    
    # Find the file to import
    if filename is None:
        filename = find_latest_export()
        if filename is None:
            print("❌ No database export files found in db/ directory")
            print("Available files:")
            for f in os.listdir('db/'):
                if f.endswith('.sql') or f.endswith('.sql.gz'):
                    print(f"  - {f}")
            return False
        print(f"📁 Using latest export: {filename}")
    
    if not os.path.exists(filename):
        print(f"❌ File not found: {filename}")
        return False
    
    # Handle compressed files
    sql_file = filename
    if filename.endswith('.gz'):
        print("📦 Decompressing file...")
        decompress_cmd = ['gunzip', '-k', filename]
        if not run_command(decompress_cmd, "Decompressing database file"):
            return False
        sql_file = filename[:-3]  # Remove .gz extension
    
    # Database connection parameters
    db_params = {
        'user': 'postgres',
        'host': 'localhost',
        'database': 'foodigram'
    }
    
    # Create backup of current data (optional)
    backup_file = f"db/backup_before_import_{datetime.now().strftime('%Y%m%d_%H%M%S')}.sql"
    print(f"💾 Creating backup: {backup_file}")
    
    backup_cmd = [
        'pg_dump',
        '-U', db_params['user'],
        '-h', db_params['host'],
        '-d', db_params['database'],
        '--data-only',
        '--no-owner',
        '--no-privileges',
        '-f', backup_file
    ]
    
    # Create backup (continue even if this fails)
    run_command(backup_cmd, "Creating backup")
    
    # Clear existing data (required for data-only imports)
    print("🗑️  Clearing existing data...")
    clear_cmd = [
        'psql',
        '-U', db_params['user'],
        '-h', db_params['host'],
        '-d', db_params['database'],
        '-c', 'DELETE FROM db_menus; DELETE FROM db_restaurants;'
    ]
    
    if not run_command(clear_cmd, "Clearing existing data"):
        print("❌ Error: Could not clear existing data. Import will fail.")
        return False
    
    # Import the new data
    import_cmd = [
        'psql',
        '-U', db_params['user'],
        '-h', db_params['host'],
        '-d', db_params['database'],
        '-f', sql_file
    ]
    
    if not run_command(import_cmd, "Importing database"):
        return False
    
    # Verify import
    verify_cmd = [
        'psql',
        '-U', db_params['user'],
        '-h', db_params['host'],
        '-d', db_params['database'],
        '-c', 'SELECT COUNT(*) AS restaurants FROM db_restaurants; SELECT COUNT(*) AS menus FROM db_menus;'
    ]
    
    print("🔍 Verifying import...")
    subprocess.run(verify_cmd)
    
    # Show file info
    file_size = os.path.getsize(sql_file) / (1024 * 1024)  # MB
    mod_time = datetime.fromtimestamp(os.path.getmtime(sql_file))
    print(f"📊 Imported file: {sql_file}")
    print(f"📊 File size: {file_size:.1f} MB")
    print(f"📊 File date: {mod_time.strftime('%Y-%m-%d %H:%M:%S')}")
    
    return True

def main():
    parser = argparse.ArgumentParser(description='Import shared database file')
    parser.add_argument('filename', nargs='?', 
                       help='Database file to import (default: latest export)')
    
    args = parser.parse_args()
    
    print("📥 Starting database import...")
    print("=" * 50)
    
    if import_database(args.filename):
        print("=" * 50)
        print("✅ Database import completed successfully!")
        print("\nNext steps:")
        print("1. Test the application: python testing/integration_test_psql.py")
        print("2. Start Django server: cd .. && python manage.py runserver")
        print("3. Check Django admin: http://localhost:8000/admin/")
    else:
        print("=" * 50)
        print("❌ Database import failed!")
        sys.exit(1)

if __name__ == '__main__':
    main()