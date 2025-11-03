# Backend (Recommendation System)

A PostgreSQL-based management system with AI-powered recommendations and semantic search.

## Quick Start

### For First-Time Setup
See [SETUP_GUIDE.md](SETUP_GUIDE.md) for complete installation instructions.

### For Team Members (Daily Use)
```bash
# 1. Pull latest code and activate environment
git pull origin develop
cd server && source venv/bin/activate

# 2. Start database (if using Docker)
cd psql/settings && docker compose up -d

# 3. Import the latest database (automatically detects compressed files)
cd ../.. && cd psql && python settings/team_sync.py import

# 4. Verify everything works
python settings/team_sync.py status

# 5. Start Django server for database management
cd .. && python manage.py runserver
# Visit http://localhost:8000/admin/
```

### Making Database Changes
```bash
# 1. Make your changes via Django admin or scripts
cd server && python manage.py runserver

# 2. Export changes for team sharing (compressed by default)
cd psql && python settings/team_sync.py export

# 3. Share the database file manually (due to large file size)
# The compressed file will be created as: db/foodigram_data_YYYYMMDD_HHMMSS.sql.gz
# Share this file with teammates via:
# - Cloud storage (Google Drive, Dropbox, etc.)
# - File transfer service
# - Direct file sharing
```

## Directory Structure

```
psql/
├── settings/              # Configuration and setup files
├── raw/                   # Raw data files
├── preprocess/            # Data preprocessing tools
├── recommend/             # Recommendation engine
├── testing/               # Test suites
├── db/                    # Database schema
└── test_run/              # Test execution data
```

## Key Features

- **Django Integration** for easy database management and team collaboration
- **PostGIS spatial queries** for location-based filtering
- **AI-powered menu categorization** using LangChain with GPT-4o-mini
- **Semantic search** with Korean embeddings (BM-K/KoSimCSE-roberta)
- **Advanced clustering** (HDBSCAN, KMeans, Spectral)
- **Image-aware recommendations** with intelligent sorting
- **Team database sharing** via SQL exports with automated scripts

## Usage Examples

### Django Admin (Recommended for Database Management)
```bash
# Start Django server
cd server && python manage.py runserver

# Visit http://localhost:8000/admin/
# Navigate to "Psql_Data" section to manage:
# - Restaurants: View, edit, filter restaurant data
# - Menus: Manage menu items, taste profiles, allergen info
```

### Team Database Operations
```bash
# Note: Run from server/psql/ directory with venv activated
cd server && source venv/bin/activate && cd psql

# Check current database status
python settings/team_sync.py status

# Export database for sharing (creates compressed .sql.gz file)
python settings/team_sync.py export

# Import shared database (handles both .sql and .sql.gz files)
python settings/team_sync.py import

# Verify setup
python settings/team_sync.py setup

# Export without compression (if needed for special cases)
python settings/db_export.py --no-compress

# Export with restaurant_id as second column (for legacy compatibility)
python settings/db_export.py --restaurant-id-first
```

### Command Line Recommendations
```bash
# Note: Activate virtual environment first
cd server && source venv/bin/activate

# Can run from any of these directories:
python psql/recommend/recommend.py               # From server/
cd psql && python recommend/recommend.py         # From server/psql/
cd psql/recommend && python recommend.py         # From server/psql/recommend/

# Basic recommendations
python recommend/recommend.py                     # Default profile
python recommend/recommend.py user1               # Custom profile (user1.json)

# Different methods and clustering
python recommend/recommend.py --method langchain  # Traditional AI categorization
python recommend/recommend.py --method embedding  # Embedding-based clustering
python recommend/recommend.py user1 --clustering hdbscan    # Use HDBScan
python recommend/recommend.py user1 --clustering kmeans     # Use KMeans
```

### Programmatic API
```python
# Django models (run from server/ directory)
import django
import os
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'config.settings')
django.setup()

from psql_data.models import DbRestaurant, DbMenu

# Get restaurants near Seoul National University
restaurants = DbRestaurant.objects.filter(name__icontains='서울대')
print(f"Found {restaurants.count()} restaurants")

# Get recommended menus with taste profiles
sweet_menus = DbMenu.objects.filter(
    recommend=True,
    taste_profile__sweet__gt=0.7
)
print(f"Found {sweet_menus.count()} sweet menus")

# Using recommendation engine (works from any directory with venv activated)
import sys
sys.path.append('psql/recommend')  # Adjust path based on current directory
from client import RestaurantRecommender, UserProfile

user = UserProfile(
    location=(126.9525, 37.4583),
    cuisine_preferences=["korean"],
    max_distance_km=1.5
)

recommender = RestaurantRecommender()  # Automatically finds .env files
recommendations = recommender.generate_recommendations(user_profile=user)
print(f"Generated {len(recommendations)} recommendations")
recommender.close()  # Don't forget to close!
```

## Configuration

The system automatically searches for `.env` files in multiple locations:
- Current working directory
- `psql/recommend/` directory  
- `psql/` directory
- `psql/settings/` directory
- `server/` directory

Key environment variables in `.env`:

```env
# OpenAI API
OPENAI_API_KEY=your_key_here

# Clustering method (hdbscan, kmeans, spectral)
CLUSTERING_METHOD=hdbscan

# LLM settings
LLM_TEMPERATURE=0.1
LLM_BATCH_SIZE=20

# Embedding model
EMBEDDING_MODEL_NAME=BM-K/KoSimCSE-roberta
```

## Troubleshooting

### Common Issues
```bash
# Database connection issues
python settings/team_sync.py setup  # Check setup

# Database port conflicts
sudo service postgresql stop
docker compose down && docker compose up -d

# Django database issues
cd server && python manage.py check
python manage.py migrate

# Import/export issues
python settings/team_sync.py status  # Check current state

# API key issues
echo $OPENAI_API_KEY  # Verify key is set

# Dependencies
source ../venv/bin/activate  # Use server virtual environment
```

### Django Admin Issues
- **Can't access admin**: Create superuser with `python manage.py createsuperuser`
- **Models not showing**: Check `psql_data` app is in `INSTALLED_APPS`
- **Data not visible**: Verify database connection and run migrations

### Performance Tuning
- **Slow processing**: Reduce `LLM_BATCH_SIZE` to 10
- **Memory issues**: Reduce `EMBEDDING_BATCH_SIZE` to 16
- **Poor clustering**: Try different `CLUSTERING_METHOD`

## Testing

```bash
# Note: Run from server/ directory with venv activated
cd server && source venv/bin/activate && cd psql

python testing/integration_test_psql.py       # Full test suite (14 tests)
python testing/unit_test_psql.py             # Unit tests
```


## For detailed documentation...
See [DETAILED.md](DETAILED.md)