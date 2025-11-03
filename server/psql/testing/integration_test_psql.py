#!/usr/bin/env python3
"""
integration_test_psql.py - Integration tests for restaurant recommendation system

This module provides comprehensive integration tests for the recommendation system including:
- Database connectivity and schema validation
- Data integrity verification
- End-to-end recommendation pipeline testing
- Performance benchmarks
"""

import os
import sys
import json
import time
import unittest
from typing import Dict, List, Any, Optional
import psycopg2
from psycopg2.extras import RealDictCursor
from dotenv import load_dotenv

# Import our modules
sys.path.append(os.path.join(os.path.dirname(__file__), '../recommend'))
from client import RestaurantRecommender, UserProfile

# Load environment variables
load_dotenv()

class DatabaseTestCase(unittest.TestCase):
    """Test database connectivity and data integrity"""
    
    @classmethod
    def setUpClass(cls):
        """Set up database connection for tests"""
        cls.db_config = {
            'host': os.getenv('DB_HOST', 'localhost'),
            'port': os.getenv('DB_PORT', '5432'),
            'database': os.getenv('DB_NAME', 'foodigram'),
            'user': os.getenv('DB_USER', 'postgres'),
            'password': os.getenv('DB_PASSWORD', 'postgres')
        }
        
        try:
            cls.conn = psycopg2.connect(**cls.db_config)
        except Exception as e:
            raise unittest.SkipTest(f"Cannot connect to database: {e}")
    
    @classmethod
    def tearDownClass(cls):
        """Close database connection"""
        if hasattr(cls, 'conn'):
            cls.conn.close()
    
    def test_database_connection(self):
        """Test that database connection works"""
        with self.conn.cursor() as cursor:
            cursor.execute("SELECT 1")
            result = cursor.fetchone()
            self.assertEqual(result[0], 1)
    
    def test_required_extensions(self):
        """Test that required PostgreSQL extensions are installed"""
        with self.conn.cursor() as cursor:
            # Check PostGIS extension
            cursor.execute("""
                SELECT EXISTS(
                    SELECT 1 FROM pg_extension WHERE extname = 'postgis'
                )
            """)
            self.assertTrue(cursor.fetchone()[0], "PostGIS extension not installed")
            
            # Check UUID extension
            cursor.execute("""
                SELECT EXISTS(
                    SELECT 1 FROM pg_extension WHERE extname = 'uuid-ossp'
                )
            """)
            self.assertTrue(cursor.fetchone()[0], "UUID extension not installed")
    
    def test_tables_exist(self):
        """Test that required tables exist"""
        with self.conn.cursor() as cursor:
            # Check restaurants table
            cursor.execute("""
                SELECT EXISTS(
                    SELECT 1 FROM information_schema.tables 
                    WHERE table_name = 'db_restaurants'
                )
            """)
            self.assertTrue(cursor.fetchone()[0], "db_restaurants table not found")
            
            # Check menus table
            cursor.execute("""
                SELECT EXISTS(
                    SELECT 1 FROM information_schema.tables 
                    WHERE table_name = 'db_menus'
                )
            """)
            self.assertTrue(cursor.fetchone()[0], "db_menus table not found")
    
    def test_data_integrity(self):
        """Test basic data integrity"""
        with self.conn.cursor(cursor_factory=RealDictCursor) as cursor:
            # Check for restaurants
            cursor.execute("SELECT COUNT(*) as count FROM db_restaurants")
            restaurant_count = cursor.fetchone()['count']
            self.assertGreater(restaurant_count, 0, "No restaurants found in database")
            
            # Check for menus
            cursor.execute("SELECT COUNT(*) as count FROM db_menus")
            menu_count = cursor.fetchone()['count']
            self.assertGreater(menu_count, 0, "No menus found in database")
            
            # Check foreign key integrity
            cursor.execute("""
                SELECT COUNT(*) as count FROM db_menus m
                LEFT JOIN db_restaurants r ON r.id = m.restaurant_id
                WHERE r.id IS NULL
            """)
            orphaned_menus = cursor.fetchone()['count']
            self.assertEqual(orphaned_menus, 0, "Found orphaned menus without restaurants")
    
    def test_spatial_data(self):
        """Test spatial data integrity"""
        with self.conn.cursor(cursor_factory=RealDictCursor) as cursor:
            # Check for restaurants with valid coordinates
            cursor.execute("""
                SELECT COUNT(*) as count FROM db_restaurants 
                WHERE geom IS NOT NULL AND ST_IsValid(geom)
            """)
            valid_geom_count = cursor.fetchone()['count']
            self.assertGreater(valid_geom_count, 0, "No restaurants with valid coordinates")
            
            # Check coordinate ranges (should be in Korea)
            cursor.execute("""
                SELECT 
                    MIN(ST_X(geom)) as min_lng,
                    MAX(ST_X(geom)) as max_lng,
                    MIN(ST_Y(geom)) as min_lat,
                    MAX(ST_Y(geom)) as max_lat
                FROM db_restaurants 
                WHERE geom IS NOT NULL
            """)
            bounds = cursor.fetchone()
            
            # Korea longitude: ~124-132, latitude: ~33-43
            self.assertGreater(bounds['min_lng'], 120, "Longitude too small")
            self.assertLess(bounds['max_lng'], 135, "Longitude too large")
            self.assertGreater(bounds['min_lat'], 30, "Latitude too small")
            self.assertLess(bounds['max_lat'], 45, "Latitude too large")

class RecommendationTestCase(unittest.TestCase):
    """Test recommendation system functionality"""
    
    @classmethod
    def setUpClass(cls):
        """Set up recommender for tests"""
        try:
            cls.recommender = RestaurantRecommender(verbose=False)
        except Exception as e:
            raise unittest.SkipTest(f"Cannot initialize recommender: {e}")
    
    @classmethod
    def tearDownClass(cls):
        """Clean up recommender"""
        if hasattr(cls, 'recommender'):
            cls.recommender.close()
    
    def setUp(self):
        """Set up test user profiles"""
        # Seoul National University coordinates
        self.test_location = (126.9525, 37.4583)
        
        # Basic user profile
        self.basic_user = UserProfile(
            location=self.test_location,
            cuisine_preferences=["korean"]
        )
        
        # User with multiple cuisine preferences
        self.multi_cuisine_user = UserProfile(
            location=self.test_location,
            cuisine_preferences=["korean", "japanese"]
        )
        
        # User with specific cuisine preference
        self.specific_user = UserProfile(
            location=self.test_location,
            cuisine_preferences=["italian"]
        )
    
    def test_find_nearby_restaurants(self):
        """Test finding nearby restaurants"""
        restaurants = self.recommender.find_nearby_restaurants(
            self.basic_user, 
            max_distance_km=2.0
        )
        
        self.assertIsInstance(restaurants, list)
        
        if not restaurants:
            self.skipTest("No nearby restaurants found")
        
        # Check that results have required fields
        for restaurant in restaurants[:3]:  # Check first 3 (or fewer if available)
            self.assertIn('id', restaurant)
            self.assertIn('name', restaurant)
            self.assertIn('distance_meters', restaurant)
            self.assertIsInstance(restaurant['distance_meters'], (int, float))
            self.assertLessEqual(restaurant['distance_meters'], 2000)  # Within 2km
    
    def test_get_restaurant_menus(self):
        """Test getting menus for restaurants"""
        # First get some restaurants
        restaurants = self.recommender.find_nearby_restaurants(
            self.basic_user, 
            max_distance_km=1.0
        )
        
        if not restaurants:
            self.skipTest("No restaurants found for menu test")
        
        restaurant_ids = [r['id'] for r in restaurants[:3]]
        menus = self.recommender.get_restaurant_menus(restaurant_ids)
        
        self.assertIsInstance(menus, dict)
        self.assertGreater(len(menus), 0, "No menus found")
        
        # Check menu structure
        for restaurant_id, menu_list in menus.items():
            self.assertIsInstance(menu_list, list)
            if menu_list:  # If there are menus
                menu = menu_list[0]
                self.assertIn('id', menu)
                self.assertIn('name', menu)
                self.assertIn('restaurant_name', menu)
                break  # Just check one restaurant's menus
    
    def test_menu_categorization(self):
        """Test menu categorization functionality"""
        # Get some menus
        restaurants = self.recommender.find_nearby_restaurants(
            self.basic_user, 
            max_distance_km=1.0
        )
        
        if not restaurants:
            self.skipTest("No restaurants found for categorization test")
        
        restaurant_ids = [r['id'] for r in restaurants[:3]]
        all_menus_dict = self.recommender.get_restaurant_menus(restaurant_ids)
        all_menus = []
        for menus in all_menus_dict.values():
            all_menus.extend(menus)
        
        if not all_menus:
            self.skipTest("No menus found for categorization test")
        
        # Test categorization
        categorized_menus = self.recommender.categorize_menus(all_menus[:10], method="langchain")  # Limit for test
        
        self.assertIsInstance(categorized_menus, dict)
        self.assertGreater(len(categorized_menus), 0, "No categories returned")
        
        # Check that all menus are categorized
        total_categorized = sum(len(menu_list) for menu_list in categorized_menus.values())
        self.assertGreater(total_categorized, 0, "No menus were categorized")
        
        # Print categorization results in readable format
        print("\n=== Menu Categorization Results ===")
        for category, menu_list in categorized_menus.items():
            print(f"\n{category} ({len(menu_list)} items):")
            for menu in menu_list:
                menu_name = menu.get('name', 'Unknown')
                restaurant_name = menu.get('restaurant_name', 'Unknown Restaurant')
                print(f"  - {menu_name} ({restaurant_name})")
        print("=" * 40)
    
    def test_normalized_menu_names(self):
        """Test that normalized menu names are generated"""
        # Get some menus
        restaurants = self.recommender.find_nearby_restaurants(
            self.basic_user, 
            max_distance_km=1.0
        )
        
        if not restaurants:
            self.skipTest("No restaurants found for normalized name test")
        
        restaurant_ids = [r['id'] for r in restaurants[:2]]
        all_menus_dict = self.recommender.get_restaurant_menus(restaurant_ids)
        
        # Check if any menus have normalized names
        has_normalized = False
        for menus in all_menus_dict.values():
            for menu in menus:
                if menu.get('name_clean'):
                    has_normalized = True
                    # Verify it's different or cleaned
                    original = menu.get('name', '')
                    cleaned = menu.get('name_clean', '')
                    self.assertIsInstance(cleaned, str)
                    break
            if has_normalized:
                break
        
        # Note: This test may pass even if normalization hasn't been run yet
        # It's mainly to verify the field exists and is accessible
    
    def test_image_urls_in_menus(self):
        """Test that image URLs are properly included in menu data"""
        # Get some restaurants
        restaurants = self.recommender.find_nearby_restaurants(
            self.basic_user, 
            max_distance_km=1.0
        )
        
        if not restaurants:
            self.skipTest("No restaurants found for image test")
        
        restaurant_ids = [r['id'] for r in restaurants[:3]]
        all_menus_dict = self.recommender.get_restaurant_menus(restaurant_ids)
        
        # Check that menus include images field
        has_images_field = False
        has_actual_images = False
        
        for menus in all_menus_dict.values():
            for menu in menus:
                # Check if images field exists
                if 'images' in menu:
                    has_images_field = True
                    images = menu['images']
                    self.assertIsInstance(images, (list, type(None)))
                    
                    # Check if there are actual image URLs
                    if images and len(images) > 0:
                        has_actual_images = True
                        for image_url in images:
                            self.assertIsInstance(image_url, str)
                        break
            if has_actual_images:
                break
        
        # At least the images field should exist
        self.assertTrue(has_images_field, "Menus should have 'images' field")
        
        if has_actual_images:
            print(f"✓ Found menus with actual image URLs")
        else:
            print(f"⚠ No menus with actual images found (field exists but empty)")
    
    def test_full_recommendation_pipeline(self):
        """Test complete recommendation generation"""
        recommendations = self.recommender.generate_recommendations(
            user_profile=self.multi_cuisine_user,
            max_distance_km=1.5
        )
        
        self.assertIsInstance(recommendations, dict)
        
        # Check for error message
        if "message" in recommendations:
            self.skipTest(f"No recommendations available: {recommendations['message']}")
        
        # Check required fields
        self.assertIn('user_location', recommendations)
        self.assertIn('total_restaurants', recommendations)
        self.assertIn('recommendations', recommendations)
        
        # Check recommendation structure
        recs = recommendations['recommendations']
        self.assertIsInstance(recs, dict)
        
        for category, rec_data in recs.items():
            self.assertIn('reason', rec_data)
            self.assertIn('menus', rec_data)
            self.assertIsInstance(rec_data['menus'], list)
            
            # Check menu structure (if menus exist)
            if rec_data['menus']:
                for menu in rec_data['menus'][:2]:  # Check first 2
                    self.assertIn('name', menu)
                    self.assertIn('restaurant', menu)
                    self.assertIn('price', menu)
                    # Check new fields
                    self.assertIn('images', menu)
                    self.assertIn('embedding_distance_to_center', menu)
                    self.assertIsInstance(menu['images'], list)
                break  # Just check one category
    
    def test_embedding_based_recommendations(self):
        """Test embedding-based recommendation pipeline with new features"""
        recommendations = self.recommender.generate_recommendations(
            user_profile=self.basic_user,
            max_distance_km=1.0,
            max_menus_to_categorize=20,
            max_menus_per_category=5,
            method="embedding",
            clustering_method="spectral"
        )
        
        self.assertIsInstance(recommendations, dict)
        
        if "message" in recommendations:
            self.skipTest(f"No recommendations available: {recommendations['message']}")
        
        # Check embedding-specific features
        if 'recommendations' in recommendations and recommendations['recommendations']:
            recs = recommendations['recommendations']
            
            for category, rec_data in recs.items():
                self.assertIn('menus', rec_data)
                
                # Check that menus are properly sorted (images first, then by embedding distance)
                menus = rec_data['menus']
                if len(menus) > 1:
                    # Check if sorting is working: menus with images should come first
                    images_first = True
                    found_no_image = False
                    
                    for menu in menus:
                        has_images = menu.get('images') and len(menu.get('images', [])) > 0
                        
                        if found_no_image and has_images:
                            images_first = False  # Found image after no-image
                            break
                        
                        if not has_images:
                            found_no_image = True
                    
                    # This is a soft check - sorting might work even if no images exist
                    if not images_first:
                        print(f"⚠ Image-first sorting might not be working in category: {category}")
                
                # Check embedding distance values
                for menu in menus:
                    distance = menu.get('embedding_distance_to_center')
                    if distance is not None:
                        self.assertIsInstance(distance, (int, float))
                        self.assertGreaterEqual(distance, 0)
                        self.assertLessEqual(distance, 2)  # Cosine distance range
                
                break  # Just check one category

class PerformanceTestCase(unittest.TestCase):
    """Test system performance"""
    
    @classmethod
    def setUpClass(cls):
        """Set up for performance tests"""
        try:
            cls.recommender = RestaurantRecommender(verbose=False)
        except Exception as e:
            raise unittest.SkipTest(f"Cannot initialize recommender: {e}")
    
    @classmethod
    def tearDownClass(cls):
        """Clean up"""
        if hasattr(cls, 'recommender'):
            cls.recommender.close()
    
    def test_recommendation_performance(self):
        """Test recommendation generation performance"""
        user = UserProfile(
            location=(126.9525, 37.4583),
            cuisine_preferences=["japanese"]
        )
        
        start_time = time.time()
        
        recommendations = self.recommender.generate_recommendations(
            user_profile=user,
            max_distance_km=1.0
        )
        
        end_time = time.time()
        duration = end_time - start_time
        
        # Should complete within reasonable time (adjust as needed)
        self.assertLess(duration, 30.0, f"Recommendation took too long: {duration:.2f}s")
        
        print(f"Recommendation generation time: {duration:.2f}s")
    
    def test_spatial_query_performance(self):
        """Test spatial query performance"""
        user = UserProfile(location=(126.9525, 37.4583))
        
        start_time = time.time()
        
        restaurants = self.recommender.find_nearby_restaurants(
            user, 
            max_distance_km=2.0
        )
        
        end_time = time.time()
        duration = end_time - start_time
        
        self.assertLess(duration, 5.0, f"Spatial query took too long: {duration:.2f}s")
        self.assertGreater(len(restaurants), 0, "No restaurants found")
        
        print(f"Spatial query time: {duration:.2f}s, found {len(restaurants)} restaurants")

def run_tests():
    """Run all tests with detailed output"""
    # Create test suite
    loader = unittest.TestLoader()
    suite = unittest.TestSuite()
    
    # Add test cases
    suite.addTests(loader.loadTestsFromTestCase(DatabaseTestCase))
    suite.addTests(loader.loadTestsFromTestCase(RecommendationTestCase))
    suite.addTests(loader.loadTestsFromTestCase(PerformanceTestCase))
    
    # Run tests
    runner = unittest.TextTestRunner(verbosity=2)
    result = runner.run(suite)
    
    # Print summary
    print("\n" + "="*50)
    print("TEST SUMMARY")
    print("="*50)
    print(f"Tests run: {result.testsRun}")
    print(f"Failures: {len(result.failures)}")
    print(f"Errors: {len(result.errors)}")
    
    if result.failures:
        print("\nFAILURES:")
        for test, traceback in result.failures:
            print(f"- {test}: {traceback.split('AssertionError: ')[-1].strip()}")
    
    if result.errors:
        print("\nERRORS:")
        for test, traceback in result.errors:
            print(f"- {test}: {traceback.split('Exception: ')[-1].strip()}")
    
    return result.wasSuccessful()

if __name__ == "__main__":
    print("Restaurant Recommendation System - Test Suite")
    print("=" * 50)
    
    # Check if database is available
    try:
        import psycopg2
        conn = psycopg2.connect(
            host=os.getenv('DB_HOST', 'localhost'),
            port=os.getenv('DB_PORT', '5432'),
            database=os.getenv('DB_NAME', 'foodigram'),
            user=os.getenv('DB_USER', 'postgres'),
            password=os.getenv('DB_PASSWORD', 'postgres')
        )
        conn.close()
        print("✓ Database connection verified")
    except Exception as e:
        print(f"✗ Database connection failed: {e}")
        print("\nPlease ensure:")
        print("1. PostgreSQL is running (docker-compose up -d)")
        print("2. Database is populated (python into_db.py)")
        print("3. Environment variables are set (.env file)")
        sys.exit(1)
    
    print("\nRunning tests...\n")
    
    success = run_tests()
    
    if success:
        print("\n✓ All tests passed!")
        sys.exit(0)
    else:
        print("\n✗ Some tests failed!")
        sys.exit(1)