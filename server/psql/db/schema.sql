-- ============================================================================
-- foodigram Database Schema
-- PostgreSQL 14
-- UTF-8 safe for Korean text and emoji (full international support)
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. Create Database (executed only when running outside docker-entrypoint)
-- ----------------------------------------------------------------------------
-- Note: If this file is placed in docker-entrypoint-initdb.d, Postgres will
--       automatically create the 'foodigram' database using POSTGRES_DB env var.
--       Otherwise, uncomment the following lines to create manually:
--
-- CREATE DATABASE foodigram
--   WITH ENCODING 'UTF8'
--        LC_COLLATE='ko_KR.utf8'
--        LC_CTYPE='ko_KR.utf8'
--        TEMPLATE=template0;
--
-- \connect foodigram;
-- ----------------------------------------------------------------------------

-- Safety: Drop old tables if reloaded manually
DROP TABLE IF EXISTS menus CASCADE;
DROP TABLE IF EXISTS restaurants CASCADE;

-- ----------------------------------------------------------------------------
-- 2. Enable Extensions
-- ----------------------------------------------------------------------------
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "postgis";

-- ----------------------------------------------------------------------------
-- 3. Table: restaurants
-- ----------------------------------------------------------------------------
CREATE TABLE db_restaurants (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    external_id     VARCHAR(20) UNIQUE,         -- Original ID from source JSON
    name            TEXT NOT NULL,              -- e.g., "쟝블랑제리 낙성대본점"
    category        TEXT,                       -- e.g., "베이커리"
    phone           TEXT,
    address         TEXT,
    road_address    TEXT,
    group1          TEXT,                       -- e.g., "서울"
    group2          TEXT,                       -- e.g., "관악구"
    group3          TEXT,                       -- e.g., "봉천동"
    category_code   TEXT,
    category_code_list TEXT[],
    geom            TEXT,                       -- PostGIS geometry stored as text (GDAL compatibility)
    place_images    TEXT[],                     -- Array of URLs
    avg_rating      NUMERIC(3,2),
    review_count    INTEGER,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    category_normalized TEXT,                  -- Normalized category for consistent querying
    meaningful_name BOOLEAN DEFAULT FALSE,
    inferred_menu   TEXT DEFAULT '',
    embedding_vector REAL[] DEFAULT '{}'
);

-- ----------------------------------------------------------------------------
-- 4. Table: menus
-- ----------------------------------------------------------------------------
CREATE TABLE db_menus (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    restaurant_id   UUID REFERENCES db_restaurants(id) ON DELETE CASCADE,
    external_id     TEXT,                       -- {restaurant_id}_{menu_index}
    name            TEXT NOT NULL,              -- e.g., "단팥빵"
    price           INTEGER,
    description     TEXT,
    images          TEXT[],                     -- URLs
    recommend       BOOLEAN DEFAULT FALSE,
    index_in_rest   INTEGER,                    -- menu index within restaurant

    -- Added fields (from preprocess.py)
    name_clean      TEXT,                       -- regex-cleaned version
    taste_profile   JSONB,                      -- JSON object for taste characteristics
    allergen_info   JSONB,                      -- JSON object for allergen information
    embedding_vector REAL[] DEFAULT '{}',       -- embeddings for similarity search
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- ----------------------------------------------------------------------------
-- 5. Triggers to update updated_at automatically
-- ----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION update_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_restaurants_update
BEFORE UPDATE ON db_restaurants
FOR EACH ROW EXECUTE FUNCTION update_timestamp();

CREATE TRIGGER trg_menus_update
BEFORE UPDATE ON db_menus
FOR EACH ROW EXECUTE FUNCTION update_timestamp();

-- ----------------------------------------------------------------------------
-- 6. Indexes (for performance)
-- ----------------------------------------------------------------------------

-- Geometry text index (for when PostGIS is available)
CREATE INDEX IF NOT EXISTS idx_restaurants_geom
    ON db_restaurants (geom);

-- Text search / filtering
CREATE INDEX IF NOT EXISTS idx_restaurants_name
    ON db_restaurants USING GIN (to_tsvector('simple', name));

CREATE INDEX IF NOT EXISTS idx_restaurants_category
    ON db_restaurants (category);

CREATE INDEX IF NOT EXISTS idx_menus_name
    ON db_menus USING GIN (to_tsvector('simple', name));

CREATE INDEX IF NOT EXISTS idx_menus_name_clean
    ON db_menus USING GIN (to_tsvector('simple', name_clean));

-- Embedding vector index for similarity search
CREATE INDEX IF NOT EXISTS idx_restaurants_embedding_vector
    ON db_restaurants USING GIN (embedding_vector);
    
CREATE INDEX IF NOT EXISTS idx_menus_embedding_vector
    ON db_menus USING GIN (embedding_vector);

-- ----------------------------------------------------------------------------
-- 7. View (optional): Quick joined menu view
-- ----------------------------------------------------------------------------
CREATE OR REPLACE VIEW menu_with_restaurant AS
SELECT
    m.id AS menu_id,
    m.name AS menu_name,
    m.name_clean AS menu_name_clean,
    m.price,
    r.name AS restaurant_name,
    r.category,
    r.geom
FROM db_menus m
JOIN db_restaurants r ON r.id = m.restaurant_id;

-- ============================================================================
-- End of schema.sql
-- ============================================================================
