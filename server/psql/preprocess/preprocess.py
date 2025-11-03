#!/usr/bin/env python3
"""
preprocess.py - Create normalized menu names

This script processes menu names to create normalized versions by:
1. Converting all alphabets to uppercase
2. Removing size indicators: S, M, L, 대, 중, 소, R, XL, mini, 미니
3. Removing set menu words: 세트, set, Set, SET
4. Removing price ranges: patterns like "7000-12000원", "5000~8000원"
5. Removing quantity patterns: "2개", "500ml", "1.25L", etc.
6. Keeping only alphabets, Korean characters, and blankspaces

Usage:
  python preprocess.py                    # Full processing (default)
  python preprocess.py --update-names     # Update name_clean only
  python preprocess.py --update-embeddings # Update embedding_vector only
  python preprocess.py --update-all       # Update both name_clean and embedding_vector
"""

import argparse
import os
import re
import sys
from typing import Optional, Set
import psycopg2
from psycopg2.extras import RealDictCursor
from dotenv import load_dotenv
from tqdm import tqdm

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

# Valid category mappings from specifications.md
VALID_CATEGORIES = {
    '한식': [
        '곰탕/설렁탕', '국밥', '국수', '기사식당', '냉면', '덮밥', '두부요리', '막국수', 
        '만두', '백반/가정식', '보리밥', '비빔밥', '순대/순댓국', '쌈밥', '아부찌부대찌개', 
        '오므라이스', '죽', '찌개/전골', '추어탕', '칼국수/만두', '한식', '한식뷔페', 
        '한정식', '해장국'
    ],
    '일식': [
        '돈가스', '우동/소바', '일본식라면', '일식당', '일식튀김/꼬치', '초밥/롤'
    ],
    '분식': [
        '33떡볶이', '개성진찹쌀순대', '김밥', '떡볶이', '라면', '분식', '오니기리', 
        '오뎅/꼬치', '전/빈대떡', '종합분식', '토스트', '핫도그'
    ],
    '중식': [
        '딤섬/중식만두', '마라탕', '중식당'
    ],
    '양식': [
        '스테이크/립', '스파게티/파스타전문', '스파게티스토리', '양식'
    ],
    '세계음식': [
        '멕시코/남미음식', '베트남음식', '스페인음식', '아시아음식', '이탈리아음식', 
        '인도음식', '카레', '태국음식', '터키음식'
    ],
    '패스트푸드': [
        '서오릉피자', '피자', '햄버거', '후렌치후라이'
    ],
    '육류/고기요리': [
        '갈비탕', '감자탕', '고기뷔페', '곱창/막창/양', '닭갈비', '닭발', '닭볶음탕', 
        '닭요리', '돼지고기구이', '백숙/삼계탕', '불닭', '사철/영양탕', '샤브샤브', 
        '소고기구이', '양꼬치', '오리요리', '육류/고기요리', '장수통닭', '정육식당', 
        '정육점', '족발/보쌈', '찜닭', '치킨/닭강정'
    ],
    '해산물': [
        '게요리', '굴요리', '낙지요리', '대게요리', '매운탕/해물탕', '복어요리', 
        '생선구이', '생선회', '아귀찜/해물찜', '오징어요리', '장어/먹장어요리', 
        '조개요리', '주꾸미요리', '해물/생선요리'
    ],
    '베이커리/디저트': [
        '도넛', '떡/한과', '떡카페', '방앗간', '베이커리', '빙수', '스마일명품찹쌀꽈배기', 
        '스마일찹쌀꽈배기', '와플', '케이크전문', '크레페', '호두과자', '호떡'
    ],
    '커피/음료': [
        '과일/주스전문점', '다방', '바나프레소', '아이스크림', '차', '카페', 
        '카페/디저트', '테이크아웃커피'
    ],
    '브런치/샌드위치': [
        '브런치', '브런치카페', '샌드위치'
    ],
    '다이어트/샐러드': [
        '다이어트/샐러드', '채식/샐러드뷔페'
    ],
    '주점': [
        '단란주점', '라이브카페', '맥주/호프', '민속주점', '바(BAR)', '술집', '와인', 
        '요리주점', '유흥주점', '이자카야', '전통/민속주점', '포장마차'
    ],
    '간편식': [
        '도시락/컵밥', '밀키트', '반찬가게'
    ],
    '기타': [
        '슈퍼/마트', '안경원', '야식', '음식점', '패밀리레스토랑', '푸드트럭', 
        '퓨전음식', '향토음식'
    ]
}


def get_db_connection():
    """Create and return a database connection"""
    try:
        conn = psycopg2.connect(**DB_CONFIG)
        conn.autocommit = False
        return conn
    except Exception as e:
        print(f"Error connecting to database: {e}")
        sys.exit(1)

def normalize_category(category: str) -> str:
    """
    Normalize category by replacing / with , for lookup
    (per specifications: "/" is interpreted as "," when looking up values)
    """
    if not category:
        return ""
    return category.replace('/', ',')

def get_all_valid_members() -> Set[str]:
    """
    Get all valid category members, normalizing / to ,
    """
    all_members = set()
    for category_group, members in VALID_CATEGORIES.items():
        for member in members:
            # Add both original and normalized versions
            all_members.add(member)
            all_members.add(normalize_category(member))
    return all_members

def find_normalized_category(category: str) -> Optional[str]:
    """
    Find the normalized category key from VALID_CATEGORIES for a given category.
    Returns the key (e.g., '한식', '일식') if category matches any member in the group.
    Returns None if no match is found.
    """
    if not category:
        return None
    
    normalized_category = normalize_category(category)
    
    for category_key, members in VALID_CATEGORIES.items():
        for member in members:
            if category == member or normalized_category == normalize_category(member):
                return category_key
    
    return None

def validate_restaurant_categories(cursor) -> None:
    """
    Check restaurant categories against valid members list and report invalid entries
    """
    print("\n" + "="*80)
    print("CATEGORY VALIDATION")
    print("="*80)
    
    # Get all restaurant categories
    query = """
    SELECT id, name, category
    FROM db_restaurants 
    WHERE category IS NOT NULL AND TRIM(category) <> ''
    ORDER BY category, name;
    """
    
    cursor.execute(query)
    restaurants = cursor.fetchall()
    
    valid_members = get_all_valid_members()
    invalid_entries = []
    
    print(f"Checking {len(restaurants)} restaurants...")
    print(f"Valid members count: {len(valid_members)}")
    print()
    
    for restaurant in restaurants:
        category = restaurant['category']
        normalized_category = normalize_category(category)
        
        # Check if category is in valid members (original or normalized)
        if category not in valid_members and normalized_category not in valid_members:
            invalid_entries.append({
                'id': restaurant['id'],
                'name': restaurant['name'],
                'category': category,
                'normalized': normalized_category
            })
    
    # Report results
    if invalid_entries:
        print(f"❌ Found {len(invalid_entries)} restaurants with INVALID categories:")
        print("="*80)
        print(f"{'Restaurant Name':<40} {'Category':<30} {'ID'}")
        print("-"*80)
        
        for entry in invalid_entries:
            print(f"{entry['name']:<40} {entry['category']:<30} {entry['id']}")
        
        print("="*80)
        print(f"Total invalid entries: {len(invalid_entries)}")
        
        # Show unique invalid categories
        unique_invalid = set(entry['category'] for entry in invalid_entries)
        print(f"\nUnique invalid categories ({len(unique_invalid)}):")
        for cat in sorted(unique_invalid):
            count = sum(1 for entry in invalid_entries if entry['category'] == cat)
            print(f"  - {cat} ({count} restaurants)")
            
    else:
        print("✅ All restaurant categories are VALID!")
    
    print("="*80)

def ensure_category_normalized_column(cursor) -> None:
    """
    Ensure category_normalized column exists in db_restaurants table
    """
    try:
        # Check if column exists
        check_query = """
        SELECT column_name 
        FROM information_schema.columns 
        WHERE table_name = 'db_restaurants' AND column_name = 'category_normalized';
        """
        cursor.execute(check_query)
        result = cursor.fetchone()
        
        if not result:
            print("Adding category_normalized column to db_restaurants table...")
            alter_query = """
            ALTER TABLE db_restaurants 
            ADD COLUMN category_normalized TEXT;
            """
            cursor.execute(alter_query)
            cursor.connection.commit()
            print("✅ Category_normalized column added successfully!")
        else:
            print("✅ Category_normalized column already exists")
            
    except Exception as e:
        print(f"❌ Error ensuring category_normalized column: {e}")
        raise

def update_restaurant_normalized_categories(cursor, batch_size: int = 1000) -> None:
    """
    Update restaurants with normalized category values
    """
    print("\n" + "="*80)
    print("CATEGORY NORMALIZATION")
    print("="*80)
    
    # Get all restaurants with categories
    query = """
    SELECT id, name, category
    FROM db_restaurants 
    WHERE category IS NOT NULL AND TRIM(category) <> ''
    ORDER BY id;
    """
    
    cursor.execute(query)
    restaurants = cursor.fetchall()
    
    print(f"Processing {len(restaurants)} restaurants for category normalization...")
    
    updated_count = 0
    null_count = 0
    
    # Process restaurants in batches with tqdm progress bar
    num_batches = (len(restaurants) + batch_size - 1) // batch_size
    
    with tqdm(total=num_batches, desc="Processing batches", unit="batch") as pbar:
        for i in range(0, len(restaurants), batch_size):
            batch = restaurants[i:i + batch_size]
            
            for restaurant in batch:
                restaurant_id = restaurant['id']
                category = restaurant['category']
                
                # Find normalized category
                normalized_category = find_normalized_category(category)
                
                # Update database
                update_query = """
                UPDATE db_restaurants 
                SET category_normalized = %s
                WHERE id = %s;
                """
                
                try:
                    cursor.execute(update_query, (normalized_category, restaurant_id))
                    if normalized_category:
                        updated_count += 1
                    else:
                        null_count += 1
                except Exception as e:
                    print(f"Error updating restaurant {restaurant_id}: {e}")
                    raise
            
            # Commit batch
            cursor.connection.commit()
            pbar.update(1)
    
    print(f"\n✅ Category normalization completed!")
    print(f"   - Updated with valid categories: {updated_count}")
    print(f"   - Set to NULL (invalid categories): {null_count}")
    print("="*80)

def clean_menu_name(menu_name: str) -> str:
    """
    Clean menu name using regex to:
    1. Uppercase all alphabets
    2. Remove price range patterns
    3. Remove quantity/unit patterns
    4. Remove size indicators
    5. Remove common keywords
    6. Remove set menu words
    7. Keep only alphabets, Korean characters, and blankspaces
    8. Replace multiple spaces with single space
    9. Trim leading/trailing whitespace
    """
    if not menu_name:
        return ""
    
    # Convert alphabets to uppercase
    cleaned = menu_name.upper()
    
    # Remove price range patterns (e.g., "7000-12000원", "5000~8000원")
    price_range_pattern = r'\d+\s*[-~]\s*\d+\s*[A-Z가-힣]+'
    cleaned = re.sub(price_range_pattern, '', cleaned)

    # Remove quantity/unit patterns (e.g., "2개", "500ml", "1인분", "1.5L", "30%")
    quantity_pattern = r'\d+(.\d+)*\s*[A-Z가-힣%]+'
    cleaned = re.sub(quantity_pattern, '', cleaned)
    
    # Remove size indicators
    size_pattern = r'(?=\b)((XL)|[RSML대중소]|(미니)|(mini))(?=\b|$)'
    cleaned = re.sub(size_pattern, '', cleaned, flags=re.IGNORECASE)
    
    # Remove common keywords
    common_pattern = r'(NEW|추가|옵션|선택|판매종료)'
    cleaned = re.sub(common_pattern, '', cleaned, flags=re.IGNORECASE)

    # Remove set menu words
    set_pattern = r'[A-Z]?\s*(세트|SET)'
    cleaned = re.sub(set_pattern, '', cleaned, flags=re.IGNORECASE)
    
    # Keep only alphabets, Korean characters, and blankspaces
    korean_alpha_pattern = r'[^가-힣A-Z\s]'
    cleaned = re.sub(korean_alpha_pattern, ' ', cleaned)
    
    # Replace double or more space with singlespace
    cleaned = re.sub(r'\s+', ' ', cleaned)
    
    # Clean up extra whitespace
    cleaned = ' '.join(cleaned.split())
    
    return cleaned.strip()


def update_menu_normalized_name(cursor, menu_id: str, name_clean: str):
    """Update menu with normalized name"""
    query = """
    UPDATE db_menus 
    SET name_clean = %s
    WHERE id = %s;
    """
    
    try:
        #print("Trying to update menu:", menu_id, "with name_clean:", name_clean)
        cursor.execute(query, (name_clean, menu_id))
        #print("Successfully updated menu:", menu_id)
    except Exception as e:
        #print(f"Error updating menu {menu_id}: {e}")
        raise

def update_embeddings_for_menu(cursor, menu_id: str, name_clean: str):
    """Update embedding vector for a menu based on clean name"""
    # This is a placeholder - you'll need to implement actual embedding generation
    # For now, we'll set it to NULL to indicate it needs regeneration
    query = """
    UPDATE db_menus 
    SET embedding_vector = NULL, updated_at = NOW()
    WHERE id = %s;
    """
    
    try:
        cursor.execute(query, (menu_id,))
    except Exception as e:
        print(f"Error updating embedding for menu {menu_id}: {e}")
        raise

def update_menu_names_only(cursor, batch_size: int = 1000):
    """Update only name_clean values for existing menus"""
    
    query = """
    SELECT id, name, name_clean
    FROM db_menus 
    WHERE name IS NOT NULL AND TRIM(name) != ''
    ORDER BY id;
    """
    
    cursor.execute(query)
    menus = cursor.fetchall()
    
    if not menus:
        print("No menus to process.")
        return
    
    print(f"Updating name_clean for {len(menus)} menus...")
    
    updated_count = 0
    unchanged_count = 0
    
    # Process menus in batches with tqdm progress bar
    num_batches = (len(menus) + batch_size - 1) // batch_size
    
    with tqdm(total=num_batches, desc="Updating names", unit="batch") as pbar:
        for i in range(0, len(menus), batch_size):
            batch = menus[i:i + batch_size]
            
            for menu in batch:
                menu_id = menu['id']
                menu_name = menu['name']
                current_name_clean = menu['name_clean']
                
                # Generate new clean name
                new_name_clean = clean_menu_name(menu_name)
                
                # Only update if changed
                if current_name_clean != new_name_clean:
                    #print(f"Updating menu ID {menu_id}: '{current_name_clean}' -> '{new_name_clean}'")
                    update_menu_normalized_name(cursor, menu_id, new_name_clean)
                    #print(f"Updated menu ID {menu_id}: '{new_name_clean}'")
                    updated_count += 1
                else:
                    #print(f"No change for menu ID {menu_id}: '{current_name_clean}' remains unchanged")
                    unchanged_count += 1
            
            # Commit batch
            cursor.connection.commit()
            pbar.update(1)
    
    print(f"✅ Name updates completed: {updated_count} updated, {unchanged_count} unchanged")

def update_menu_embeddings_only(cursor, batch_size: int = 1000):
    """Update only embedding_vector values for existing menus"""
    query = """
    SELECT id, name_clean
    FROM db_menus 
    WHERE name_clean IS NOT NULL AND TRIM(name_clean) != ''
    ORDER BY id;
    """
    
    cursor.execute(query)
    menus = cursor.fetchall()
    
    if not menus:
        print("No menus with clean names to process.")
        return
    
    print(f"Updating embeddings for {len(menus)} menus...")
    print("⚠️  Note: Setting embedding_vector to NULL - implement actual embedding generation")
    
    # Process menus in batches with tqdm progress bar
    num_batches = (len(menus) + batch_size - 1) // batch_size
    
    with tqdm(total=num_batches, desc="Updating embeddings", unit="batch") as pbar:
        for i in range(0, len(menus), batch_size):
            batch = menus[i:i + batch_size]
            
            for menu in batch:
                menu_id = menu['id']
                name_clean = menu['name_clean']
                
                # Update embedding (placeholder implementation)
                update_embeddings_for_menu(cursor, menu_id, name_clean)
            
            # Commit batch
            cursor.connection.commit()
            pbar.update(1)
    
    print(f"✅ Embedding updates completed for {len(menus)} menus")

def update_both_names_and_embeddings(cursor, batch_size: int = 1000):
    """Update both name_clean and embedding_vector values"""
    query = """
    SELECT id, name, name_clean
    FROM db_menus 
    WHERE name IS NOT NULL AND TRIM(name) != ''
    ORDER BY id;
    """
    
    cursor.execute(query)
    menus = cursor.fetchall()
    
    if not menus:
        print("No menus to process.")
        return
    
    print(f"Updating both names and embeddings for {len(menus)} menus...")
    
    updated_names = 0
    unchanged_names = 0
    
    # Process menus in batches with tqdm progress bar
    num_batches = (len(menus) + batch_size - 1) // batch_size
    
    with tqdm(total=num_batches, desc="Updating names & embeddings", unit="batch") as pbar:
        for i in range(0, len(menus), batch_size):
            batch = menus[i:i + batch_size]
            
            for menu in batch:
                menu_id = menu['id']
                menu_name = menu['name']
                current_name_clean = menu['name_clean']
                
                # Generate new clean name
                new_name_clean = clean_menu_name(menu_name)
                
                # Update name_clean if changed
                if current_name_clean != new_name_clean:
                    update_menu_normalized_name(cursor, menu_id, new_name_clean)
                    updated_names += 1
                else:
                    unchanged_names += 1
                
                # Always update embedding when requested
                update_embeddings_for_menu(cursor, menu_id, new_name_clean)
            
            # Commit batch
            cursor.connection.commit()
            pbar.update(1)
    
    print(f"✅ Updates completed: {updated_names} names updated, {unchanged_names} names unchanged")
    print(f"✅ Embeddings updated for all {len(menus)} menus")

def process_menus(cursor, batch_size: int = 1000):
    """Process menus to create normalized names (original function for new data)"""
    # Get all menus (overwrite existing normalized names)
    query = """
    SELECT id, name 
    FROM db_menus 
    WHERE name IS NOT NULL AND TRIM(name) != ''
    ORDER BY id;
    """
    
    cursor.execute(query)
    menus = cursor.fetchall()
    
    if not menus:
        print("No menus to process.")
        return
    
    print(f"Found {len(menus)} menus to process...")
    
    # Process menus in batches with tqdm progress bar
    num_batches = (len(menus) + batch_size - 1) // batch_size
    
    with tqdm(total=num_batches, desc="Processing batches", unit="batch") as pbar:
        for i in range(0, len(menus), batch_size):
            batch = menus[i:i + batch_size]
            
            for menu in batch:
                menu_id = menu['id']
                menu_name = menu['name']
                
                # Clean menu name
                name_clean = clean_menu_name(menu_name)
                
                # Update database
                update_menu_normalized_name(cursor, menu_id, name_clean)
            
            # Commit batch
            cursor.connection.commit()
            pbar.update(1)

def preview_name_changes(cursor, limit: int = 20):
    """Preview what changes would be made to name_clean values"""
    query = """
    SELECT id, name, name_clean
    FROM db_menus 
    WHERE name IS NOT NULL AND TRIM(name) != ''
    ORDER BY id
    LIMIT %s;
    """
    
    cursor.execute(query, (limit,))
    menus = cursor.fetchall()
    
    changes = []
    for menu in menus:
        current_clean = menu['name_clean'] or ''
        new_clean = clean_menu_name(menu['name'])
        
        if current_clean != new_clean:
            changes.append({
                'id': menu['id'],
                'name': menu['name'],
                'old_clean': current_clean,
                'new_clean': new_clean
            })
    
    print(f"Preview of changes (first {limit} menus):")
    print("="*80)
    
    if changes:
        print(f"Found {len(changes)} changes in sample:")
        for i, change in enumerate(changes[:10]):  # Show first 10 changes
            print(f"{i+1}. Menu: '{change['name']}'")
            print(f"   Old clean: '{change['old_clean']}'")
            print(f"   New clean: '{change['new_clean']}'")
            print()
    else:
        print("No changes found in sample.")
    
    return len(changes)

def main():
    """Main function to process menu data, validate and normalize restaurant categories"""
    parser = argparse.ArgumentParser(description='Process menu names and restaurant categories')
    parser.add_argument('--update-names', action='store_true', 
                       help='Update name_clean values only')
    parser.add_argument('--update-embeddings', action='store_true', 
                       help='Update embedding_vector values only')
    parser.add_argument('--update-all', action='store_true', 
                       help='Update both name_clean and embedding_vector values')
    parser.add_argument('--preview', action='store_true', 
                       help='Preview changes without updating')
    parser.add_argument('--force', action='store_true', 
                       help='Skip confirmation prompts')
    
    args = parser.parse_args()
    
    # Connect to database
    conn = get_db_connection()
    
    try:
        with conn.cursor(cursor_factory=RealDictCursor) as cursor:
            if args.preview:
                print("Previewing name_clean changes...")
                print("="*80)
                preview_name_changes(cursor, limit=50)
                
            elif args.update_names:
                print("Updating name_clean values only...")
                print("="*80)
                update_menu_names_only(cursor, batch_size=1000)
                
            elif args.update_embeddings:
                print("Updating embedding_vector values only...")
                print("="*80)
                update_menu_embeddings_only(cursor, batch_size=1000)
                
            elif args.update_all:
                print("Updating both name_clean and embedding_vector values...")
                print("="*80)
                update_both_names_and_embeddings(cursor, batch_size=1000)
                
            else:
                # Default behavior: full processing
                print("Starting full processing: menu name normalization, category validation, and category normalization...")
                
                # First: Validate restaurant categories
                validate_restaurant_categories(cursor)
                
                # Second: Ensure category_normalized column exists
                ensure_category_normalized_column(cursor)
                
                # Third: Update restaurants with normalized categories
                update_restaurant_normalized_categories(cursor, batch_size=1000)
                
                # Fourth: Process menus
                print("\n" + "="*80)
                print("MENU NAME NORMALIZATION")
                print("="*80)
                process_menus(cursor, batch_size=1000)
            
        print("\n✅ All processing completed successfully!")
        
    except Exception as e:
        print(f"❌ Processing failed: {e}")
        conn.rollback()
        sys.exit(1)
    finally:
        conn.close()

if __name__ == "__main__":
    main()