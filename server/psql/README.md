# Restaurant Recommendation System

A PostgreSQL-based restaurant management system with AI-powered recommendations, semantic search, and advanced clustering capabilities.

## Features

- **PostGIS spatial queries** for location-based restaurant filtering
- **AI-powered menu categorization** using LangChain with GPT-4o-mini
- **Semantic search** with Korean language embeddings (BM-K/KoSimCSE-roberta)
- **Advanced clustering algorithms** (HDBSCAN, KMeans, Spectral)
- **Image-aware recommendations** with support for menu image URLs
- **Intelligent sorting** prioritizing menus with images and embedding similarity
- **Configurable parameters** via environment variables

## Quick Start

### 1. Environment Setup
```bash
cd server/psql
cp .env.example .env
# Edit .env with your OpenAI API key
```

### 2. Start Database
```bash
docker compose up -d
```

### 3. Install Dependencies
```bash
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate
pip install -r requirements.txt
```

### 4. Load Data
```bash
python into_db.py        # Load restaurant data (~3 min)
python preprocess.py     # Process menu names (~2 min)
python embedding.py      # Generate embeddings (~15 min)
```

### 5. Test System
```bash
python recommend.py              # Run demo
python integration_test_psql.py # Run integration tests
python unit_test_psql.py         # Run unit tests
```

## Database Schema

### Initial Setup (`db/schema.sql`)
- `db_restaurants`: Restaurant data with PostGIS coordinates
- `db_menus`: Menu items with pricing and categories

### Schema Updates (`db/schema_update.sql`)

**When to update:** After initial data loading, before running embeddings.

```bash
# Apply schema updates for embedding support
docker exec -it foodigram_db psql -U postgres -d foodigram -f /docker-entrypoint-initdb.d/schema_update.sql
```

**Added columns:**
- `meaningful_name` (BOOLEAN): Restaurant name contains cuisine info
- `inferred_menu` (TEXT): AI-extracted menu information  
- `embedding_vector` (REAL[]): 768-dimensional semantic vectors

## Configuration Parameters

### Clustering Methods (`CLUSTERING_METHOD`)

#### HDBSCAN (default)
Best for discovering natural clusters of varying density.
```env
CLUSTERING_METHOD=hdbscan
HDBSCAN_MIN_CLUSTER_SIZE=3          # Minimum items per cluster
HDBSCAN_MIN_SAMPLES=1               # Core point threshold
HDBSCAN_CLUSTER_SELECTION_EPSILON=0.3  # Clustering flexibility
```

#### KMeans
Best for creating specific number of balanced clusters.
```env
CLUSTERING_METHOD=kmeans
KMEANS_MAX_CLUSTERS=15              # Maximum clusters allowed
KMEANS_CLUSTER_DIVISOR=6            # items_count ÷ divisor = clusters
KMEANS_N_INIT=20                    # Initialization attempts
KMEANS_MAX_ITER=500                 # Maximum iterations
```

#### Spectral Clustering
Best for complex, non-linear cluster shapes.
```env
CLUSTERING_METHOD=spectral
SPECTRAL_MAX_CLUSTERS=8             # Maximum clusters allowed
SPECTRAL_CLUSTER_DIVISOR=8          # items_count ÷ divisor = clusters  
SPECTRAL_N_NEIGHBORS=10             # Neighborhood graph size
```

### Clustering Tuning Guide

**For bigger clusters:** Increase divisor values
```env
KMEANS_CLUSTER_DIVISOR=10           # 200 items → 20 clusters
SPECTRAL_CLUSTER_DIVISOR=15         # 200 items → 13 clusters
```

**For smaller clusters:** Decrease divisor values
```env
KMEANS_CLUSTER_DIVISOR=3            # 200 items → 66 clusters
SPECTRAL_CLUSTER_DIVISOR=4          # 200 items → 50 clusters
```

**Fallback similarity clustering:**
```env
SIMILARITY_THRESHOLD_DEFAULT=0.7    # Standard threshold
SIMILARITY_THRESHOLD_AGGRESSIVE=0.4 # Lower = bigger clusters
```

### LLM Configuration
```env
LLM_TEMPERATURE=0.1                 # Response consistency
LLM_MAX_TOKENS=500                  # Response length limit
LLM_BATCH_SIZE=20                   # Items per categorization batch
```

### Embedding Model
```env
EMBEDDING_MODEL_NAME=BM-K/KoSimCSE-roberta
EMBEDDING_MAX_LENGTH=512            # Token limit per text
EMBEDDING_BATCH_SIZE=32             # Processing batch size
```

## User Profiles

Create JSON files in `test_run/user/`:

```json
{
  "location_info": "Seoul National University",
  "location": [126.9525, 37.4583],
  "cuisine_preferences": ["korean", "japanese"],
  "max_distance_km": 1.5
}
```

**Cuisine aliases:**
- `korean` → 한식, `japanese` → 일식, `chinese` → 중식
- `western` → 양식, `snackfood` → 분식, `fastfood` → 패스트푸드
- `meat` → 육류, `seafood` → 해산물

## Usage Examples

### Command Line
```bash
python recommend.py                     # Default profile
python recommend.py japanese_lover      # Custom profile
python recommend.py --help             # Show options
```

### Programmatic API
```python
from client import RestaurantRecommender, UserProfile

user = UserProfile(
    location=(126.9525, 37.4583),
    cuisine_preferences=["korean"],
    max_distance_km=1.5
)

recommender = RestaurantRecommender()
recommendations = recommender.generate_recommendations(user_profile=user)
```

## Performance Tuning

### Clustering Performance
- **KMeans**: Fastest, good for balanced clusters
- **HDBSCAN**: Medium speed, best for natural grouping
- **Spectral**: Slowest, best for complex shapes

### Memory Optimization
- Reduce `EMBEDDING_BATCH_SIZE` if running low on memory
- Use `LLM_BATCH_SIZE=10` for slower but more reliable processing
- Set `KMEANS_MAX_CLUSTERS=30` to limit complexity

### Speed Optimization
- Increase `LLM_BATCH_SIZE=30` for faster categorization
- Use `KMEANS_CLUSTER_DIVISOR=3` for more granular results
- Set `LLM_MAX_TOKENS=200` for shorter AI responses

## Directory Structure

```
psql/
├── client.py               # Main recommendation engine
├── embedding.py            # ML embedding generation  
├── recommend.py            # Demo script
├── integration_test_psql.py # Integration tests
├── unit_test_psql.py       # Unit tests
├── requirements.txt        # Dependencies
├── .env.example           # Configuration template
├── db/
│   ├── schema.sql         # Initial database schema
│   └── schema_update.sql  # Embedding support updates
└── test_run/
    ├── user/              # User profile JSON files
    └── result/            # Recommendation results
```

## Recommendation Features

### Image-Aware Recommendations

The system now includes support for menu image URLs and intelligent sorting:

```python
# Generate recommendations with image and embedding distance sorting
recommendations = recommender.generate_recommendations(
    user_profile=user,
    max_distance_km=1.0,
    method="embedding",  # or "langchain"
    clustering_method="spectral"
)

# Each menu now includes:
# - images: List of image URLs
# - embedding_distance_to_center: Distance from category centroid
```

### Sorting Algorithm

Menus within each category are sorted by:

1. **Image priority**: Menus with images appear first
2. **Embedding similarity**: Closest to category center (lowest cosine distance)

### Output Format

```json
{
  "user_location": [126.9338, 37.4721],
  "total_restaurants": 30,
  "recommendations": {
    "찌개류": {
      "reason": "matches your korean preference",
      "menus": [
        {
          "name": "김치찌개",
          "restaurant": "맛있는집",
          "price": 8000,
          "images": ["http://example.com/image1.jpg"],
          "embedding_distance_to_center": 0.125
        }
      ]
    }
  }
}
```

### Display Features

- 📷 indicator for menus with images
- Embedding distance values (lower = more representative of category)
- Sorted output prioritizing visual content

## Troubleshooting

### Port Occuptaion Issues
```bash
sudo lsof -i :5432 # Checking if port 5432 is occupied by postgresql
sudo service postgresql stop
```

### Database Issues
```bash
docker compose down && docker compose up -d
sudo lsof -i :5432  # Check port conflicts
```

### API Issues
```bash
echo $OPENAI_API_KEY  # Verify key is set
# Rate limits: Wait or upgrade OpenAI plan
```

### Clustering Issues
- **Too many small clusters**: Increase divisor values
- **Few giant clusters**: Decrease divisor values  
- **Spectral warnings**: Switch to `CLUSTERING_METHOD=kmeans`

### Performance Issues
- **Slow recommendations**: Reduce `LLM_BATCH_SIZE` and `max_menus_to_categorize`
- **High memory usage**: Reduce `EMBEDDING_BATCH_SIZE`
- **Poor clustering**: Try different `CLUSTERING_METHOD`

## Testing

```bash
python integration_test_psql.py       # Full integration test suite
python unit_test_psql.py               # Unit tests for components
python -m unittest integration_test_psql.DatabaseTestCase      # Database tests only
python -m unittest integration_test_psql.PerformanceTestCase   # Performance tests only
```

**Expected Results:**
- ✅ 3,799 restaurants loaded
- ✅ 36,445 menus loaded
- ✅ All spatial queries under 5 seconds
- ✅ Recommendations generated under 30 seconds

## Contributing

1. Update database schema in `db/schema.sql` or `db/schema_update.sql`
2. Add new parameters to `.env.example`
3. Update configuration documentation
4. Add tests in `integration_test_psql.py` or `unit_test_psql.py`
5. Update this README

## License

Part of SWPP 2025 course project.