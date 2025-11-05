#!/usr/bin/env python3
"""
Team Database Sync Script

Quick commands for common team database operations.

Usage:
    python settings/team_sync.py export     # Export current database for sharing
    python settings/team_sync.py import     # Import latest shared database
    python settings/team_sync.py status     # Show current database status
    python settings/team_sync.py setup      # Initial setup verification
"""

import subprocess
import sys
import os
import argparse
from datetime import datetime

def run_command(cmd, description, capture=True):
    """Run a shell command and handle errors."""
    print(f"🔄 {description}")
    
    try:
        if capture:
            result = subprocess.run(cmd, check=True, capture_output=True, text=True)
        else:
            result = subprocess.run(cmd, check=True)
        return True, result.stdout if capture else ""
    except subprocess.CalledProcessError as e:
        error_msg = e.stderr if capture and e.stderr else str(e)
        print(f"❌ Failed: {error_msg}")
        return False, error_msg

def check_database_status():
    """Check current database status."""
    print("📊 Database Status")
    print("=" * 30)
    
    # Check PostgreSQL connection
    success, _ = run_command([
        'psql', '-U', 'postgres', '-h', 'localhost', 
        '-d', 'foodigram', '-c', 'SELECT 1;'
    ], "Testing database connection")
    
    if not success:
        print("❌ Cannot connect to database")
        return False
    
    # Get record counts
    count_cmd = [
        'psql', '-U', 'postgres', '-h', 'localhost', 
        '-d', 'foodigram', '-t', '-c',
        """
        SELECT 'Restaurants: ' || COUNT(*) FROM db_restaurants;
        SELECT 'Menus: ' || COUNT(*) FROM db_menus;
        SELECT 'Menus with embeddings: ' || COUNT(*) FROM db_menus WHERE embedding_vector IS NOT NULL AND array_length(embedding_vector, 1) > 0;
        SELECT 'Menus with taste profiles: ' || COUNT(*) FROM db_menus WHERE taste_profile IS NOT NULL;
        SELECT 'Recommended menus: ' || COUNT(*) FROM db_menus WHERE recommend = true;
        """
    ]
    
    success, output = run_command(count_cmd, "Getting database statistics")
    if success and output:
        for line in output.strip().split('\n'):
            if line.strip():
                print(f"  {line.strip()}")
    
    # Check for recent exports
    print("\n📁 Recent Exports")
    print("-" * 20)
    
    if os.path.exists('db'):
        exports = []
        for f in os.listdir('db'):
            if f.startswith('foodigram_data_') and (f.endswith('.sql') or f.endswith('.sql.gz')):
                path = os.path.join('db', f)
                mtime = os.path.getmtime(path)
                size = os.path.getsize(path) / (1024 * 1024)  # MB
                exports.append((f, mtime, size))
        
        if exports:
            exports.sort(key=lambda x: x[1], reverse=True)
            for name, mtime, size in exports[:5]:  # Show last 5
                dt = datetime.fromtimestamp(mtime)
                print(f"  {name} ({size:.1f}MB) - {dt.strftime('%Y-%m-%d %H:%M')}")
        else:
            print("  No exports found")
    
    return True

def export_database():
    """Export current database."""
    script_path = os.path.join(os.path.dirname(__file__), 'db_export.py')
    # Compression is now default, no need to specify --compress
    success, _ = run_command([sys.executable, script_path], 
                           "Exporting database", capture=False)
    return success

def import_database():
    """Import latest database."""
    script_path = os.path.join(os.path.dirname(__file__), 'db_import.py')
    success, _ = run_command([sys.executable, script_path], 
                           "Importing database", capture=False)
    return success

def setup_check():
    """Verify setup is correct."""
    print("🔧 Setup Verification")
    print("=" * 30)
    
    checks = [
        {
            'name': 'PostgreSQL connection',
            'cmd': ['psql', '-U', 'postgres', '-h', 'localhost', '-d', 'foodigram', '-c', 'SELECT 1;']
        },
        {
            'name': 'PostGIS extension',
            'cmd': ['psql', '-U', 'postgres', '-h', 'localhost', '-d', 'foodigram', 
                   '-c', "SELECT EXISTS(SELECT 1 FROM pg_extension WHERE extname = 'postgis');"]
        },
        {
            'name': 'Database tables exist',
            'cmd': ['psql', '-U', 'postgres', '-h', 'localhost', '-d', 'foodigram',
                   '-c', "SELECT COUNT(*) FROM information_schema.tables WHERE table_name IN ('db_restaurants', 'db_menus');"]
        }
    ]
    
    all_passed = True
    for check in checks:
        success, output = run_command(check['cmd'], f"Checking {check['name']}")
        if success:
            print(f"✅ {check['name']}")
        else:
            print(f"❌ {check['name']}")
            all_passed = False
    
    # Check Python dependencies
    try:
        import django  # noqa: F401
        import psycopg2  # noqa: F401
        print("✅ Python dependencies (Django, psycopg2)")
    except ImportError as e:
        print(f"❌ Python dependencies: {e}")
        all_passed = False
    
    # Check file structure
    required_files = [
        'db/schema.sql',
        'db/schema_update.sql',
        'settings/docker-compose.yml'
    ]
    
    for file_path in required_files:
        if os.path.exists(file_path):
            print(f"✅ {file_path}")
        else:
            print(f"❌ {file_path}")
            all_passed = False
    
    if all_passed:
        print("\n🎉 Setup is complete and working!")
    else:
        print("\n⚠️  Some issues found. Check the setup guide.")
    
    return all_passed

def main():
    parser = argparse.ArgumentParser(description='Team database sync operations')
    parser.add_argument('action', choices=['export', 'import', 'status', 'setup'],
                       help='Action to perform')
    
    args = parser.parse_args()
    
    # Ensure we're in the right directory
    script_dir = os.path.dirname(os.path.abspath(__file__))
    psql_dir = os.path.dirname(script_dir)
    os.chdir(psql_dir)
    
    print(f"🏠 Working directory: {psql_dir}")
    print(f"⏰ Time: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print()
    
    if args.action == 'status':
        success = check_database_status()
    elif args.action == 'export':
        success = export_database()
    elif args.action == 'import':
        success = import_database()
    elif args.action == 'setup':
        success = setup_check()
    else:
        print(f"❌ Unknown action: {args.action}")
        success = False
    
    if success:
        print("\n✅ Operation completed successfully!")
    else:
        print("\n❌ Operation failed!")
        sys.exit(1)

if __name__ == '__main__':
    main()