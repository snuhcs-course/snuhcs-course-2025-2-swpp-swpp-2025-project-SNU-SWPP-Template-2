#!/usr/bin/env python3
"""
recommend.py - Restaurant recommendation system demonstration

This script demonstrates the recommendation system with output formatted
according to specifications.md requirements.
"""

import os
import sys
import json
import argparse
from datetime import datetime
from client import RestaurantRecommender, UserProfile

def generate_recommendations_with_embedding(recommender, user_profile, clustering_method, max_menus_to_categorize, max_menus_per_category):
    """Generate recommendations using embedding-based categorization"""
    import time
    
    # Start total timing
    total_start_time = time.time()
    timing_info = {}
    
    # Use UserProfile's max_distance_km if available, otherwise default to 2.0
    search_distance = user_profile.max_distance_km or 2.0
    
    # Find nearby restaurants
    print("Finding nearby restaurants...")
    restaurant_start_time = time.time()
    restaurants = recommender.find_nearby_restaurants(user_profile, search_distance, user_profile.cuisine_preferences)
    restaurant_end_time = time.time()
    timing_info['restaurant_search_time'] = restaurant_end_time - restaurant_start_time
    
    if not restaurants:
        return {"message": "No restaurants found matching criteria"}
    
    print(f"Found {len(restaurants)} restaurants (took {timing_info['restaurant_search_time']:.2f}s)")
    
    # Get menus for these restaurants
    menu_fetch_start_time = time.time()
    restaurant_ids = [r['id'] for r in restaurants]
    restaurant_menus = recommender.get_restaurant_menus(restaurant_ids)
    menu_fetch_end_time = time.time()
    timing_info['menu_fetch_time'] = menu_fetch_end_time - menu_fetch_start_time
    
    # Collect all menus
    all_menus = []
    for menus in restaurant_menus.values():
        all_menus.extend(menus)
    
    print(f"Found {len(all_menus)} total menus (took {timing_info['menu_fetch_time']:.2f}s)")
    
    # Categorize menus using embedding method
    print("Categorizing menus using embedding method...")
    categorization_start_time = time.time()
    menus_to_categorize = min(len(all_menus), max_menus_to_categorize)
    print(f"Processing {menus_to_categorize} menus for categorization (limit: {max_menus_to_categorize})")
    categorized_menus = recommender.categorize_menus_embedding(
        all_menus[:menus_to_categorize], 
        user_profile, 
        clustering_method
    )
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
        reason = recommender._generate_category_reason(category, user_profile)
        
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
    print(f"   🧠 Embedding categorization: {timing_info['categorization_time']:.2f}s")
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
    parser.add_argument("--method", choices=["langchain", "embedding"], 
                       default="langchain", help="Categorization method to use")
    parser.add_argument("--clustering", choices=["hdbscan", "spectral", "kmeans"], 
                       default="hdbscan", help="Clustering method for embedding approach")
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
        print(f"   🧠 Categorization method: {args.method}")
        if args.method == "embedding":
            print(f"   🔧 Clustering method: {args.clustering}")
        
        # For embedding method, we need to modify the workflow
        if args.method == "embedding":
            recommendations = generate_recommendations_with_embedding(
                recommender, user, args.clustering, max_categorize, max_per_category
            )
        else:
            recommendations = recommender.generate_recommendations(
                user_profile=user,
                max_distance_km=user.max_distance_km,
                categories=user.cuisine_preferences,
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
        print("  python recommend.py                                     # Use default.json with embedding")
        print("  python recommend.py user1                               # Use user1.json with embedding")
        print("  python recommend.py --method langchain                  # Use langchain categorization")
        print("  python recommend.py user1 --clustering hdbscan    # Use HDBScan clustering")
        print("  python recommend.py user1 --clustering kmeans     # Use KMeans clustering")
        print("\nCategorization methods:")
        print("  --method langchain   # Traditional LangChain AI categorization (default)")
        print("  --method embedding   # New embedding-based clustering")
        print("\nClustering options (for embedding method):")
        print("  --clustering spectral  # Spectral clustering (default)")
        print("  --clustering hdbscan   # Density-based clustering")
        print("  --clustering kmeans    # K-Means clustering")
        print("\nUser profiles should be in test_run/user/ directory")
        print("Results will be saved in test_run/result/ directory")
    
    try:
        main()
    except KeyboardInterrupt:
        print("\n\n⏹️  Demo interrupted by user")
    except Exception as e:
        print(f"\n❌ Demo failed: {e}")
        sys.exit(1)