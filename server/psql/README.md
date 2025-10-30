# Restaurant Database Management System

This directory contains a PostgreSQL-based restaurant and menu information management system with AI-powered recommendation capabilities.

## Overview

The system processes restaurant data from JSON files, stores it in a PostgreSQL database with PostGIS support, and provides intelligent menu recommendations based on user preferences, location, and dietary restrictions.

## Directory Structure

```
psql/
├── specifications.md       # System specifications and requirements
├── restaurants_gwanak.json # Raw restaurant data (Gwanak-gu area)
├── into_db.py              # Data loading script
├── preprocess.py           # AI-powered menu processing
├── embedding.py            # ML embedding generation for recommendations
├── client.py               # Restaurant recommendation engine
├── sample_run.py           # Demo script with JSON-based user profiles
├── test.py                 # Test suite for recommendation system
├── test_run/               # User profiles and results storage
│   ├── user/               # JSON user profile files
│   │   ├── default.json    # Default user profile
│   │   └── *.json          # Additional user profiles
│   └── result/             # Timestamped recommendation results
├── requirements.txt        # Python dependencies
├── .env.example           # Environment configuration template
├── .gitignore             # Git ignore rules
├── docker-compose.yml     # PostgreSQL container setup
├── db/
│   ├── schema.sql         # Database schema definition
│   └── schema_update.sql  # Schema updates for embedding support
└── README.md              # This file
```

## Features

### 1. Database Schema (`db/schema.sql`)
- **db_restaurants**: Restaurant information with PostGIS spatial data and embedding vectors
- **db_menus**: Menu items with normalized name processing and embedding vectors
- Automatic timestamp updates and optimized indexes
- Full Unicode support for Korean text
- ML embedding support for semantic similarity matching

### 2. Data Loading (`into_db.py`)
- Loads restaurant data from JSON files
- Handles coordinate conversion for PostGIS
- Processes review statistics and images
- Transactional data loading with error handling

### 3. Menu Processing (`preprocess.py`)
- Cleans menu names using regex patterns:
  - Removes size indicators: S, M, L, 대, 중, 소
  - Removes set menu words: 세트, set, Set, SET
  - Removes price ranges: patterns like "7000-12000원", "5000~8000원"
  - Removes quantity patterns: "2개", "500ml", etc.
  - Keeps only alphanumeric and Korean characters

### 4. ML Embedding Generation (`embedding.py`)
- **Restaurant name analysis** using GPT-4o-mini for menu inference
- **Vector embedding generation** using BM-K/KoSimCSE-roberta model
- **Multiprocessing support** with connection pooling and retry logic
- **Batch processing** for efficient database updates
- **Meaningful name detection** for restaurants with specific cuisine indicators

### 5. Recommendation System (`client.py`)
- **PostGIS-based spatial queries** for nearby restaurants
- **AI-powered menu categorization** using LangChain with gpt-4o-mini
- **Optimized performance** with hexadecimal indices for token reduction
- **Parameterizable distance limits** via UserProfile
- **Maximum 10 menu categories** with intelligent consolidation
- **Cuisine preference matching** with alias support (korean → 한식)
- **Location-based restaurant filtering** limited to top 30 closest

### 6. Demo System (`sample_run.py`)
- **JSON-based user profiles** stored in `test_run/user/`
- **Command line parameter support** for profile selection
- **Automatic result saving** with timestamps in `test_run/result/`
- **Human-readable location descriptions** in user profiles

### 7. Test Suite (`test.py`)
- Database connectivity and integrity tests
- Recommendation algorithm validation
- Performance benchmarking
- Menu categorization testing
- Spatial query testing

## Quick Start Guide

### Prerequisites
- Docker and Docker Compose installed
- Python 3.8+ installed
- OpenAI API key (for AI-powered menu categorization)

### Complete Setup (First Time)

1. **Navigate to the psql directory**
   ```bash
   cd server/psql
   ```

2. **Environment Setup**
   ```bash
   # Copy environment template
   cp .env.example .env
   
   # Edit .env with your OpenAI API key
   nano .env
   # Add: OPENAI_API_KEY=your_openai_api_key_here
   ```

3. **Start Database**
   ```bash
   # Start PostgreSQL with PostGIS (first time may take a few minutes)
   docker compose up -d
   
   # Wait for database to be ready (check logs)
   docker logs foodigram_db
   ```

4. **Install Python Dependencies**
   ```bash
   # Create virtual environment (recommended)
   python -m venv venv
   source venv/bin/activate  # On Windows: venv\Scripts\activate
   
   # Install required packages
   pip install -r requirements.txt
   ```

5. **Load and Process Data**
   ```bash
   # 1. Load raw restaurant data into database (takes ~2-3 minutes)
   python into_db.py
   
   # 2. Process and normalize menu names (takes ~1-2 minutes)
   python preprocess.py
   
   # 3. Generate ML embeddings for semantic search (takes ~10-15 minutes)
   python embedding.py
   ```

6. **Test the System**
   ```bash
   # Run demo with default user profile
   python sample_run.py
   
   # Expected output: Restaurant recommendations with categorized menus
   ```

### Quick Demo (After Setup)

```bash
# Use different user profiles
python sample_run.py                    # Default profile
python sample_run.py japanese_lover     # Japanese cuisine focused
python sample_run.py --help            # Show all options
```

## ML Embedding Features

### Embedding Pipeline (`embedding.py`)

The embedding system provides semantic similarity matching for restaurants and menus:

```bash
# Run full embedding pipeline
python embedding.py

# Run only restaurant name analysis
python embedding.py --meaningful

# Force regeneration of embeddings
python embedding.py --menu-embedding --restaurant-embedding
```

**Features:**
- **Restaurant Name Analysis**: Uses GPT-4o-mini to detect meaningful menu information in restaurant names
- **Vector Embeddings**: BM-K/KoSimCSE-roberta model for Korean text semantic similarity
- **Multiprocessing**: Parallel database updates with connection pooling
- **Retry Logic**: Handles connection failures with exponential backoff
- **Performance Optimized**: Limited to 4 concurrent workers to prevent database overload

**Database Schema Updates:**
- `meaningful_name` (BOOLEAN): Whether restaurant name contains specific cuisine/menu info
- `inferred_menu` (TEXT): Extracted menu information from restaurant name
- `embedding_vector` (REAL[]): 768-dimensional embedding vectors for semantic search

### Docker Configuration Improvements

**Enhanced PostgreSQL Settings:**
```yaml
command: >
  postgres
  -c max_connections=200        # Increased from default 100
  -c shared_buffers=256MB       # Optimized memory usage
  -c effective_cache_size=1GB   # Better query performance
  -c work_mem=4MB              # Improved sort operations
```

## Usage Examples

### Demo Script Usage

```bash
# Use default user profile (test_run/user/default.json)
python sample_run.py

# Use specific user profile
python sample_run.py japanese_lover

# Show help
python sample_run.py --help
```

### Creating Custom User Profiles

Create new JSON files in `test_run/user/` directory:

**Example: `test_run/user/my_profile.json`**
```json
{
  "location_info": "Seoul National University Venture Town",
  "location": [126.933830, 37.472087],
  "cuisine_preferences": ["korean", "japanese"],
  "max_distance_km": 1.5
}
```

**Available cuisine preferences:**
- `"korean"` → 한식 (Korean food)
- `"japanese"` → 일식 (Japanese food)
- `"chinese"` → 중식 (Chinese food)
- `"western"` → 양식 (Western food)
- `"snackfood"` → 분식 (Korean snacks)
- `"fastfood"` → 패스트푸드 (Fast food)
- `"coffee/beverage"` → 커피/음료 (Coffee/Beverages)
- `"meat"` → 육류/고기요리 (Meat dishes)
- `"seafood"` → 해산물 (Seafood)
- And more... (see specifications.md for complete list)

**Usage:**
```bash
python sample_run.py my_profile  # Uses my_profile.json
```

### Programmatic API Usage

```python
from client import RestaurantRecommender, UserProfile

# Create user profile
user = UserProfile(
    location=(126.9525, 37.4583),  # Seoul National University
    location_info="Seoul National University",
    cuisine_preferences=["korean", "japanese"],
    max_distance_km=1.5
)

# Get recommendations (uses gpt-4o-mini by default)
recommender = RestaurantRecommender()
recommendations = recommender.generate_recommendations(
    user_profile=user,
    max_menus_to_categorize=30  # Reduced for faster processing
)
```

### Manual Database Queries

```sql
-- Find restaurants within 1km of SNU
SELECT name, category, 
       ST_Distance(geom::geography, ST_SetSRID(ST_Point(126.9525, 37.4583), 4326)::geography) as distance
FROM db_restaurants 
WHERE ST_DWithin(geom::geography, ST_SetSRID(ST_Point(126.9525, 37.4583), 4326)::geography, 1000)
ORDER BY distance;

-- Find menus with normalized names
SELECT m.name, m.name_clean, r.name as restaurant
FROM db_menus m
JOIN db_restaurants r ON r.id = m.restaurant_id
WHERE m.name_clean IS NOT NULL
ORDER BY r.name, m.name;
```

## Data Processing Pipeline

1. **Raw Data** → `restaurants_gwanak.json`
2. **Database Loading** → `into_db.py` → PostgreSQL tables
3. **Menu Processing** → `preprocess.py` → Normalized menu names
4. **ML Embeddings** → `embedding.py` → Semantic vectors and restaurant analysis
5. **Recommendations** → `client.py` → User-specific menu categories
6. **Testing** → `test.py` → System validation

## Configuration Options

### User Profile Parameters
- **location**: `(longitude, latitude)` coordinates
- **location_info**: Human-readable location description (optional)
- **cuisine_preferences**: List of cuisine types (korean, japanese, chinese, etc.)
- **max_distance_km**: Maximum search radius in kilometers

### Recommendation Parameters
- **max_distance_km**: Override search radius (uses UserProfile.max_distance_km if None)
- **categories**: Restaurant category filters with alias support
- **max_menus_to_categorize**: Number of menus to send to AI (default: 30)
- **max_menus_per_category**: Maximum menus shown per category (default: 3)
- **max_restaurants**: Maximum restaurants returned (default: 30, closest first)

### Performance Optimizations
- **Hexadecimal indices**: Reduced token usage in AI communication
- **gpt-4o-mini model**: Faster and more cost-effective than GPT-4
- **Category consolidation**: Maximum 10 categories with "Miscellaneous" for overflow
- **Batch processing**: Smaller batches (15 items) for better reliability

## Testing

### Running Tests

```bash
# Run all tests
python test.py

# Run specific test categories
python -m unittest test.DatabaseTestCase
python -m unittest test.RecommendationTestCase
python -m unittest test.PerformanceTestCase

# Run with verbose output
python -m unittest -v
```

### Test Categories

1. **Database Tests** (`DatabaseTestCase`)
   - Connection verification
   - Extension availability (PostGIS, UUID)
   - Table structure validation
   - Data integrity checks
   - Spatial data validation

2. **Recommendation Tests** (`RecommendationTestCase`)
   - Nearby restaurant finding
   - Menu retrieval accuracy
   - Menu categorization functionality
   - Full pipeline integration

3. **Performance Tests** (`PerformanceTestCase`)
   - Recommendation generation timing
   - Spatial query performance
   - Database query optimization

### Test Requirements

- Database must be running and populated
- Environment variables configured (.env file)
- All Python dependencies installed

## Performance Considerations

- **Spatial Indexes**: PostGIS GIST indexes for fast location queries
- **API Rate Limits**: Built-in delays for OpenAI API calls
- **Batch Processing**: Configurable batch sizes for large datasets
- **Caching**: Consider implementing Redis for frequent queries

## Troubleshooting

### Common Issues

1. **Database Connection Error**
   ```bash
   # Restart database container
   docker compose down && docker compose up -d
   
   # Check if port 5432 is occupied
   sudo lsof -i :5432
   
   # If occupied by local PostgreSQL, stop it
   sudo service postgresql stop
   ```

2. **OpenAI API Errors**
   ```bash
   # Check API key is set
   echo $OPENAI_API_KEY
   
   # Rate limit exceeded - wait or upgrade plan
   # Invalid API key - regenerate on OpenAI platform
   ```

3. **Python Dependencies Issues**
   ```bash
   # Use virtual environment to avoid conflicts
   python -m venv venv
   source venv/bin/activate
   pip install --upgrade pip
   pip install -r requirements.txt
   ```

4. **Data Loading Errors**
   ```bash
   # Check if database is ready
   docker logs foodigram_db
   
   # Verify schema exists
   docker exec -it foodigram_db psql -U postgres -d foodigram -c "\dt"
   ```

5. **Docker Issues**
   ```bash
   # Remove and recreate containers
   docker compose down -v
   docker compose up -d
   
   # Check Docker is running
   docker --version
   docker compose --version
   ```

### Data Validation

```bash
# Check data loading results
python -c "
import psycopg2
conn = psycopg2.connect('postgresql://postgres:postgres@localhost/foodigram')
cur = conn.cursor()
cur.execute('SELECT COUNT(*) FROM db_restaurants')
print(f'Restaurants: {cur.fetchone()[0]}')
cur.execute('SELECT COUNT(*) FROM db_menus')
print(f'Menus: {cur.fetchone()[0]}')
"
```
**Expected Data Load Results:**
- ✅ 3,799 restaurants loaded
- ✅ 36,445 menus loaded  
- ✅ 100% restaurants have coordinates
- ✅ 96% menus have price information

## API Reference

### RestaurantRecommender Class

- `find_nearby_restaurants(user_profile, max_distance_km, categories)`: Find restaurants by location/category
- `get_restaurant_menus(restaurant_ids)`: Get menus for specific restaurants
- `categorize_menus(menus)`: AI-powered menu categorization using LangChain
- `generate_recommendations(user_profile, ...)`: Complete recommendation pipeline

## Contributing

When adding new features:

1. Update the database schema in `db/schema.sql`
2. Modify data loading logic in `into_db.py` if needed
3. Extend menu processing in `preprocess.py` for new normalization rules
4. Update recommendation logic in `client.py`
5. Add corresponding tests in `test.py`
6. Update documentation

## License

This project is part of the SWPP 2025 course project.