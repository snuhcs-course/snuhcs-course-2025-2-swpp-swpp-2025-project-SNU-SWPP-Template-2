ALTER TABLE db_restaurants 
    ADD COLUMN IF NOT EXISTS meaningful_name BOOLEAN DEFAULT FALSE;

ALTER TABLE db_restaurants 
    ADD COLUMN IF NOT EXISTS inferred_menu TEXT DEFAULT '';

ALTER TABLE db_restaurants 
    ADD COLUMN IF NOT EXISTS embedding_vector REAL[] DEFAULT '{}';

ALTER TABLE db_menus 
    ADD COLUMN IF NOT EXISTS embedding_vector REAL[] DEFAULT '{}';

CREATE INDEX IF NOT EXISTS idx_menus_embedding_vector 
    ON db_menus USING GIN (embedding_vector);

CREATE INDEX IF NOT EXISTS idx_restaurants_embedding_vector
    ON db_restaurants USING GIN (embedding_vector);