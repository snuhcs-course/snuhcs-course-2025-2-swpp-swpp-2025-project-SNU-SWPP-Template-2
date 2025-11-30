#!/usr/bin/env python
"""Test streaming API functionality"""

import os
import sys
import json

# Setup Django before importing models
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'config.settings')

import django
django.setup()

from django.conf import settings
if 'testserver' not in settings.ALLOWED_HOSTS:
    settings.ALLOWED_HOSTS.append('testserver')

from django.test import Client
from django.contrib.auth import authenticate
from users.models import User, UserPreference

def test_streaming_api():
    """Test that streaming API works correctly"""
    client = Client()
    
    # Create test user if needed
    try:
        user = User.objects.get(username='testuser')
    except User.DoesNotExist:
        user = User.objects.create_user(username='testuser', email='test@test.com', password='testpass')
        
    # Create preferences
    UserPreference.objects.get_or_create(
        user=user,
        defaults={
            'spicy_level': 3,
            'sweet_level': 3,
            'salty_level': 3,
            'allergies': [],
            'disliked_ingredients': [],
            'favorite_cuisines': ['한식'],
        }
    )
    
    # Login
    client.force_login(user)
    
    # Test data - using 서울대입구역 coordinates
    data = {
        'user_location': [126.9619864, 37.477136],  # 서울대입구역
        'maxResults': 3,
        'queryText': ''  # Remove query filter to get more results
    }
    
    print("Testing streaming API...")
    print(f"Request data: {data}")
    
    # Make streaming request
    response = client.post(
        '/api/v1/recommendation/recommend/menu/',
        data=json.dumps(data),
        content_type='application/json',
        HTTP_ACCEPT='application/x-ndjson'
    )
    
    print(f"Response status: {response.status_code}")
    print(f"Response headers: {dict(response.items())}")
    
    if response.status_code == 200:
        # Check if response is streaming
        if hasattr(response, 'streaming_content'):
            print("✅ Response is streaming!")
            chunk_count = 0
            for chunk in response.streaming_content:
                if chunk.strip():
                    chunk_count += 1
                    try:
                        chunk_data = json.loads(chunk.decode('utf-8').strip())
                        print(f"Chunk {chunk_count}: {chunk_data}")
                        if chunk_count >= 3:  # Limit output
                            break
                    except json.JSONDecodeError as e:
                        print(f"Failed to parse chunk: {chunk} - Error: {e}")
            print(f"✅ Processed {chunk_count} chunks successfully")
        else:
            print("❌ Response is not streaming")
            print(f"Response content: {response.content}")
    else:
        print(f"❌ Request failed: {response.content}")

if __name__ == '__main__':
    test_streaming_api()