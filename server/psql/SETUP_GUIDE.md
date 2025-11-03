# Complete Setup Guide for psql/ Database Management

This guide provides step-by-step instructions for setting up the psql database system integrated with Django for easy management and team sharing.

## Prerequisites

- Python 3.9+
- Git (for team collaboration)
- Admin/sudo access for PostgreSQL installation

## 1. PostgreSQL Installation

### macOS
```bash
# Using Homebrew
brew install postgresql@14 postgis
brew services start postgresql@14

# Create database user
createuser -s postgres
```

### Ubuntu/Debian
```bash
# Install PostgreSQL and PostGIS
sudo apt update
sudo apt install postgresql-14 postgresql-14-postgis-3 postgresql-client-14

# Start PostgreSQL service
sudo systemctl start postgresql
sudo systemctl enable postgresql

# Create database user
sudo -u postgres createuser -s postgres
sudo -u postgres psql -c "ALTER USER postgres PASSWORD 'postgres';"
```

### Windows
1. Download PostgreSQL 14+ from https://www.postgresql.org/download/windows/
2. Run installer and include PostGIS in the installation
3. Set password for postgres user as 'postgres'
4. Ensure PostgreSQL service is running

## 2. Database Setup

### Using Docker (Recommended)
```bash
cd server/psql/settings

# Stop any existing PostgreSQL services
sudo service postgresql stop

# Remove any existing foodigram database containers (for clean setup)
docker compose down
docker volume rm settings_postgres_data 2>/dev/null || true

# Start fresh Docker PostgreSQL
docker compose up -d

# Verify container is running
docker ps | grep foodigram_db
```

### Manual Database Creation (Alternative)
```bash
# Connect to PostgreSQL
psql -U postgres -h localhost

# Drop existing database if it exists (for clean setup)
DROP DATABASE IF EXISTS foodigram;

# Create the database
CREATE DATABASE foodigram
  WITH ENCODING 'UTF8'
       LC_COLLATE='en_US.utf8'
       LC_CTYPE='en_US.utf8'
       TEMPLATE=template0;

# Connect to the new database
\c foodigram;

# Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "postgis";

# Exit psql
\q
```


## 3. Python Environment Setup

### Navigate to Server Directory
```bash
cd /path/to/project/server
```

### Activate Virtual Environment
```bash
python3 -m venv venv # if no environment exists yet
source venv/bin/activate  # Linux/macOS
# venv\Scripts\activate     # Windows
```

### Install Dependencies
```bash
# Install all required dependencies (Django + psql system requirements)
pip install --upgrade pip
pip install -r requirements.txt
```

### Verify Dependencies
```bash
# Check that all psql dependencies are installed
python -c "import django, psycopg2, langchain, transformers; print('All dependencies OK')"
```

## 4. Django Configuration

### Environment Variables Setup
Django requires AWS environment variables. Create the environment file:

```bash
# From server/ directory
# Create .env.dev file with required AWS variables
cat > .env.dev << 'EOF'
AWS_ACCESS_KEY_ID=dummy_key_for_local_dev
AWS_SECRET_ACCESS_KEY=dummy_secret_for_local_dev
AWS_STORAGE_BUCKET_NAME=dummy_bucket_for_local_dev
EOF
```

**Note**: These are dummy values for local development. The actual AWS functionality won't work, but Django will start properly.

### Database Configuration
The database settings are already configured in `server/config/settings.py`:
```python
DATABASES = {
    'default': {
        'ENGINE': 'django.db.backends.postgresql',
        'NAME': 'foodigram',
        'USER': 'postgres',
        'PASSWORD': 'postgres',
        'HOST': 'localhost',
        'PORT': '5432',
    }
}
```
### Check docker is running
```bash
sudo docker ps # if not running, then execute the following
sudo docker compose up -d
```

### Test Django Connection
```bash
# From server/ directory (only after PostgreSQL connection works)
python manage.py check
```

### Apply Django Migrations
```bash
# Create initial Django tables (users, sessions, etc.)
python manage.py migrate

# The psql tables are automatically created by Django migrations
```

## 5. Initialize Database Schema

Django will automatically create the database tables when you run migrations.

### Before Loading Data
```bash
# Navigate to psql directory
cd psql/

cp settings/.env.example settings/.env
# Edit settings/.env and add your OpenAI API key
```

### Importing DB
```bash
# .sql.gz file stored in psql/db/
python settings/team_sync.py import

# if verifying import returns 0 menus, make sure you ran:
# python manage.py migrate (from server/ directory)
# then try import again
```
## 6. Generate Embeddings (Optional - for AI features)

```bash
python preprocess/embedding.py
```

## 7. Initial Database Import

### Import Database (One-time setup)
```bash
cd psql/
python settings/team_sync.py import
```

## 8. Verify Setup

### Test the Setup
```bash
cd psql/
python testing/integration_test_psql.py
```

### Create Django Superuser (Optional)
```bash
cd server/
python manage.py createsuperuser
```

## Quick Start Summary

After initial setup is complete:
```bash
# Start database and Django
cd server/psql/settings && docker compose up -d
cd ../.. && source venv/bin/activate && python manage.py runserver
```

Visit http://localhost:8000/admin/ for database management.

For detailed usage, troubleshooting, and team workflows, see README.md