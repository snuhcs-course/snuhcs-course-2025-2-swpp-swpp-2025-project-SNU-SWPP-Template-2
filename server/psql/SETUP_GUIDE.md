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

# The psql tables are managed separately (see step 5)
```

## 5. Initialize psql Database Schema

### Apply Database Schema (Docker)
```bash
# From server/ directory - using Docker PostgreSQL
docker exec -i foodigram_db psql -U postgres -d foodigram < psql/db/schema.sql

# Alternative: Copy files into container and execute
# docker cp psql/db/schema.sql foodigram_db:/tmp/
# docker exec foodigram_db psql -U postgres -d foodigram -f /tmp/schema.sql
```

### Apply Database Schema (Native PostgreSQL - Alternative)
```bash
# Only use this if not using Docker
psql -U postgres -h localhost -d foodigram -f psql/db/schema.sql
```

### Before Loading Initial Data
```bash
# Navigate to psql directory
cd psql/

cp settings/.env.example settings/.env
# Edit settings/.env and add your OpenAI API key
```

### Loading Initial Data
```bash
# Load restaurant and menu data (~5 minutes total)
python preprocess/into_db.py        # Load raw data (~3 min)
python preprocess/preprocess.py     # Clean menu names (~2 min)
```

### Verify Data Loading
```bash
# Method 1: Check via Docker PostgreSQL directly
docker exec foodigram_db psql -U postgres -d foodigram -c "
SELECT 'Restaurants: ' || COUNT(*) FROM db_restaurants;
SELECT 'Menus: ' || COUNT(*) FROM db_menus;
"

# Method 2: Check via Django (from server/ directory)
python -c "
import os, django
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'config.settings')
django.setup()
from psql_data.models import DbRestaurant, DbMenu
print(f'Restaurants: {DbRestaurant.objects.count()}')
print(f'Menus: {DbMenu.objects.count()}')
"
```

**Expected Output:**
```
Restaurants: 3799
Menus: 36445
```

## 6. Generate Embeddings (Optional - for AI features)

```bash
python preprocess/embedding.py
```

## 7. Team Database Sharing

### Export Database for Sharing (Docker)
```bash
# Method 1: Use automated export script (recommended)
cd psql/
python settings/team_sync.py export

# Method 2: Manual Docker export
docker exec foodigram_db pg_dump -U postgres -d foodigram \
  --no-owner --no-privileges --data-only \
  > db/foodigram_data.sql

# Compress for sharing
gzip -k db/foodigram_data.sql
```

### Import Shared Database (Docker)
```bash
# Method 1: Use automated import script (recommended)
cd psql/
python settings/team_sync.py import

# Method 2: Manual Docker import
cd psql/
# If using compressed file
gunzip -k db/foodigram_data.sql.gz

# Clear existing data and import
docker exec foodigram_db psql -U postgres -d foodigram -c "DELETE FROM db_menus; DELETE FROM db_restaurants;"
docker exec -i foodigram_db psql -U postgres -d foodigram < db/foodigram_data.sql
```

### Export/Import for Native PostgreSQL (Alternative)
```bash
# Only use if not using Docker
# Export
pg_dump -U postgres -h localhost -d foodigram \
  --no-owner --no-privileges --data-only \
  -f db/foodigram_data.sql

# Import  
psql -U postgres -h localhost -d foodigram -f db/foodigram_data.sql
```

## 8. Django Admin Setup

### Create Django Superuser
```bash
# From server/ directory
python manage.py createsuperuser
```

### Access Django Admin
1. Start Django development server:
   ```bash
   python manage.py runserver
   ```
2. Visit http://localhost:8000/admin/
3. Login with superuser credentials
4. Navigate to "Psql_Data" section to manage restaurants and menus

## 9. Daily Workflow for Teams

### Before Starting Work
```bash
# 1. Pull latest code
git pull origin develop

# 2. Activate environment
cd server && source venv/bin/activate

# 3. Check if new database updates are available
ls -la psql/db/foodigram_data.sql*

# 4. If newer database exists, import it
cd psql && psql -U postgres -d foodigram -f db/foodigram_data.sql
```

### After Making Database Changes
```bash
# 1. Export your changes
cd psql/
pg_dump -U postgres -h localhost -d foodigram \
  --no-owner --no-privileges --clean --if-exists \
  -f db/foodigram_data.sql

# 2. Compress for sharing
gzip -f db/foodigram_data.sql

# 3. Commit to git
git add db/foodigram_data.sql.gz
git commit -m "Update database with [description of changes]"
git push origin [your-branch]
```

### Working with Django Models
```python
# Example: Accessing data through Django
from psql_data.models import DbRestaurant, DbMenu

# Get restaurants near Seoul National University
restaurants = DbRestaurant.objects.filter(
    name__icontains='서울대'
)

# Get menus with high ratings
popular_menus = DbMenu.objects.filter(
    recommend=True,
    restaurant__avg_rating__gte=4.0
)

# Update menu recommendations
DbMenu.objects.filter(
    taste_profile__sweet__gt=0.8
).update(recommend=True)
```

## 10. Testing the Setup

### Run Integration Tests
```bash
cd psql/
python testing/integration_test_psql.py
```

### Run Recommendation Demo
```bash
python recommend/recommend.py
```

### Expected Test Results
- ✅ Database connection successful
- ✅ 3,799 restaurants loaded
- ✅ 36,445 menus loaded
- ✅ Spatial queries working
- ✅ Django admin accessible
- ✅ Recommendations generated


### Permission Issues
```bash
# Grant necessary permissions
sudo -u postgres psql -d foodigram -c "
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO postgres;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO postgres;
"
```

### Data Loading Issues

#### Using Docker (Default)
```bash
# Clear and reload data using Docker
docker exec foodigram_db psql -U postgres -d foodigram -c "
DROP TABLE IF EXISTS db_menus CASCADE;
DROP TABLE IF EXISTS db_restaurants CASCADE;
"
docker exec -i foodigram_db psql -U postgres -d foodigram < psql/db/schema.sql
python psql/preprocess/into_db.py
```

#### Using Native PostgreSQL (Alternative)
```bash
# Only use if not using Docker
psql -U postgres -h localhost -d foodigram -c "
DROP TABLE IF EXISTS db_menus CASCADE;
DROP TABLE IF EXISTS db_restaurants CASCADE;
"
psql -U postgres -h localhost -d foodigram -f psql/db/schema.sql
python psql/preprocess/into_db.py
```

### Django Integration Issues
```bash
# Reset Django database state
python manage.py migrate --fake-initial
python manage.py migrate
```

## 12. Advanced Features

### Using AI Features
Requires OpenAI API key in `psql/settings/.env`:
```bash
# Generate embeddings for semantic search
python preprocess/embedding.py

# Update taste profiles and allergen info
python preprocess/update_menu_allergens.py

# Run recommendation system
python recommend/recommend.py
```

### PostGIS Spatial Queries
```python
# Example: Find restaurants within 1km of a point
from django.contrib.gis.geos import Point
from django.contrib.gis.measure import D
from psql_data.models import DbRestaurant

point = Point(126.9525, 37.4583)  # Seoul National University
nearby = DbRestaurant.objects.filter(
    geom__distance_lte=(point, D(km=1))
)
```

## 13. File Structure After Setup

```
server/
├── psql/
│   ├── db/
│   │   ├── schema.sql
│   │   ├── schema_update.sql
│   │   ├── foodigram_data.sql.gz      # Shared database
│   │   └── foodigram_data.sql         # Latest export
│   ├── settings/
│   │   ├── .env                       # Your API keys
│   │   └── docker-compose.yml
│   └── [other psql directories]
├── psql_data/                         # Django app
│   ├── models.py                      # Database models
│   └── admin.py                       # Admin interface
└── venv/                              # Contains all dependencies
```

## Quick Start Summary

For teammates who have completed the initial setup:

```bash
# 1. Pull latest code
git pull origin develop

# 2. Activate environment
cd server && source venv/bin/activate

# 3. Import latest database
cd psql && gunzip -k db/foodigram_data.sql.gz
psql -U postgres -d foodigram -f db/foodigram_data.sql

# 4. Test everything works
python testing/integration_test_psql.py

# 5. Start Django server
cd .. && python manage.py runserver
```

Visit http://localhost:8000/admin/ to access the Django admin interface for database management.