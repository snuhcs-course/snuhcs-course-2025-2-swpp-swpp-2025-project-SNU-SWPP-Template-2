# Detailed Documentation

## Organization Structure

The codebase is organized into functional directories for better maintainability:

- **`settings/`** - Configuration files, dependencies, and deployment settings
- **`raw/`** - Raw data files and datasets  
- **`preprocess/`** - Data preprocessing, cleaning, and feature extraction tools
- **`recommend/`** - Core recommendation engine and algorithms
- **`testing/`** - Test suites and quality assurance tools

## Data Preprocessing

### Menu Name Normalization (`preprocess/preprocess.py`)

Processes menu names to create clean, normalized versions for better search and recommendation accuracy.

#### Processing Steps
1. Convert all alphabets to uppercase
2. Remove size indicators (S, M, L, R, XL, 대, 중, 소, mini, 미니)
3. Remove set menu words (세트, SET)
4. Remove price ranges (e.g., "7000-12000원", "5000~8000원")
5. Remove quantity patterns (e.g., "2개", "500ml", "1.25L")
6. Remove common keywords (NEW, 추가, 옵션, 선택, 판매종료)
7. Keep only alphabets, Korean characters, and spaces
8. Replace multiple spaces with single space

#### Usage Commands

**Preview changes without updating:**
```bash
python preprocess/preprocess.py --preview
```

**Update name_clean values only:**
```bash
python preprocess/preprocess.py --update-names --force
```

**Update embedding vectors only:**
```bash
python preprocess/preprocess.py --update-embeddings --force
```

**Update both names and embeddings:**
```bash
python preprocess/preprocess.py --update-all --force
```

**Full processing (categories + menus):**
```bash
python preprocess/preprocess.py
```

#### Examples

**Original:** `보양식통문어 황제해물찜(중) 2인분 15000원`  
**Cleaned:** `보양식통문어 황제해물찜`

**Original:** `A세트 NEW 치킨버거+콜라 1.5L 추가옵션`  
**Cleaned:** `치킨버거 콜라`

### Korean Food Term Analysis (`preprocess/foodlist.py`)

Analyzes Korean food terms from menu data using morphological analysis.

#### Features
- Fetches all `name_clean` values from `db_menus` table
- Short preprocessing: normalizes "돈카츠", "돈까스", "카츠" → "돈가스"
- Uses KoNLPy Mecab for Korean POS (Part-of-Speech) tagging
- Extracts NNG (일반명사/Common Noun), VA (형용사/Adjective), XR (어근/Root) tagged words
- Counts occurrences and saves results to JSON

#### Requirements
- Java JDK installed on system
- Python package: `konlpy>=0.6.0`

#### Usage
```bash
python preprocess/foodlist.py
```

#### Output
- Generates `food_terms_nng.json` with word counts
- Shows top 20 most common food terms
- Includes metadata about analysis parameters

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

## Recommendation Features

### Image-Aware Recommendations

The system includes support for menu image URLs and intelligent sorting:

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

## Troubleshooting Details

### Port Conflicts
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

## Advanced Testing

```bash
python testing/integration_test_psql.py       # Full integration test suite
python testing/unit_test_psql.py               # Unit tests for components
python -m unittest testing.integration_test_psql.DatabaseTestCase      # Database tests only
python -m unittest testing.integration_test_psql.PerformanceTestCase   # Performance tests only
cd testing && coverage run --source=. unit_test_psql.py 2>/dev/null && coverage report # Checking coverage
```

**Expected Results:**
- ✅ 3,799 restaurants loaded
- ✅ 36,445 menus loaded
- ✅ All spatial queries under 5 seconds
- ✅ Recommendations generated under 30 seconds
- ✅ client.py unit test showing 86% coverage

## Contributing

1. Update database schema in `db/schema.sql` or `db/schema_update.sql`
2. Add new parameters to `.env.example`
3. Update configuration documentation
4. Add tests in `integration_test_psql.py` or `unit_test_psql.py`
5. Update this README

## License

Part of SWPP 2025 course project.