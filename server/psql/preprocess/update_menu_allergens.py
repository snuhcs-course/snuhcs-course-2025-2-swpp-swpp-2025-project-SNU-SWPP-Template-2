#!/usr/bin/env python
import os
import json
import django
from functools import reduce
from tqdm import tqdm

# Setup Django - need to go up two directories to find config
import sys
sys.path.append(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'config.settings')
django.setup()

from psql_data.models import DbMenu
from django.db import transaction

def load_foodlist_data():
    """Load the foodlist.json file"""
    foodlist_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), 'raw', 'foodlist.json')
    with open(foodlist_path, 'r', encoding='utf-8') as f:
        return json.load(f)

def remove_spaces(text):
    """Remove all spaces from text"""
    return text.replace(' ', '') if text else ''

def normalize_text(text):
    """Apply preprocessing normalization rules to match ai_analysis keys"""
    if not text:
        return ''
    
    # Apply normalization rules
    text = text.replace('돈카츠', '돈가스').replace('돈까스', '돈가스').replace('카츠', '돈가스')
    text = text.replace('아메리카노', '커피')
    text = text.replace('버거', '햄버거')
    text = text.replace('밀크', '우유')
    text = text.replace('짜장', '짜장면')
    text = text.replace('마라탕', '마라')
    text = text.replace('스시', '초밥')
    
    return text

def find_matching_keys(menu, ai_analysis):
    """Find all keys in ai_analysis that are substrings of name_clean, description, or restaurant name (normalized and spaces removed)"""
    search_texts = []
    
    # Add name_clean if available
    if menu.name_clean:
        search_texts.append(remove_spaces(normalize_text(menu.name_clean)))
    
    # Add description if available
    if menu.description:
        search_texts.append(remove_spaces(normalize_text(menu.description)))
    
    # Add restaurant name if available
    if menu.restaurant and menu.restaurant.name:
        search_texts.append(remove_spaces(normalize_text(menu.restaurant.name)))
    
    if not search_texts:
        return []
    
    matching_keys = []
    for key in ai_analysis.keys():
        # Check if key is in any of the search texts
        for search_text in search_texts:
            if key in search_text:
                matching_keys.append(key)
                break  # Found in one text, no need to check others
    
    return matching_keys

def calculate_allergen_union(matching_keys, ai_analysis):
    """Calculate union of allergens from all matching keys (lowercase, no duplicates)"""
    all_allergens = set()
    
    for key in matching_keys:
        if key in ai_analysis and 'allergens' in ai_analysis[key]:
            allergens = ai_analysis[key]['allergens']
            if allergens:
                # Convert to lowercase and add to set (automatically removes duplicates)
                normalized_allergens = {allergen.lower() for allergen in allergens if allergen}
                all_allergens.update(normalized_allergens)
    
    # Return sorted list for consistent ordering
    return sorted(list(all_allergens))

def calculate_taste_profile_probability_or(matching_keys, ai_analysis):
    """Calculate taste profile using probability-or: T = 1 - product(1 - t)"""
    if not matching_keys:
        return None
    
    # Initialize taste values
    sweet_values = []
    salty_values = []
    spicy_values = []
    
    # Collect taste values from matching keys
    for key in matching_keys:
        if key in ai_analysis and 'taste_profile' in ai_analysis[key]:
            taste_profile = ai_analysis[key]['taste_profile']
            if taste_profile:
                sweet_values.append(taste_profile.get('sweet', 0))
                salty_values.append(taste_profile.get('salty', 0))
                spicy_values.append(taste_profile.get('spicy', 0))
    
    # Calculate probability-or for each taste
    def prob_or(values):
        if not values:
            return 0
        # T = 1 - product(1 - t)
        product = reduce(lambda x, y: x * (1 - y), values, 1)
        return 1 - product
    
    return {
        'sweet': round(prob_or(sweet_values), 3),
        'salty': round(prob_or(salty_values), 3),
        'spicy': round(prob_or(spicy_values), 3)
    }

def process_batch(menu_batch, ai_analysis):
    """Process a batch of menus and return updates for taste_profile and allergen_info only"""
    updates = []
    
    for menu in menu_batch:
        # Find matching keys
        matching_keys = find_matching_keys(menu, ai_analysis)
        
        # Calculate allergen info and taste profile
        allergen_info = calculate_allergen_union(matching_keys, ai_analysis) if matching_keys else None
        taste_profile = calculate_taste_profile_probability_or(matching_keys, ai_analysis) if matching_keys else None
        
        # Store the menu object and computed data (no recommend logic here)
        updates.append({
            'menu': menu,
            'allergen_info': allergen_info,
            'taste_profile': taste_profile
        })
    
    return updates

def update_menu_data():
    """Update all menus with allergen_info and taste_profile using batched processing"""
    print("Loading foodlist data...")
    foodlist_data = load_foodlist_data()
    ai_analysis = foodlist_data.get('ai_analysis', {})
    
    print(f"Found {len(ai_analysis)} items in ai_analysis")
    
    # Get all menus
    total_menus = DbMenu.objects.count()
    print(f"Found {total_menus} menus to process")
    
    batch_size = 1000
    updated_count = 0
    skipped_count = 0
    
    # Process in batches with progress bar
    with tqdm(total=total_menus, desc="Processing menus") as pbar:
        for offset in range(0, total_menus, batch_size):
            # Get batch of menus with restaurant data prefetched
            menu_batch = list(DbMenu.objects.select_related('restaurant').all()[offset:offset + batch_size])
            
            # Process the batch
            updates = process_batch(menu_batch, ai_analysis)
            
            # Apply updates in a transaction
            with transaction.atomic():
                for update in updates:
                    menu = update['menu']
                    menu.allergen_info = update['allergen_info']
                    menu.taste_profile = update['taste_profile']
                    menu.save(update_fields=['allergen_info', 'taste_profile'])
                    updated_count += 1
            
            # Count menus with no matches in this batch
            no_match_count = sum(1 for update in updates if not (update['taste_profile'] or update['allergen_info']))
            skipped_count += no_match_count
            
            # Update progress bar
            pbar.update(len(menu_batch))
            pbar.set_postfix({
                'Updated': updated_count,
                'Skipped': skipped_count
            })
    
    print(f"\nUpdate complete!")
    print(f"Updated: {updated_count} menus")
    print(f"No matches found: {skipped_count} menus")

def update_recommend_values():
    """Update recommend values based on final database state"""
    print("\nUpdating recommend values based on final database state...")
    
    # Get all menus
    total_menus = DbMenu.objects.count()
    print(f"Processing {total_menus} menus for recommend values")
    
    batch_size = 1000
    recommend_true_count = 0
    recommend_false_count = 0
    
    # Process in batches
    with tqdm(total=total_menus, desc="Setting recommend values") as pbar:
        for offset in range(0, total_menus, batch_size):
            # Get batch of menus
            menu_batch = list(DbMenu.objects.all()[offset:offset + batch_size])
            
            # Process updates in a transaction
            with transaction.atomic():
                for menu in menu_batch:
                    # Check if menu has meaningful data
                    has_meaningful_taste = (
                        menu.taste_profile is not None and 
                        any(value > 0 for value in menu.taste_profile.values())
                    )
                    has_meaningful_allergens = (
                        menu.allergen_info is not None and 
                        len(menu.allergen_info) > 0
                    )
                    
                    # Set recommend: False if and only if both taste_profile and allergen_info are nothing
                    # (taste_profile with all 0.0 values also counts as nothing)
                    should_recommend = has_meaningful_taste or has_meaningful_allergens
                    
                    if menu.recommend != should_recommend:
                        menu.recommend = should_recommend
                        menu.save(update_fields=['recommend'])
                    
                    if should_recommend:
                        recommend_true_count += 1
                    else:
                        recommend_false_count += 1
            
            # Update progress bar
            pbar.update(len(menu_batch))
            pbar.set_postfix({
                'Recommend True': recommend_true_count,
                'Recommend False': recommend_false_count
            })
    
    print(f"\nRecommend update complete!")
    print(f"Recommend True: {recommend_true_count} menus")
    print(f"Recommend False: {recommend_false_count} menus")
    print(f"Recommendation rate: {(recommend_true_count/total_menus)*100:.1f}%")

if __name__ == '__main__':
    # First update taste_profile and allergen_info
    update_menu_data()
    
    # Then update recommend values based on final database state
    update_recommend_values()