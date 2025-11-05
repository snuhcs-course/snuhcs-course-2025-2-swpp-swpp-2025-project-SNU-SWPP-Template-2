#!/usr/bin/env python3
"""
into_db.py - Load restaurant data from JSON into PostgreSQL database

This script processes restaurants_gwanak.json and populates the db_restaurants
and db_menus tables according to the schema defined in db/schema.sql.
"""

import json
import os
import sys
from typing import Dict, List, Any, Optional
import psycopg2
from psycopg2.extras import RealDictCursor
from psycopg2.extensions import register_adapter, AsIs
import numpy as np
from dotenv import load_dotenv

# Load environment variables
load_dotenv()

# Database connection parameters
DB_CONFIG = {
    'host': os.getenv('DB_HOST', 'localhost'),
    'port': os.getenv('DB_PORT', '5432'),
    'database': os.getenv('DB_NAME', 'foodigram'),
    'user': os.getenv('DB_USER', 'postgres'),
    'password': os.getenv('DB_PASSWORD', 'postgres')
}

def adapt_numpy_int64(numpy_int64):
    """Adapter for numpy int64 to PostgreSQL int"""
    return AsIs(int(numpy_int64))

def adapt_numpy_float64(numpy_float64):
    """Adapter for numpy float64 to PostgreSQL float"""
    return AsIs(float(numpy_float64))

# Register numpy adapters
register_adapter(np.int64, adapt_numpy_int64)
register_adapter(np.float64, adapt_numpy_float64)

def get_db_connection():
    """Create and return a database connection"""
    try:
        conn = psycopg2.connect(**DB_CONFIG)
        conn.autocommit = False
        return conn
    except Exception as e:
        print(f"Error connecting to database: {e}")
        sys.exit(1)

def parse_price(price_str: Any) -> Optional[int]:
    """Parse price string handling ranges and various formats"""
    if not price_str:
        return None
    
    # Convert to string if not already
    price_str = str(price_str).strip()
    
    if not price_str or price_str == '0':
        return None
    
    # Handle ranges like "7000~12000" - take the lower value
    if '~' in price_str:
        price_str = price_str.split('~')[0].strip()
    
    # Handle ranges like "7000-12000" - take the lower value
    if '-' in price_str and not price_str.startswith('-'):
        price_str = price_str.split('-')[0].strip()
    
    # Remove any non-digit characters except minus sign
    import re
    clean_price = re.sub(r'[^\d-]', '', price_str)
    
    try:
        return int(clean_price) if clean_price else None
    except ValueError:
        return None

def load_json_data(file_path: str) -> List[Dict[str, Any]]:
    """Load restaurant data from JSON file"""
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            data = json.load(f)
        print(f"Loaded {len(data)} restaurants from {file_path}")
        return data
    except Exception as e:
        print(f"Error loading JSON file: {e}")
        sys.exit(1)

def insert_restaurant(cursor, restaurant_data: Dict[str, Any]) -> str:
    """Insert a restaurant into db_restaurants table and return the UUID"""
    detail_info = restaurant_data.get('detail_info', {})
    visitor_stats = detail_info.get('visitor_review_stats', {})
    review_info = visitor_stats.get('review', {}) if visitor_stats else {}
    
    # Extract coordinates
    longitude = detail_info.get('x')
    latitude = detail_info.get('y')
    geom_point = None
    if longitude and latitude:
        geom_point = f"SRID=4326;POINT({longitude} {latitude})"
    
    # Prepare data
    insert_data = {
        'external_id': str(restaurant_data.get('id', '')),
        'name': restaurant_data.get('name', ''),
        'category': detail_info.get('category', ''),
        'phone': detail_info.get('phone', ''),
        'address': detail_info.get('address', ''),
        'road_address': detail_info.get('road_address', ''),
        'group1': detail_info.get('group1', ''),
        'group2': detail_info.get('group2', ''),
        'group3': detail_info.get('group3', ''),
        'category_code': detail_info.get('category_code', ''),
        'category_code_list': detail_info.get('category_code_list', []),
        'place_images': detail_info.get('place_images', []),
        'avg_rating': float(review_info.get('avgRating', 0)) if review_info.get('avgRating') else None,
        'review_count': int(review_info.get('totalCount', 0)) if review_info.get('totalCount') else None
    }
    
    # Build query dynamically based on whether geometry is available
    if geom_point:
        query = """
        INSERT INTO db_restaurants (
            external_id, name, category, phone, address, road_address,
            group1, group2, group3, category_code, category_code_list,
            geom, place_images, avg_rating, review_count
        ) VALUES (
            %(external_id)s, %(name)s, %(category)s, %(phone)s, %(address)s, %(road_address)s,
            %(group1)s, %(group2)s, %(group3)s, %(category_code)s, %(category_code_list)s,
            ST_GeomFromText(%(geom)s), %(place_images)s, %(avg_rating)s, %(review_count)s
        ) ON CONFLICT (external_id) DO UPDATE SET
            name = EXCLUDED.name,
            category = EXCLUDED.category,
            phone = EXCLUDED.phone,
            address = EXCLUDED.address,
            road_address = EXCLUDED.road_address,
            group1 = EXCLUDED.group1,
            group2 = EXCLUDED.group2,
            group3 = EXCLUDED.group3,
            category_code = EXCLUDED.category_code,
            category_code_list = EXCLUDED.category_code_list,
            geom = EXCLUDED.geom,
            place_images = EXCLUDED.place_images,
            avg_rating = EXCLUDED.avg_rating,
            review_count = EXCLUDED.review_count,
            updated_at = NOW()
        RETURNING id;
        """
        insert_data['geom'] = geom_point
    else:
        query = """
        INSERT INTO db_restaurants (
            external_id, name, category, phone, address, road_address,
            group1, group2, group3, category_code, category_code_list,
            place_images, avg_rating, review_count
        ) VALUES (
            %(external_id)s, %(name)s, %(category)s, %(phone)s, %(address)s, %(road_address)s,
            %(group1)s, %(group2)s, %(group3)s, %(category_code)s, %(category_code_list)s,
            %(place_images)s, %(avg_rating)s, %(review_count)s
        ) ON CONFLICT (external_id) DO UPDATE SET
            name = EXCLUDED.name,
            category = EXCLUDED.category,
            phone = EXCLUDED.phone,
            address = EXCLUDED.address,
            road_address = EXCLUDED.road_address,
            group1 = EXCLUDED.group1,
            group2 = EXCLUDED.group2,
            group3 = EXCLUDED.group3,
            category_code = EXCLUDED.category_code,
            category_code_list = EXCLUDED.category_code_list,
            place_images = EXCLUDED.place_images,
            avg_rating = EXCLUDED.avg_rating,
            review_count = EXCLUDED.review_count,
            updated_at = NOW()
        RETURNING id;
        """
    
    try:
        cursor.execute(query, insert_data)
        result = cursor.fetchone()
        restaurant_uuid = result['id']
        return restaurant_uuid
    except Exception as e:
        print(f"Error inserting restaurant {insert_data['name']}: {type(e).__name__}: {str(e)}")
        print(f"Query: {query}")
        print(f"Data: {insert_data}")
        raise

def insert_menus(cursor, restaurant_uuid: str, menus_data: List[Dict[str, Any]]):
    """Insert menus for a restaurant into db_menus table"""
    for menu in menus_data:
        menu_data = {
            'restaurant_id': restaurant_uuid,
            'external_id': menu.get('id', ''),
            'name': menu.get('name', ''),
            'price': parse_price(menu.get('price')),
            'description': menu.get('description', ''),
            'images': menu.get('images', []) if menu.get('images') else [],
            'index_in_rest': int(menu.get('index', 0)) if menu.get('index') is not None else None
        }
        
        query = """
        INSERT INTO db_menus (
            restaurant_id, external_id, name, price, description, images, index_in_rest
        ) VALUES (
            %(restaurant_id)s, %(external_id)s, %(name)s, %(price)s, 
            %(description)s, %(images)s, %(index_in_rest)s
        );
        """
        
        try:
            cursor.execute(query, menu_data)
        except Exception as e:
            print(f"Error inserting menu {menu_data['name']}: {type(e).__name__}: {str(e)}")
            print(f"Menu data: {menu_data}")
            raise

def clear_existing_data(cursor):
    """Clear existing data from tables"""
    print("Clearing existing data...")
    cursor.execute("DELETE FROM db_menus;")
    cursor.execute("DELETE FROM db_restaurants;")
    print("Existing data cleared.")

def main():
    """Main function to load data into database"""
    # Check if JSON file exists
    json_file = 'restaurants_gwanak.json'
    if not os.path.exists(json_file):
        print(f"Error: {json_file} not found in current directory")
        sys.exit(1)
    
    # Load data
    restaurants_data = load_json_data(json_file)
    
    # Connect to database
    conn = get_db_connection()
    
    try:
        with conn.cursor(cursor_factory=RealDictCursor) as cursor:
            # Clear existing data
            clear_existing_data(cursor)
            
            # Process each restaurant
            total_restaurants = len(restaurants_data)
            total_menus = 0
            
            for i, restaurant in enumerate(restaurants_data, 1):
                try:
                    # Insert restaurant (with automatic duplicate handling)
                    restaurant_uuid = insert_restaurant(cursor, restaurant)
                    
                    # Insert menus
                    menus = restaurant.get('detail_info', {}).get('menus', [])
                    if menus:
                        insert_menus(cursor, restaurant_uuid, menus)
                        total_menus += len(menus)
                    
                    if i % 1000 == 0:
                        print(f"Processed {i}/{total_restaurants} restaurants...")
                        
                except Exception as e:
                    error_type = type(e).__name__
                    print(f"Error processing restaurant {i}: {error_type}: {str(e)}")
                    print(f"Restaurant name: {restaurant.get('name', 'unknown')}")
                    conn.rollback()
                    raise
            
            # Commit all changes
            conn.commit()
            print(f"\nSuccessfully loaded:")
            print(f"  - {total_restaurants} restaurants")
            print(f"  - {total_menus} menus")
            
    except Exception as e:
        print(f"Database operation failed: {type(e).__name__}: {str(e)}")
        conn.rollback()
        sys.exit(1)
    finally:
        conn.close()

if __name__ == "__main__":
    main()