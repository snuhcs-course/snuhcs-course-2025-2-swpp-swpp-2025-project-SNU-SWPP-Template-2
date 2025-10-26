#!/usr/bin/env python3
"""
client.py - Restaurant recommendation system

This script provides restaurant and menu recommendations based on:
- User location (using PostGIS for spatial queries)
- User profile (cuisine preferences)
- Menu categorization using LangChain
"""

import os
import sys
import json
import time
from typing import Dict, List, Any, Optional, Tuple
import psycopg2
from psycopg2.extras import RealDictCursor
from dotenv import load_dotenv
from langchain_openai import OpenAI
from langchain_core.prompts import PromptTemplate
import math

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

# OpenAI API key
OPENAI_API_KEY = os.getenv('OPENAI_API_KEY')

# Category aliases mapping from specifications.md
CATEGORY_ALIASES = {
    'korean': '한식',
    'japanese': '일식', 
    'snackfood': '분식',
    'chinese': '중식',
    'western': '양식',
    'global': '세계음식',
    'fastfood': '패스트푸드',
    'meat': '육류/고기요리',
    'seafood': '해산물',
    'bakery/dessert': '베이커리/디저트',
    'coffee/beverage': '커피/음료',
    'brunch/sandwich': '브런치/샌드위치',
    'healthy/salad': '다이어트/샐러드',
    'bar/pub': '주점',
    'convenience': '간편식',
    'miscellaneous': '기타'
}

class UserProfile:
    """User profile with preferences"""
    def __init__(self, 
                 location: Tuple[float, float],  # (longitude, latitude)
                 location_info: str = None,  # Human-readable location description
                 cuisine_preferences: List[str] = None,
                 max_distance_km: float = None):  # Maximum search distance in km
        self.location = location
        self.location_info = location_info
        self.cuisine_preferences = cuisine_preferences or []
        self.max_distance_km = max_distance_km

class RestaurantRecommender:
    """Main recommendation system"""
    
    def __init__(self, model_name: str = "gpt-4o-mini"):
        self.conn = self._get_db_connection()
        self.llm = self._setup_langchain(model_name)
    
    def resolve_category_aliases(self, categories: List[str]) -> List[str]:
        """
        Resolve category aliases to Korean category names.
        Supports both English aliases and Korean names.
        
        Args:
            categories: List of category names (can be aliases or Korean names)
            
        Returns:
            List of resolved Korean category names
        """
        if not categories:
            return []
        
        resolved_categories = []
        for category in categories:
            # Convert to lowercase for case-insensitive matching
            category_lower = category.lower().strip()
            
            # Check if it's an alias
            if category_lower in CATEGORY_ALIASES:
                resolved_categories.append(CATEGORY_ALIASES[category_lower])
            else:
                # Assume it's already a Korean category name
                resolved_categories.append(category.strip())
        
        return resolved_categories
    
    def _salvage_partial_json(self, json_str: str, batch_start: int, batch_end: int) -> Dict:
        """
        Try to salvage a partial or malformed JSON response
        """
        try:
            # First, try to find and fix common JSON issues
            cleaned = json_str.strip()
            
            # Remove any trailing incomplete entries
            if cleaned.endswith(','):
                cleaned = cleaned[:-1]
            
            # Try to close incomplete JSON objects
            if not cleaned.endswith('}'):
                # Count opening and closing braces
                open_braces = cleaned.count('{')
                close_braces = cleaned.count('}')
                
                # Add missing closing braces
                if open_braces > close_braces:
                    cleaned += '}' * (open_braces - close_braces)
            
            # Try to parse the cleaned version
            try:
                return json.loads(cleaned)
            except json.JSONDecodeError:
                pass
            
            # Try to extract valid complete category entries using regex
            import re
            # Pattern to match complete category entries: "CategoryName": ["hex1", "hex2"]
            pattern = r'"([^"]+)":\s*\[([^\]]*)\]'
            matches = re.findall(pattern, json_str)
            
            if matches:
                salvaged = {}
                for category_name, indices_str in matches:
                    try:
                        # Parse the indices (handle both hex strings and decimal)
                        indices_str = indices_str.strip()
                        if indices_str:
                            # Split by comma and clean up each index
                            raw_indices = [x.strip().strip('"\'') for x in indices_str.split(',')]
                            valid_indices = []
                            
                            for raw_index in raw_indices:
                                if raw_index:
                                    try:
                                        # Try hex first, then decimal
                                        if raw_index.isdigit():
                                            decimal_index = int(raw_index)
                                        else:
                                            decimal_index = int(raw_index, 16)
                                        
                                        # Filter indices to be within the current batch range
                                        if batch_start <= decimal_index < batch_end:
                                            valid_indices.append(decimal_index)
                                    except ValueError:
                                        continue
                            
                            if valid_indices:
                                salvaged[category_name] = valid_indices
                    except (ValueError, AttributeError):
                        continue
                
                if salvaged:
                    print(f"Salvaged {len(salvaged)} categories from malformed JSON")
                    return salvaged
            
        except Exception as e:
            print(f"Error in JSON salvage: {e}")
        
        return {}
    
    def _get_db_connection(self):
        """Create and return a database connection"""
        try:
            conn = psycopg2.connect(**DB_CONFIG)
            return conn
        except Exception as e:
            print(f"Error connecting to database: {e}")
            sys.exit(1)
    
    def _setup_langchain(self, model_name: str = "gpt-4o-mini"):
        """Setup LangChain with OpenAI"""
        if not OPENAI_API_KEY:
            print("Warning: OPENAI_API_KEY not set. Menu categorization will be skipped.")
            return None
        
        return OpenAI(
            openai_api_key=OPENAI_API_KEY,
            model_name=model_name,
            temperature=0.3,
            max_tokens=500  # Reduced for faster responses
        )
    
    def find_nearby_restaurants(self, user_profile: UserProfile, 
                              max_distance_km: float = None,
                              categories: List[str] = None,
                              max_restaurants: int = 30) -> List[Dict]:
        """Find restaurants within range and matching categories
        
        Args:
            user_profile: User profile with location and preferences  
            max_distance_km: Override for maximum search radius (uses UserProfile.max_distance_km if None)
            categories: List of category names (supports aliases like 'korean', 'japanese', etc.)
            max_restaurants: Maximum number of restaurants to return (default 30, closest first)
        """
        
        # Use UserProfile's max_distance_km if no override provided
        search_distance = max_distance_km if max_distance_km is not None else user_profile.max_distance_km
        
        # Warning for unrestricted queries
        if search_distance is None and not categories:
            response = input("Warning: No distance or category restrictions. This may cause high latency. Continue? (y/n): ")
            if response.lower() != 'y':
                return []
        
        longitude, latitude = user_profile.location
        
        # Resolve category aliases to Korean names
        resolved_categories = self.resolve_category_aliases(categories) if categories else None
        
        with self.conn.cursor(cursor_factory=RealDictCursor) as cursor:
            # Build query dynamically
            where_conditions = []
            where_params = []
            
            # Distance filter
            if search_distance:
                where_conditions.append("ST_DWithin(geom::geography, ST_SetSRID(ST_Point(%s, %s), 4326)::geography, %s)")
                where_params.extend([longitude, latitude, search_distance * 1000])  # Convert km to meters
            
            # Category filter - use category_normalized column for better matching
            if resolved_categories:
                placeholders = ','.join(['%s'] * len(resolved_categories))
                where_conditions.append(f"category_normalized = ANY(ARRAY[{placeholders}])")
                where_params.extend(resolved_categories)
            
            where_clause = " AND ".join(where_conditions) if where_conditions else "1=1"
            
            # Combine all parameters: SELECT clause params + WHERE clause params + LIMIT param
            query_params = [longitude, latitude] + where_params + [max_restaurants]
            
            query = f"""
            SELECT *,
                   ST_Distance(geom::geography, ST_SetSRID(ST_Point(%s, %s), 4326)::geography) as distance_meters
            FROM db_restaurants
            WHERE {where_clause}
            ORDER BY distance_meters
            LIMIT %s;
            """
            
            # Time the database query execution
            query_start_time = time.time()
            cursor.execute(query, query_params)
            restaurants = cursor.fetchall()
            query_end_time = time.time()
            
            query_time = query_end_time - query_start_time
            print(f"   🔍 Restaurant database query: {query_time:.3f}s")
            
            return [dict(restaurant) for restaurant in restaurants]
    
    def get_restaurant_menus(self, restaurant_ids: List[str]) -> Dict[str, List[Dict]]:
        """Get menus for given restaurants"""
        if not restaurant_ids:
            return {}
        
        with self.conn.cursor(cursor_factory=RealDictCursor) as cursor:
            placeholders = ','.join(['%s::uuid'] * len(restaurant_ids))
            query = f"""
            SELECT m.*, r.name as restaurant_name
            FROM db_menus m
            JOIN db_restaurants r ON r.id = m.restaurant_id
            WHERE m.restaurant_id = ANY(ARRAY[{placeholders}])
            ORDER BY m.restaurant_id, m.index_in_rest;
            """
            
            cursor.execute(query, restaurant_ids)
            menus = cursor.fetchall()
            
            # Group by restaurant
            restaurant_menus = {}
            for menu in menus:
                restaurant_id = menu['restaurant_id']
                if restaurant_id not in restaurant_menus:
                    restaurant_menus[restaurant_id] = []
                restaurant_menus[restaurant_id].append(dict(menu))
            
            return restaurant_menus
    
    
    def categorize_menus(self, menus: List[Dict]) -> Dict[str, List[Dict]]:
        """Categorize menus using LangChain with improved batching and prompting"""
        if not self.llm or not menus:
            return {"General": menus}
        
        # Create menu dictionary with hexadecimal indices
        menu_items = []
        for i, menu in enumerate(menus):
            if menu.get('name'):
                hex_index = hex(i)[2:]  # Remove '0x' prefix
                menu_items.append(f"{hex_index}: {menu['name']}")
        
        if not menu_items:
            return {"General": menus}
        
        # Process in batches to handle more menus
        batch_size = 20  # Smaller batches for faster processing and better reliability
        all_categorized_menus = {}
        all_used_indices = set()
        
        # Track AI timing
        ai_total_time = 0.0
        successful_batches = 0
        failed_batches = 0
        
        for batch_start in range(0, len(menu_items), batch_size):
            batch_end = min(batch_start + batch_size, len(menu_items))
            batch_items = menu_items[batch_start:batch_end]
            
            # Create improved prompt for categorization
            prompt_template = """
            Categorize these Korean food menu items by their index numbers. Be comprehensive and categorize EVERY item.
            
            Menu items:
            {menu_list}
            
            Create meaningful categories for Korean foods. Consider: main ingredients, cooking methods, food types.
            Create no more than 10 categories, ordered from the best fit to the least fit, with "Miscellaneous" always last.
            The last category should be "Miscellaneous" for items that don't fit well in other categories.
            Use descriptive names including either main ingredients, cooking methods, food types, or their combination.
            Avoid vague categories like "Main Ingredients," "Cooking Methods," "Food Types," "Other," or "Others."
            Each category name must not overlap in meaning.
            
            Return ONLY a valid, complete JSON object. No extra text, no explanations.
            Format: {{"Rice Dishes": ["0", "3"], "Noodles & Soups": ["1", "5"], "Miscellaneous": ["2", "4"]}}
            Use hexadecimal indices (without 0x prefix) to save tokens.
            CRITICAL:
            1. Include ALL indices from {start_idx_hex} to {end_idx_hex}
            2. Use exactly 10 categories or fewer
            3. Use only valid JSON syntax with hex indices as strings
            4. End with a complete closing brace
            5. Do not truncate the response
            """
            
            try:
                prompt_template_obj = PromptTemplate(
                    input_variables=["menu_list", "start_idx_hex", "end_idx_hex"],
                    template=prompt_template
                )
                
                menu_list_str = "\n".join(batch_items)
                start_idx_hex = hex(batch_start)[2:]
                end_idx_hex = hex(batch_end-1)[2:]
                formatted_prompt = prompt_template_obj.format(
                    menu_list=menu_list_str,
                    start_idx_hex=start_idx_hex,
                    end_idx_hex=end_idx_hex
                )
                
                # Time the AI call
                ai_start_time = time.time()
                result = self.llm.invoke(formatted_prompt)
                ai_end_time = time.time()
                
                batch_ai_time = ai_end_time - ai_start_time
                ai_total_time += batch_ai_time
                
                # Parse result
                result_clean = result.strip()
                
                # Clean up markdown formatting
                if result_clean.startswith('```'):
                    result_clean = result_clean.replace('```json', '').replace('```', '').strip()
                
                # Try to extract JSON from the response if it's wrapped in text
                if not result_clean.startswith('{'):
                    # Look for JSON object in the response
                    start_idx = result_clean.find('{')
                    end_idx = result_clean.rfind('}') + 1
                    if start_idx != -1 and end_idx > start_idx:
                        result_clean = result_clean[start_idx:end_idx]
                
                # Try to parse JSON with robust error handling
                try:
                    categories = json.loads(result_clean)
                except json.JSONDecodeError as json_error:
                    print(f"JSON parsing error in batch {batch_start}-{batch_end}: {json_error}")
                    print(f"Raw response: {result[:200]}...")
                    
                    # Try to salvage partial JSON
                    categories = self._salvage_partial_json(result_clean, batch_start, batch_end)
                    
                    if not categories:
                        # If salvage failed, add this batch to "Other" category and continue
                        if "Other" not in all_categorized_menus:
                            all_categorized_menus["Other"] = []
                        for i in range(batch_start, batch_end):
                            if i < len(menus):
                                all_categorized_menus["Other"].append(menus[i])
                                all_used_indices.add(i)
                        continue
                
                # Map menus to categories using indices (convert hex to decimal)
                for category, menu_indices in categories.items():
                    if category not in all_categorized_menus:
                        all_categorized_menus[category] = []
                    
                    for index in menu_indices:
                        try:
                            # Convert hex string to decimal integer
                            if isinstance(index, str):
                                decimal_index = int(index, 16)
                            elif isinstance(index, int):
                                decimal_index = index
                            else:
                                continue
                                
                            if 0 <= decimal_index < len(menus):
                                all_categorized_menus[category].append(menus[decimal_index])
                                all_used_indices.add(decimal_index)
                        except (ValueError, TypeError):
                            continue
                
                successful_batches += 1
                
            except Exception as e:
                print(f"Error categorizing batch {batch_start}-{batch_end}: {e}")
                failed_batches += 1
                # Add this batch to "Other" category
                if "Other" not in all_categorized_menus:
                    all_categorized_menus["Other"] = []
                for i in range(batch_start, batch_end):
                    if i < len(menus):
                        all_categorized_menus["Other"].append(menus[i])
                        all_used_indices.add(i)
        
        # Add any remaining uncategorized menus to appropriate categories using simple rules
        uncategorized = [menus[i] for i in range(len(menus)) if i not in all_used_indices]
        if uncategorized:
            # Try to categorize remaining items using simple keyword matching
            for menu in uncategorized:
                menu_name = menu.get('name', '').lower()
                categorized = False
                
                # Simple keyword-based categorization for common Korean foods
                if any(word in menu_name for word in ['치킨', '닭', 'chicken']):
                    if "Chicken Dishes" not in all_categorized_menus:
                        all_categorized_menus["Chicken Dishes"] = []
                    all_categorized_menus["Chicken Dishes"].append(menu)
                    categorized = True
                elif any(word in menu_name for word in ['밥', '덮밥', '비빔밥', 'rice']):
                    if "Rice Dishes" not in all_categorized_menus:
                        all_categorized_menus["Rice Dishes"] = []
                    all_categorized_menus["Rice Dishes"].append(menu)
                    categorized = True
                elif any(word in menu_name for word in ['국수', '면', '라면', 'noodle']):
                    if "Noodles & Soups" not in all_categorized_menus:
                        all_categorized_menus["Noodles & Soups"] = []
                    all_categorized_menus["Noodles & Soups"].append(menu)
                    categorized = True
                elif any(word in menu_name for word in ['찌개', '탕', '국', 'soup', 'stew']):
                    if "Soups & Stews" not in all_categorized_menus:
                        all_categorized_menus["Soups & Stews"] = []
                    all_categorized_menus["Soups & Stews"].append(menu)
                    categorized = True
                elif any(word in menu_name for word in ['구이', '불고기', '삼겹살', 'grilled', 'bbq']):
                    if "Grilled Dishes" not in all_categorized_menus:
                        all_categorized_menus["Grilled Dishes"] = []
                    all_categorized_menus["Grilled Dishes"].append(menu)
                    categorized = True
                
                # If still not categorized, add to "Other"
                if not categorized:
                    if "Other" not in all_categorized_menus:
                        all_categorized_menus["Other"] = []
                    all_categorized_menus["Other"].append(menu)
        
        # Clean up empty categories
        all_categorized_menus = {k: v for k, v in all_categorized_menus.items() if v}
        
        # Ensure maximum 10 categories - consolidate excess into Miscellaneous
        if len(all_categorized_menus) > 10:
            # Sort categories by number of items (descending)
            sorted_categories = sorted(all_categorized_menus.items(), key=lambda x: len(x[1]), reverse=True)
            
            # Keep top 9 categories and move the rest to Miscellaneous
            final_categories = {}
            miscellaneous_menus = []
            
            for i, (category, menus) in enumerate(sorted_categories):
                if i < 9:
                    final_categories[category] = menus
                else:
                    miscellaneous_menus.extend(menus)
            
            # Add Miscellaneous category if there are items for it
            if miscellaneous_menus:
                final_categories["Miscellaneous"] = miscellaneous_menus
            
            all_categorized_menus = final_categories
        
        # Print AI timing summary
        total_batches = successful_batches + failed_batches
        if total_batches > 0 and len(menus) > 0:
            print(f"   🤖 AI Summary: {successful_batches}/{total_batches} batches successful")
        
        return all_categorized_menus if all_categorized_menus else {"General": menus}
    
    def generate_recommendations(self, user_profile: UserProfile,
                               max_distance_km: float = None,
                               categories: List[str] = None,
                               max_menus_to_categorize: int = 30,
                               max_menus_per_category: int = 3) -> Dict[str, Any]:
        """Generate complete recommendations
        
        Args:
            user_profile: User preferences and location
            max_distance_km: Override for maximum search radius (uses UserProfile.max_distance_km if None, defaults to 2.0)
            categories: Restaurant categories to filter by
            max_menus_to_categorize: Maximum number of menus to send to AI for categorization
            max_menus_per_category: Maximum number of menus to display per category
        """
        
        # Use UserProfile's max_distance_km if no override provided, otherwise default to 2.0
        search_distance = max_distance_km if max_distance_km is not None else (user_profile.max_distance_km or 2.0)
        
        # Start total timing
        total_start_time = time.time()
        timing_info = {}
        
        # Find nearby restaurants
        print("Finding nearby restaurants...")
        restaurant_start_time = time.time()
        restaurants = self.find_nearby_restaurants(user_profile, search_distance, categories)
        restaurant_end_time = time.time()
        timing_info['restaurant_search_time'] = restaurant_end_time - restaurant_start_time
        
        if not restaurants:
            return {"message": "No restaurants found matching criteria"}
        
        print(f"Found {len(restaurants)} restaurants (took {timing_info['restaurant_search_time']:.2f}s)")
        
        # Get menus for these restaurants
        menu_fetch_start_time = time.time()
        restaurant_ids = [r['id'] for r in restaurants]
        restaurant_menus = self.get_restaurant_menus(restaurant_ids)
        menu_fetch_end_time = time.time()
        timing_info['menu_fetch_time'] = menu_fetch_end_time - menu_fetch_start_time
        
        # Collect all menus
        all_menus = []
        for menus in restaurant_menus.values():
            all_menus.extend(menus)
        
        print(f"Found {len(all_menus)} total menus (took {timing_info['menu_fetch_time']:.2f}s)")
        
        # Categorize menus using LangChain
        print("Categorizing menus...")
        categorization_start_time = time.time()
        menus_to_categorize = min(len(all_menus), max_menus_to_categorize)
        print(f"Processing {menus_to_categorize} menus for categorization (limit: {max_menus_to_categorize})")
        categorized_menus = self.categorize_menus(all_menus[:menus_to_categorize])
        categorization_end_time = time.time()
        timing_info['categorization_time'] = categorization_end_time - categorization_start_time
        
        print(f"Menu categorization completed (took {timing_info['categorization_time']:.2f}s)")
        
        # Generate final recommendations
        recommendation_build_start_time = time.time()
        recommendations = {}
        for category, menus in categorized_menus.items():
            if not menus:
                continue
            
            # Get top menus in category (sorted by price as a simple metric)
            menus_available = len(menus)
            menus_to_show = min(menus_available, max_menus_per_category)
            top_menus = sorted(menus, key=lambda x: x.get('price', 0) or 0)[:menus_to_show]
            
            # Generate reason
            reason = self._generate_category_reason(category, user_profile)
            
            recommendations[category] = {
                "reason": reason,
                "menus": [
                    {
                        "name": menu['name'],
                        "restaurant": menu['restaurant_name'],
                        "price": menu['price']
                    }
                    for menu in top_menus
                ]
            }
        
        recommendation_build_end_time = time.time()
        timing_info['recommendation_build_time'] = recommendation_build_end_time - recommendation_build_start_time
        
        # Calculate total time
        total_end_time = time.time()
        timing_info['total_time'] = total_end_time - total_start_time
        
        # Print timing summary
        print(f"\n⏱️  PERFORMANCE SUMMARY:")
        print(f"   🔍 Restaurant search: {timing_info['restaurant_search_time']:.2f}s")
        print(f"   📋 Menu fetch: {timing_info['menu_fetch_time']:.2f}s")
        print(f"   🤖 AI categorization: {timing_info['categorization_time']:.2f}s")
        print(f"   📊 Recommendation build: {timing_info['recommendation_build_time']:.2f}s")
        print(f"   ⏰ Total time: {timing_info['total_time']:.2f}s")
        
        return {
            "user_location": user_profile.location,
            "search_radius_km": search_distance,
            "total_restaurants": len(restaurants),
            "total_menus_found": len(all_menus),
            "recommendations": recommendations,
            "timing_info": timing_info
        }
    
    def _generate_category_reason(self, category: str, user_profile: UserProfile) -> str:
        """Generate explanation for category recommendation"""
        reasons = []
        
        if user_profile.cuisine_preferences:
            matching_cuisines = [c for c in user_profile.cuisine_preferences if c.lower() in category.lower()]
            if matching_cuisines:
                reasons.append(f"matches your {', '.join(matching_cuisines)} preference")
        
        if not reasons:
            return f"Popular {category.lower()} items in your area"
        
        return f"Recommended because it {' and '.join(reasons)}"
    
    def close(self):
        """Close database connection"""
        if self.conn:
            self.conn.close()