#!/usr/bin/env python3
"""
Database Cleanup Script

This script removes existing foodigram database and recreates it fresh.
Useful for clean setup when switching between teammates or starting fresh.

Usage:
    python settings/clean_db.py
"""

import os
import sys
import subprocess
from pathlib import Path

# Load environment variables from .env file if it exists
env_file = Path(__file__).parent / '.env'
if env_file.exists():
    with open(env_file) as f:
        for line in f:
            if line.strip() and not line.startswith('#'):
                key, value = line.strip().split('=', 1)
                os.environ.setdefault(key, value)

def run_command(cmd, description, ignore_errors=False):
    """Run a shell command and handle errors."""
    print(f"🔄 {description}")
    print(f"Command: {' '.join(cmd)}")
    
    try:
        result = subprocess.run(cmd, check=True, capture_output=True, text=True)
        print(f"✅ {description} completed successfully")
        return True
    except subprocess.CalledProcessError as e:
        if ignore_errors:
            print(f"⚠️  {description} failed (continuing anyway)")
            print(f"Error: {e.stderr}")
            return True
        else:
            print(f"❌ {description} failed:")
            print(f"Error: {e.stderr}")
            return False

def clean_docker_database():
    """Clean Docker-based PostgreSQL database."""
    
    print("🐳 Cleaning Docker PostgreSQL database...")
    
    # Change to settings directory
    script_dir = Path(__file__).parent
    os.chdir(script_dir)
    
    # Stop containers
    if not run_command(['docker', 'compose', 'down'], "Stopping Docker containers"):
        return False
    
    # Remove volume (ignore errors if doesn't exist)
    run_command(['docker', 'volume', 'rm', 'settings_postgres_data'], 
               "Removing Docker volume", ignore_errors=True)
    
    # Start fresh containers
    if not run_command(['docker', 'compose', 'up', '-d'], "Starting fresh Docker containers"):
        return False
    
    # Wait a moment for container to be ready
    print("⏳ Waiting for container to be ready...")
    import time
    time.sleep(3)
    
    # Verify container is running
    result = subprocess.run(['docker', 'ps'], capture_output=True, text=True)
    if 'foodigram_db' in result.stdout:
        print("✅ Docker container is running")
        return True
    else:
        print("❌ Docker container failed to start")
        return False

def clean_native_database():
    """Clean native PostgreSQL database."""
    
    print("🏠 Cleaning native PostgreSQL database...")
    
    # Set password environment
    env = os.environ.copy()
    env['PGPASSWORD'] = 'postgres'
    
    # Drop database if exists
    drop_cmd = [
        'psql',
        '-U', 'postgres',
        '-h', 'localhost',
        '-c', 'DROP DATABASE IF EXISTS foodigram;'
    ]
    
    if not run_command(drop_cmd, "Dropping existing database", env=env):
        return False
    
    # Create fresh database
    create_cmd = [
        'psql',
        '-U', 'postgres', 
        '-h', 'localhost',
        '-c', """CREATE DATABASE foodigram
                 WITH ENCODING 'UTF8'
                      LC_COLLATE='en_US.utf8'
                      LC_CTYPE='en_US.utf8'
                      TEMPLATE=template0;"""
    ]
    
    if not run_command(create_cmd, "Creating fresh database", env=env):
        return False
    
    # Enable extensions
    ext_cmd = [
        'psql',
        '-U', 'postgres',
        '-h', 'localhost', 
        '-d', 'foodigram',
        '-c', "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\"; CREATE EXTENSION IF NOT EXISTS \"postgis\";"
    ]
    
    if not run_command(ext_cmd, "Enabling database extensions", env=env):
        return False
    
    return True

def main():
    print("🧹 Database Cleanup Tool")
    print("=" * 50)
    
    # Check configuration
    use_docker = os.getenv('USE_DOCKER', 'true').lower() == 'true'
    
    if use_docker:
        print("📦 Detected Docker configuration")
        success = clean_docker_database()
    else:
        print("🏠 Detected native PostgreSQL configuration")
        success = clean_native_database()
    
    print("=" * 50)
    
    if success:
        print("✅ Database cleanup completed successfully!")
        print("\nNext steps:")
        print("1. Apply schema: docker exec -i foodigram_db psql -U postgres -d foodigram < ../db/schema.sql")
        print("2. Load data: python ../preprocess/into_db.py")
        print("3. Test connection: python team_sync.py status")
    else:
        print("❌ Database cleanup failed!")
        print("\nTroubleshooting:")
        if use_docker:
            print("- Check Docker is running: docker ps")
            print("- Check docker-compose.yml exists")
        else:
            print("- Check PostgreSQL is running: systemctl status postgresql")
            print("- Check credentials are correct")
        sys.exit(1)

if __name__ == '__main__':
    main()