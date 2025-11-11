"""
Test error cases for accounts app views and serializers to improve coverage.
"""

import pytest
from django.contrib.auth import get_user_model
from django.urls import reverse
from rest_framework.test import APIClient
from django.core.cache import cache

from accounts.models import UserTaste, BookGenre, BookMood, ReadingPurpose, BookLength


User = get_user_model()


@pytest.mark.django_db
def test_taste_survey_validation_errors():
    """Test taste survey with insufficient selections."""
    client = APIClient()
    user = User.objects.create(username="user", email="u@test.com", first_name="U", last_name="ser")
    client.force_authenticate(user)
    
    # Step 1: Less than 3 genres
    res = client.put(
        reverse("accounts:taste-survey"),
        {"favorite_genres": [BookGenre.NOVEL]},
        format="json",
    )
    assert res.status_code == 400
    assert "3개 이상" in res.data["message"]
    
    # Step 2: Less than 3 authors
    taste = UserTaste.objects.get(user=user)
    taste.current_step = 2
    taste.save()
    
    res = client.put(
        reverse("accounts:taste-survey"),
        {"favorite_authors": ["Author 1"]},
        format="json",
    )
    assert res.status_code == 400
    assert "3명 이상" in res.data["message"]
    
    # Step 3: Less than 3 books
    taste.current_step = 3
    taste.save()
    
    res = client.put(
        reverse("accounts:taste-survey"),
        {"favorite_books": ["Book 1"]},
        format="json",
    )
    assert res.status_code == 400
    assert "3권 이상" in res.data["message"]
    
    # Step 4: Missing preferred_length
    taste.current_step = 4
    taste.save()
    
    res = client.put(
        reverse("accounts:taste-survey"),
        {"preferred_length": ""},
        format="json",
    )
    assert res.status_code == 400
    
    # Step 5: Less than 3 moods
    taste.current_step = 5
    taste.save()
    
    res = client.put(
        reverse("accounts:taste-survey"),
        {"preferred_moods": [BookMood.WARM]},
        format="json",
    )
    assert res.status_code == 400
    assert "3개 이상" in res.data["message"]
    
    # Step 6: Less than 3 purposes
    taste.current_step = 6
    taste.save()
    
    res = client.put(
        reverse("accounts:taste-survey"),
        {"reading_purposes": [ReadingPurpose.HEALING]},
        format="json",
    )
    assert res.status_code == 400
    assert "3개 이상" in res.data["message"]


@pytest.mark.django_db
def test_taste_survey_creates_if_not_exists():
    """Test taste survey GET creates UserTaste if it doesn't exist."""
    client = APIClient()
    user = User.objects.create(username="user", email="u@test.com", first_name="U", last_name="ser")
    client.force_authenticate(user)
    
    # Verify no taste exists
    assert not UserTaste.objects.filter(user=user).exists()
    
    # GET should create it
    res = client.get(reverse("accounts:taste-survey"))
    assert res.status_code == 200
    assert UserTaste.objects.filter(user=user).exists()


@pytest.mark.django_db
def test_taste_survey_invalid_serializer():
    """Test taste survey with invalid data."""
    client = APIClient()
    user = User.objects.create(username="user", email="u@test.com", first_name="U", last_name="ser")
    client.force_authenticate(user)
    
    # Invalid data type
    res = client.put(
        reverse("accounts:taste-survey"),
        {"favorite_genres": "not_a_list"},
        format="json",
    )
    assert res.status_code == 400


@pytest.mark.django_db
def test_password_reset_invalid_code():
    """Test password reset verify with invalid code."""
    client = APIClient()
    
    # Set up valid request in cache
    request_id = "test-request-id"
    cache_key = f"password_reset_{request_id}"
    cache.set(cache_key, {"email": "test@test.com", "code": "123456", "verified": False}, timeout=900)
    
    # Try with wrong code
    res = client.post(
        reverse("accounts:password-reset-verify"),
        {"request_id": request_id, "code": "wrong"},
        format="json",
    )
    assert res.status_code == 400
    assert "invalid" in res.data["message"].lower()


@pytest.mark.django_db
def test_password_reset_verify_invalid_serializer():
    """Test password reset verify with invalid data."""
    client = APIClient()
    
    # Missing fields
    res = client.post(
        reverse("accounts:password-reset-verify"),
        {},
        format="json",
    )
    assert res.status_code == 400
    assert "errors" in res.data


@pytest.mark.django_db
def test_user_registration_invalid_password():
    """Test user registration with invalid passwords."""
    client = APIClient()
    
    # Password too short
    res = client.post(
        reverse("accounts:register"),
        {
            "username": "newuser",
            "email": "new@test.com",
            "password": "12345",
            "first_name": "New",
            "last_name": "User",
        },
        format="json",
    )
    assert res.status_code == 400
    
    # Password without letter
    res = client.post(
        reverse("accounts:register"),
        {
            "username": "newuser2",
            "email": "new2@test.com",
            "password": "123456",
            "first_name": "New",
            "last_name": "User",
        },
        format="json",
    )
    assert res.status_code == 400


@pytest.mark.django_db
def test_login_serializer_password_validation():
    """Test login serializer validates password on registration."""
    from accounts.serializers import LoginSerializer
    
    # Valid password
    serializer = LoginSerializer(data={
        "username": "test",
        "email": "test@test.com",
        "password": "validpass123",
        "first_name": "Test",
        "last_name": "User",
    })
    assert serializer.is_valid()
    
    # Too short
    serializer = LoginSerializer(data={
        "username": "test2",
        "email": "test2@test.com",
        "password": "short",
        "first_name": "Test",
        "last_name": "User",
    })
    assert not serializer.is_valid()
    
    # No letter
    serializer = LoginSerializer(data={
        "username": "test3",
        "email": "test3@test.com",
        "password": "123456789",
        "first_name": "Test",
        "last_name": "User",
    })
    assert not serializer.is_valid()


@pytest.mark.django_db
def test_password_change_serializer_validation():
    """Test password change serializer validates new password."""
    from accounts.serializers import PasswordChangeSerializer
    
    # Valid password
    serializer = PasswordChangeSerializer(data={
        "old_password": "oldpass123",
        "new_password": "newpass456",
    })
    assert serializer.is_valid()
    
    # Too short
    serializer = PasswordChangeSerializer(data={
        "old_password": "oldpass123",
        "new_password": "short",
    })
    assert not serializer.is_valid()
    
    # No letter
    serializer = PasswordChangeSerializer(data={
        "old_password": "oldpass123",
        "new_password": "123456789",
    })
    assert not serializer.is_valid()
