#!/usr/bin/env python3
"""
sample_run.py - Sample demonstration of the restaurant recommendation system

This script demonstrates the recommendation system with output formatted
according to specifications.md requirements.
"""

import os
import sys
import json
import argparse
from datetime import datetime
from client import RestaurantRecommender, UserProfile

def print_recommendations(recommendations, user_profile):
    """Print recommendations in the format specified in specifications.md"""
    
    print("=" * 60)
    print("🍽️  RESTAURANT RECOMMENDATION RESULTS")
    print("=" * 60)
    
    # Print user information
    longitude, latitude = user_profile.location
    print(f"📍 User Location: ({longitude:.4f}, {latitude:.4f})")
    if user_profile.cuisine_preferences:
        print(f"🍜 Cuisine Preferences: {', '.join(user_profile.cuisine_preferences)}")
    
    # Print search summary
    if "total_restaurants" in recommendations:
        print(f"🏪 Total Restaurants Found: {recommendations['total_restaurants']}")
        print(f"📋 Total Menus Found: {recommendations['total_menus_found']}")
        print(f"🔍 Search Radius: {recommendations['search_radius_km']} km")
    
    print("\n" + "=" * 60)
    print("📊 MENU RECOMMENDATIONS BY CATEGORY")
    print("=" * 60)
    
    # Check if we have recommendations
    if "message" in recommendations:
        print(f"❌ {recommendations['message']}")
        return
    
    if not recommendations.get('recommendations'):
        print("❌ No menu recommendations available")
        return
    
    # Print discovered categories first
    recs = recommendations['recommendations']
    category_names = list(recs.keys())
    print(f"\n📂 Discovered Menu Categories ({len(category_names)}):")
    for i, category in enumerate(category_names, 1):
        menu_count = len(recs[category]['menus'])
        print(f"   {i}. {category} ({menu_count} menus)")
    
    # Print each category according to specifications format
    for category, rec_data in recs.items():
        print(f"\n🍽️  {category}")
        print(f"   💡 {rec_data['reason']}")
        print(f"   📋 Recommended menus:")
        
        if not rec_data['menus']:
            print(f"      (No menus available in this category)")
            continue
            
        for i, menu in enumerate(rec_data['menus'], 1):
            menu_name = menu.get('name', 'Unknown Menu')
            restaurant_name = menu.get('restaurant', 'Unknown Restaurant')
            price = menu.get('price')
            
            if price:
                print(f"      {i}. {menu_name} ({restaurant_name}) - {price:,}원")
            else:
                print(f"      {i}. {menu_name} ({restaurant_name})")
    
    print("\n" + "=" * 60)

def load_user_profile(user_file: str) -> UserProfile:
    """Load user profile from JSON file"""
    user_path = f"test_run/user/{user_file}.json"
    
    if not os.path.exists(user_path):
        raise FileNotFoundError(f"User profile file not found: {user_path}")
    
    with open(user_path, 'r', encoding='utf-8') as f:
        user_data = json.load(f)
    
    return UserProfile(
        location_info=user_data.get("location_info"),
        location=tuple(user_data["location"]),
        cuisine_preferences=user_data.get("cuisine_preferences", []),
        max_distance_km=user_data.get("max_distance_km")
    )

def save_results(recommendations: dict, user_profile: UserProfile, user_file: str):
    """Save recommendation results to JSON file"""
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    result_filename = f"{user_file}_{timestamp}.json"
    result_path = f"test_run/result/{result_filename}"
    
    # Prepare data for JSON serialization
    result_data = {
        "timestamp": timestamp,
        "user_profile": {
            "location_info": user_profile.location_info,
            "location": user_profile.location,
            "cuisine_preferences": user_profile.cuisine_preferences,
            "max_distance_km": user_profile.max_distance_km
        },
        "recommendations": recommendations
    }
    
    # Create result directory if it doesn't exist
    os.makedirs("test_run/result", exist_ok=True)
    
    with open(result_path, 'w', encoding='utf-8') as f:
        json.dump(result_data, f, indent=2, ensure_ascii=False)
    
    print(f"📁 Results saved to: {result_path}")
    return result_path

def main():
    """Main demonstration function"""
    # Parse command line arguments
    parser = argparse.ArgumentParser(description="Restaurant Recommendation System Demo")
    parser.add_argument("user_file", nargs="?", default="default", 
                       help="User profile file name (without .json extension)")
    args = parser.parse_args()
    
    print("Starting Restaurant Recommendation System Demo...")
    print("=" * 60)
    
    # Load user profile from JSON
    try:
        user = load_user_profile(args.user_file)
        print(f"✅ Loaded user profile: {args.user_file}.json")
    except Exception as e:
        print(f"❌ Failed to load user profile: {e}")
        return
    
    # Initialize recommender
    try:
        recommender = RestaurantRecommender()
    except Exception as e:
        print(f"❌ Failed to initialize recommender: {e}")
        return
    
    print(f"👤 User Profile:")
    print(f"   📍 Location: {user.location_info} ({user.location[0]:.4f}, {user.location[1]:.4f})")
    print(f"   🍜 Preferences: {', '.join(user.cuisine_preferences)}")
    print(f"   📏 Max Distance: {user.max_distance_km}km")
    
    try:
        # Generate recommendations with custom parameters
        print(f"\n🔍 Generating recommendations...")
        max_categorize = 500
        max_per_category = 50
        print(f"   📊 Max menus to categorize: {max_categorize}")
        print(f"   📋 Max menus per category: {max_per_category}")
        print(f"   🏷️  Filtering by cuisine preferences: {', '.join(user.cuisine_preferences)}")
        recommendations = recommender.generate_recommendations(
            user_profile=user,
            max_distance_km=user.max_distance_km,
            categories=user.cuisine_preferences,  # Use cuisine preferences as category filter
            max_menus_to_categorize=max_categorize,
            max_menus_per_category=max_per_category
        )
        
        # Print formatted results according to specifications
        print_recommendations(recommendations, user)
        
        # Save results to JSON file
        save_results(recommendations, user, args.user_file)
        
    except Exception as e:
        print(f"❌ Error generating recommendations: {e}")
    finally:
        # Close recommender
        recommender.close()
        print("\n✅ Demo completed!")

if __name__ == "__main__":
    # Check if we're in the right directory
    if not os.path.exists('client.py'):
        print("❌ Please run this script from the psql/ directory")
        sys.exit(1)
    
    # Check environment
    if not os.getenv('OPENAI_API_KEY'):
        print("⚠️  Warning: OPENAI_API_KEY not set. Menu categorization will use fallback.")
    
    # Show usage if no args and help needed
    if len(sys.argv) > 1 and sys.argv[1] in ['-h', '--help']:
        print("\nUsage examples:")
        print("  python sample_run.py                # Use default.json")
        print("  python sample_run.py user1          # Use user1.json")
        print("  python sample_run.py my_profile     # Use my_profile.json")
        print("\nUser profiles should be in test_run/user/ directory")
        print("Results will be saved in test_run/result/ directory")
    
    try:
        main()
    except KeyboardInterrupt:
        print("\n\n⏹️  Demo interrupted by user")
    except Exception as e:
        print(f"\n❌ Demo failed: {e}")
        sys.exit(1)