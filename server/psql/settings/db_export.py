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

def export_database(compress=True):
    """Export the database to SQL file."""
    
    # Ensure we're in the psql directory
    script_dir = os.path.dirname(os.path.abspath(__file__))
    psql_dir = os.path.dirname(script_dir)
    os.chdir(psql_dir)
    
    # Create db directory if it doesn't exist
    os.makedirs('db', exist_ok=True)
    
    # Database connection parameters
    db_params = {
        'user': 'postgres',
        'host': 'localhost',
        'database': 'foodigram'
    }
    
    # Output file with timestamp
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    output_file = f"db/foodigram_data_{timestamp}.sql"
    
    # pg_dump command
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
    if not run_command(dump_cmd, "Exporting database"):
        return False
    
    print(f"📁 Database exported to: {output_file}")
    
    # Compress if requested
    if compress:
        gzip_cmd = ['gzip', '-k', output_file]
        if run_command(gzip_cmd, "Compressing database file"):
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
    
    args = parser.parse_args()
    
    print("🔄 Starting database export...")
    print("=" * 50)
    
    # Compression is default, only disable if --no-compress is specified
    compress = not args.no_compress
    
    if export_database(compress=compress):
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