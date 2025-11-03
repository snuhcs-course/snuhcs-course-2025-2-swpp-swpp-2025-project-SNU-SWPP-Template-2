#!/usr/bin/env python3
"""
Environment Setup Script

This script helps configure the environment for database operations.
It creates the .env file with appropriate settings for Docker or native PostgreSQL.

Usage:
    python settings/setup_env.py --docker     # Configure for Docker (default)
    python settings/setup_env.py --native     # Configure for native PostgreSQL
"""

import os
import sys
import argparse
from pathlib import Path

def setup_environment(use_docker=True):
    """Set up environment configuration."""
    
    script_dir = Path(__file__).parent
    env_file = script_dir / '.env'
    env_example = script_dir / '.env.example'
    
    # Create .env from example if it doesn't exist
    if not env_file.exists():
        if env_example.exists():
            # Copy example file
            with open(env_example, 'r') as f:
                content = f.read()
        else:
            # Create minimal .env content
            content = """# Database Configuration
USE_DOCKER=true
POSTGRES_CONTAINER=foodigram_db
OPENAI_API_KEY=your_openai_api_key_here
"""
        
        with open(env_file, 'w') as f:
            f.write(content)
        print(f"✅ Created .env file: {env_file}")
    else:
        print(f"📁 Using existing .env file: {env_file}")
    
    # Update USE_DOCKER setting
    lines = []
    use_docker_set = False
    
    with open(env_file, 'r') as f:
        for line in f:
            if line.strip().startswith('USE_DOCKER='):
                lines.append(f'USE_DOCKER={str(use_docker).lower()}\n')
                use_docker_set = True
            else:
                lines.append(line)
    
    # Add USE_DOCKER if not found
    if not use_docker_set:
        lines.insert(0, f'USE_DOCKER={str(use_docker).lower()}\n')
    
    # Write updated content
    with open(env_file, 'w') as f:
        f.writelines(lines)
    
    # Show configuration
    connection_type = "Docker container" if use_docker else "Native PostgreSQL"
    print(f"🔧 Configured for: {connection_type}")
    
    if use_docker:
        print("📦 Database connection will use Docker container 'foodigram_db'")
        print("   Make sure Docker is running: docker ps | grep foodigram_db")
    else:
        print("🏠 Database connection will use localhost PostgreSQL")
        print("   Make sure PostgreSQL is running: systemctl status postgresql")
    
    print(f"\n📝 Configuration saved to: {env_file}")
    print("🔑 Don't forget to set your OPENAI_API_KEY in the .env file for AI features!")
    
    # Offer to clean existing database
    print("\n🧹 Database Cleanup:")
    if use_docker:
        print("To ensure clean setup, consider running:")
        print("  docker compose down && docker volume rm settings_postgres_data")
        print("  docker compose up -d")
    else:
        print("To ensure clean setup, consider running:")
        print("  psql -U postgres -c 'DROP DATABASE IF EXISTS foodigram;'")
        print("  psql -U postgres -c 'CREATE DATABASE foodigram;'")

def main():
    parser = argparse.ArgumentParser(description='Configure environment for database operations')
    group = parser.add_mutually_exclusive_group()
    group.add_argument('--docker', action='store_true', default=True,
                      help='Configure for Docker PostgreSQL (default)')
    group.add_argument('--native', action='store_true',
                      help='Configure for native PostgreSQL installation')
    
    args = parser.parse_args()
    
    # If --native is specified, use_docker = False, otherwise True
    use_docker = not args.native
    
    print("🚀 Setting up environment configuration...")
    print("=" * 50)
    
    setup_environment(use_docker)
    
    print("=" * 50)
    print("✅ Environment setup complete!")
    print("\nNext steps:")
    if use_docker:
        print("1. Start Docker: docker compose up -d")
        print("2. Test export: python settings/team_sync.py export")
        print("3. Test import: python settings/team_sync.py import")
    else:
        print("1. Start PostgreSQL: sudo systemctl start postgresql")
        print("2. Test export: python settings/team_sync.py export")
        print("3. Test import: python settings/team_sync.py import")

if __name__ == '__main__':
    main()