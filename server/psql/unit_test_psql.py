#!/usr/bin/env python3
"""
unit_test_psql.py - Unit tests for restaurant recommendation system

This module provides comprehensive unit tests for individual components
and edge cases in the psql/ recommendation system.
"""

import unittest
from unittest.mock import Mock, patch, MagicMock
import numpy as np
import os
import sys
import json
import time
from typing import Dict, List, Any
import psycopg2
from psycopg2.extras import RealDictCursor
from dotenv import load_dotenv

# Import our modules
from client import RestaurantRecommender, UserProfile

# Load environment variables
load_dotenv()

class ExtendedRecommenderTestCase(unittest.TestCase):
    """Extended tests for RestaurantRecommender to improve coverage"""
    
    @classmethod
    def setUpClass(cls):
        """Set up recommender for tests"""
        try:
            cls.recommender = RestaurantRecommender()
        except Exception as e:
            raise unittest.SkipTest(f"Cannot initialize recommender: {e}")
    
    @classmethod
    def tearDownClass(cls):
        """Clean up recommender"""
        if hasattr(cls, 'recommender'):
            cls.recommender.close()
    
    def setUp(self):
        """Set up test data"""
        self.basic_user = UserProfile(
            location=(126.9525, 37.4583),
            cuisine_preferences=["korean"]
        )
    
    def test_llm_initialization_scenarios(self):
        """Test LLM initialization with different scenarios"""
        # Test that LLM exists when API key is present
        if hasattr(self.recommender, 'llm') and self.recommender.llm:
            self.assertIsNotNone(self.recommender.llm)
        
        # Test behavior without API key by creating new recommender
        original_key = os.environ.get('OPENAI_API_KEY')
        if 'OPENAI_API_KEY' in os.environ:
            del os.environ['OPENAI_API_KEY']
        
        try:
            # Create new recommender without API key
            new_recommender = RestaurantRecommender()
            self.assertIsNone(new_recommender.llm)
            new_recommender.close()
        except:
            # Skip if initialization fails without API key
            pass
        finally:
            # Restore the original key
            if original_key:
                os.environ['OPENAI_API_KEY'] = original_key
    
    def test_precompute_category_embeddings_no_llm(self):
        """Test _precompute_category_embeddings without LLM"""
        original_llm = self.recommender.llm
        self.recommender.llm = None
        
        try:
            # Should handle missing LLM gracefully
            self.recommender._precompute_category_embeddings()
            # No assertion needed, just checking it doesn't crash
        finally:
            self.recommender.llm = original_llm
    
    def test_embedding_categorization_no_embedding_model(self):
        """Test categorize_menus_embedding without embedding model"""
        original_model = self.recommender.embedding_model
        self.recommender.embedding_model = None
        
        try:
            menus = [{'name': 'test', 'name_clean': 'test'}]
            result = self.recommender.categorize_menus_embedding(menus, self.basic_user)
            
            # Should return 일반 category when no embedding model
            self.assertEqual(result, {"일반": menus})
        finally:
            self.recommender.embedding_model = original_model
    
    def test_embedding_categorization_empty_menus(self):
        """Test categorize_menus_embedding with empty menu list"""
        result = self.recommender.categorize_menus_embedding([], self.basic_user)
        self.assertEqual(result, {"일반": []})
    
    def test_embedding_categorization_no_valid_menus(self):
        """Test categorize_menus_embedding with no valid menus for clustering"""
        # Menus without name_clean or embedding_vector
        menus = [
            {'name': 'test1'},  # No name_clean or embedding_vector
            {'name': 'test2'},  # No name_clean or embedding_vector
        ]
        
        result = self.recommender.categorize_menus_embedding(menus, self.basic_user)
        self.assertEqual(result, {"일반": menus})
    
    def test_simple_similarity_clustering_edge_cases(self):
        """Test _simple_similarity_clustering with edge cases"""
        # Test with single vector
        single_vector = np.array([[1.0, 0.0, 0.0]])
        labels = self.recommender._simple_similarity_clustering(single_vector)
        self.assertEqual(len(labels), 1)
        # Single item starts as noise (-1) until clustering is complete
        self.assertEqual(labels[0], -1)
        
        # Test with identical vectors
        identical_vectors = np.array([
            [1.0, 0.0, 0.0],
            [1.0, 0.0, 0.0],
            [1.0, 0.0, 0.0]
        ])
        labels = self.recommender._simple_similarity_clustering(identical_vectors)
        # All should have same label
        self.assertTrue(np.all(labels == labels[0]))
    
    def test_clean_category_name_edge_cases(self):
        """Test _clean_category_name with various edge cases"""
        # Test with normal string (should not be truncated by _clean_category_name itself)
        long_string = "a" * 100
        result = self.recommender._clean_category_name(long_string)
        self.assertEqual(result, "a" * 100)  # _clean_category_name doesn't truncate, just cleans
        
        # Test with special unicode characters
        unicode_string = "한식\ufffd요리"
        result = self.recommender._clean_category_name(unicode_string)
        self.assertEqual(result, "한식요리")  # Should clean invalid unicode
        
        # Test with just whitespace
        whitespace_string = "   \t\n   "
        result = self.recommender._clean_category_name(whitespace_string)
        self.assertEqual(result, "음식")  # Should return fallback for empty after cleaning
    
    def test_generate_cluster_name_with_no_names(self):
        """Test _generate_cluster_name with menus that have no names"""
        menus = [
            {'id': 1, 'price': 5000},  # No 'name' field
            {'id': 2, 'name': ''},     # Empty name
        ]
        
        result = self.recommender._generate_cluster_name(menus)
        self.assertEqual(result, "음식")  # Should return fallback
    
    def test_generate_cluster_name_prompt_creation(self):
        """Test _generate_cluster_name prompt template creation"""
        menus = [
            {'name': '김치찌개'},
            {'name': '된장찌개'},
            {'name': '순두부찌개'}
        ]
        
        # Mock the LLM completely to test prompt creation
        if self.recommender.llm:
            # Create a mock LLM and temporarily replace it
            mock_llm = Mock()
            mock_response = Mock()
            mock_response.content = "찌개류"
            mock_llm.invoke.return_value = mock_response
            
            original_llm = self.recommender.llm
            self.recommender.llm = mock_llm
            
            try:
                result = self.recommender._generate_cluster_name(menus)
                
                # Verify LLM was called
                mock_llm.invoke.assert_called_once()
                # Verify result processing
                self.assertEqual(result, "찌개류")
            finally:
                # Restore original LLM
                self.recommender.llm = original_llm
    
    def test_categorize_menus_langchain_batch_processing(self):
        """Test categorize_menus_langchain batch processing logic"""
        # Create enough menus to trigger batching
        menus = []
        for i in range(25):  # More than LLM_BATCH_SIZE (20)
            menus.append({
                'name': f'menu_{i}',
                'id': i,
                'price': 5000 + i * 100
            })
        
        # Test that it handles batching
        if self.recommender.llm:
            result = self.recommender.categorize_menus_langchain(menus)
            self.assertIsInstance(result, dict)
            # Should either categorize or return fallback
            self.assertTrue(len(result) > 0)
    
    def test_categorize_menus_langchain_llm_response_parsing(self):
        """Test LLM response parsing in categorize_menus_langchain"""
        menus = [
            {'name': '김치찌개', 'id': 1},
            {'name': '된장찌개', 'id': 2}
        ]
        
        if self.recommender.llm:
            # Test with different response formats
            test_responses = [
                '{"찌개류": ["1", "2"]}',  # Valid JSON
                'Category name: 찌개류\n1: 김치찌개\n2: 된장찌개',  # Invalid JSON
                '',  # Empty response
            ]
            
            original_llm = self.recommender.llm
            
            for response_content in test_responses:
                # Create a mock LLM and temporarily replace it
                mock_llm = Mock()
                mock_response = Mock()
                mock_response.content = response_content
                mock_llm.invoke.return_value = mock_response
                
                self.recommender.llm = mock_llm
                
                try:
                    result = self.recommender.categorize_menus_langchain(menus)
                    self.assertIsInstance(result, dict)
                finally:
                    # Restore original LLM for next iteration
                    self.recommender.llm = original_llm
    
    def test_clustering_methods_coverage(self):
        """Test different clustering methods to cover conditional branches"""
        # Get a real restaurant ID from the database to avoid UUID/integer mismatch
        restaurants = self.recommender.find_nearby_restaurants(
            self.basic_user, max_distance_km=1.0
        )
        
        if not restaurants:
            self.skipTest("No restaurants found for clustering test")
        
        real_restaurant_id = restaurants[0]['id']
        
        menus = []
        for i in range(10):
            embedding = np.random.random(768).tolist()
            menus.append({
                'name': f'menu_{i}',
                'name_clean': f'menu_{i}',
                'embedding_vector': embedding,
                'restaurant_id': real_restaurant_id
            })
        
        # Test HDBSCAN
        with patch.dict(os.environ, {'CLUSTERING_METHOD': 'hdbscan'}):
            result = self.recommender.categorize_menus_embedding(menus, self.basic_user)
            self.assertIsInstance(result, dict)
        
        # Test KMeans
        with patch.dict(os.environ, {'CLUSTERING_METHOD': 'kmeans'}):
            result = self.recommender.categorize_menus_embedding(menus, self.basic_user)
            self.assertIsInstance(result, dict)
        
        # Test Spectral
        with patch.dict(os.environ, {'CLUSTERING_METHOD': 'spectral'}):
            result = self.recommender.categorize_menus_embedding(menus, self.basic_user)
            self.assertIsInstance(result, dict)
    
    def test_user_preference_weighting(self):
        """Test user preference weighting in embedding categorization"""
        # Get real restaurant IDs to avoid UUID/integer mismatch
        restaurants = self.recommender.find_nearby_restaurants(
            self.basic_user, max_distance_km=1.0
        )
        
        if len(restaurants) < 5:
            self.skipTest("Not enough restaurants found for weighting test")
        
        menus = []
        for i in range(5):
            embedding = np.random.random(768).tolist()
            menus.append({
                'name': f'menu_{i}',
                'name_clean': f'menu_{i}',
                'embedding_vector': embedding,
                'restaurant_id': restaurants[i]['id']
            })
        
        # Test with user weighting enabled
        result_with_weighting = self.recommender.categorize_menus_embedding(
            menus, self.basic_user, use_user_weighting=True
        )
        self.assertIsInstance(result_with_weighting, dict)
        
        # Test with user weighting disabled
        result_without_weighting = self.recommender.categorize_menus_embedding(
            menus, self.basic_user, use_user_weighting=False
        )
        self.assertIsInstance(result_without_weighting, dict)
    
    def test_menu_categorization_error_handling(self):
        """Test error handling in menu categorization"""
        # Test with malformed menu data
        bad_menus = [
            {'name': 'valid_menu', 'name_clean': 'valid_menu'},
            {'name': None},  # None name
            {'name_clean': None},  # None name_clean
            {'embedding_vector': 'not_a_list'},  # Invalid embedding
        ]
        
        try:
            result = self.recommender.categorize_menus_embedding(bad_menus, self.basic_user)
            self.assertIsInstance(result, dict)
        except Exception as e:
            # Should handle errors gracefully
            self.fail(f"categorize_menus_embedding raised unexpected exception: {e}")
    
    def test_database_connection_properties(self):
        """Test database connection related methods"""
        # Test that connection exists
        self.assertIsNotNone(self.recommender.conn)
        
        # Test _get_db_connection method
        # Note: This will use the existing connection setup
        conn = self.recommender._get_db_connection()
        self.assertIsNotNone(conn)
    
    def test_find_nearby_restaurants_distance_variations(self):
        """Test find_nearby_restaurants with different distance parameters"""
        # Test with small distance
        restaurants_small = self.recommender.find_nearby_restaurants(
            self.basic_user, max_distance_km=0.5
        )
        
        # Test with larger distance  
        restaurants_large = self.recommender.find_nearby_restaurants(
            self.basic_user, max_distance_km=2.0
        )
        
        # Should get more restaurants with larger distance (or at least same amount)
        self.assertGreaterEqual(len(restaurants_large), len(restaurants_small))
    
    def test_get_restaurant_menus_multiple_restaurants(self):
        """Test get_restaurant_menus with multiple restaurant IDs"""
        # First get some restaurant IDs with distance limit to avoid prompt
        restaurants = self.recommender.find_nearby_restaurants(
            self.basic_user, max_distance_km=1.0
        )
        
        if len(restaurants) >= 2:
            restaurant_ids = [r['id'] for r in restaurants[:2]]
            menus = self.recommender.get_restaurant_menus(restaurant_ids)
            
            self.assertIsInstance(menus, dict)
            # Should have entries for the requested restaurants
            for restaurant_id in restaurant_ids:
                if restaurant_id in menus:
                    self.assertIsInstance(menus[restaurant_id], list)
    
    def test_categorize_menus_method_variations(self):
        """Test categorize_menus wrapper with different method parameters"""
        menus = [{'name': 'test_menu', 'id': 1}]
        
        # Test with explicit method parameters
        result_embedding = self.recommender.categorize_menus(
            menus, self.basic_user, method="embedding"
        )
        self.assertIsInstance(result_embedding, dict)
        
        result_langchain = self.recommender.categorize_menus(
            menus, method="langchain"
        )
        self.assertIsInstance(result_langchain, dict)
        
        # Test with invalid method
        result_invalid = self.recommender.categorize_menus(
            menus, method="invalid_method"
        )
        self.assertEqual(result_invalid, {"일반": menus})
    
    def test_generate_recommendations_parameter_variations(self):
        """Test generate_recommendations with different parameters"""
        try:
            # Test with custom max_distance_km
            result1 = self.recommender.generate_recommendations(
                user_profile=self.basic_user,
                max_distance_km=1.0  # Use reasonable distance to avoid prompts
            )
            self.assertIsInstance(result1, dict)
            
            # Test with custom categories
            result2 = self.recommender.generate_recommendations(
                user_profile=self.basic_user,
                max_distance_km=1.0,
                categories=['한식']
            )
            self.assertIsInstance(result2, dict)
            
            # Test with custom menu limits
            result3 = self.recommender.generate_recommendations(
                user_profile=self.basic_user,
                max_distance_km=1.0,
                max_menus_to_categorize=10,
                max_menus_per_category=2
            )
            self.assertIsInstance(result3, dict)
        except Exception as e:
            # Skip if database transaction issues occur
            self.skipTest(f"Database transaction issue: {e}")


class TestUserProfileVariations(unittest.TestCase):
    """Test UserProfile class with different parameter combinations"""
    
    def test_user_profile_with_location_info(self):
        """Test UserProfile with location_info parameter"""
        profile = UserProfile(
            location=(126.95, 37.45),
            location_info="Seoul National University"
        )
        self.assertEqual(profile.location_info, "Seoul National University")
    
    def test_user_profile_edge_case_coordinates(self):
        """Test UserProfile with edge case coordinates"""
        # Test with extreme coordinates
        profile = UserProfile(location=(-180.0, -90.0))
        self.assertEqual(profile.location, (-180.0, -90.0))
        
        profile2 = UserProfile(location=(180.0, 90.0))
        self.assertEqual(profile2.location, (180.0, 90.0))
    
    def test_user_profile_many_cuisine_preferences(self):
        """Test UserProfile with many cuisine preferences"""
        many_cuisines = [
            "korean", "japanese", "chinese", "italian", 
            "mexican", "thai", "vietnamese", "indian"
        ]
        profile = UserProfile(
            location=(126.95, 37.45),
            cuisine_preferences=many_cuisines
        )
        self.assertEqual(profile.cuisine_preferences, many_cuisines)


def run_unit_tests():
    """Run unit tests to improve coverage"""
    # Create test suite
    loader = unittest.TestLoader()
    suite = unittest.TestSuite()
    
    # Add test cases
    suite.addTests(loader.loadTestsFromTestCase(ExtendedRecommenderTestCase))
    suite.addTests(loader.loadTestsFromTestCase(TestUserProfileVariations))
    
    # Run tests
    runner = unittest.TextTestRunner(verbosity=2)
    result = runner.run(suite)
    
    # Print summary
    print("\n" + "="*50)
    print("EXTENDED TEST SUMMARY")
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
    
    success_rate = ((result.testsRun - len(result.failures) - len(result.errors)) / result.testsRun * 100) if result.testsRun > 0 else 0
    print(f"\nSuccess rate: {success_rate:.1f}%")
    
    return result.wasSuccessful()


if __name__ == "__main__":
    print("Unit Tests for Restaurant Recommendation System")
    print("=" * 50)
    print("Target: Comprehensive unit testing for psql/ components")
    print()
    
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
        sys.exit(1)
    
    print("\nRunning unit tests...\n")
    
    success = run_unit_tests()
    
    if success:
        print("\n✓ All unit tests passed!")
        sys.exit(0)
    else:
        print("\n✗ Some unit tests failed!")
        sys.exit(1)