"""
Test error cases for social app views to improve coverage.
"""

import pytest
from django.contrib.auth import get_user_model
from rest_framework.test import APIClient

from books.models import Author as BookAuthor, Book, Publisher
from social.models import Post


User = get_user_model()


@pytest.mark.django_db
def test_create_post_with_invalid_data():
    """Test POST /posts/ with invalid data returns 400."""
    client = APIClient()
    user = User.objects.create(username="user", email="u@test.com", first_name="U", last_name="ser")
    client.force_authenticate(user)
    
    # Missing content
    res = client.post("/posts/", {}, format="json")
    assert res.status_code == 400
    assert "error" in res.data or "content" in res.data


@pytest.mark.django_db
def test_update_post_with_invalid_data():
    """Test PUT /posts/{id}/ with invalid data returns 400."""
    client = APIClient()
    user = User.objects.create(username="user", email="u@test.com", first_name="U", last_name="ser")
    post = Post.objects.create(author=user, content="original")
    
    client.force_authenticate(user)
    
    # Empty content
    res = client.put(f"/posts/{post.id}/", {"content": ""}, format="json")
    assert res.status_code == 400


@pytest.mark.django_db
def test_comment_post_without_content():
    """Test POST /posts/{id}/comments/ without content returns 400."""
    client = APIClient()
    user = User.objects.create(username="user", email="u@test.com", first_name="U", last_name="ser")
    post = Post.objects.create(author=user, content="test")
    
    client.force_authenticate(user)
    
    # Missing content
    res = client.post(f"/posts/{post.id}/comments/", {}, format="json")
    assert res.status_code == 400
    assert "error" in res.data
    assert "required" in res.data["error"].lower()


@pytest.mark.django_db
def test_comment_post_with_empty_content():
    """Test POST /posts/{id}/comments/ with empty content returns 400."""
    client = APIClient()
    user = User.objects.create(username="user", email="u@test.com", first_name="U", last_name="ser")
    post = Post.objects.create(author=user, content="test")
    
    client.force_authenticate(user)
    
    # Empty/whitespace content
    res = client.post(f"/posts/{post.id}/comments/", {"content": "   "}, format="json")
    assert res.status_code == 400


@pytest.mark.django_db
def test_comment_nonexistent_post():
    """Test POST /posts/{id}/comments/ with invalid post_id returns 404."""
    client = APIClient()
    user = User.objects.create(username="user", email="u@test.com", first_name="U", last_name="ser")
    
    client.force_authenticate(user)
    
    res = client.post("/posts/99999/comments/", {"content": "test"}, format="json")
    assert res.status_code == 404
    assert "not found" in res.data["error"].lower()


@pytest.mark.django_db
def test_barter_post_without_related_book():
    """Test POST /posts/{id}/barter/ on post without related_book returns 400."""
    client = APIClient()
    user = User.objects.create(username="user", email="u@test.com", first_name="U", last_name="ser")
    post = Post.objects.create(author=user, content="no book here")
    
    requester = User.objects.create(username="req", email="r@test.com", first_name="R", last_name="eq")
    client.force_authenticate(requester)
    
    res = client.post(f"/posts/{post.id}/barter/", {"message": "trade?"}, format="json")
    assert res.status_code == 400
    assert "no related book" in res.data["error"].lower()


@pytest.mark.django_db
def test_barter_post_not_found():
    """Test POST /posts/{id}/barter/ with invalid post_id returns 404."""
    client = APIClient()
    user = User.objects.create(username="user", email="u@test.com", first_name="U", last_name="ser")
    client.force_authenticate(user)
    
    res = client.post("/posts/99999/barter/", {"message": "trade?"}, format="json")
    assert res.status_code == 404


@pytest.mark.django_db
def test_barter_own_book():
    """Test cannot create barter request for own book."""
    client = APIClient()
    user = User.objects.create(username="user", email="u@test.com", first_name="U", last_name="ser")
    
    publisher = Publisher.objects.create(name="Pub")
    auth = BookAuthor.objects.create(name="Auth")
    book = Book.objects.create(
        title="MyBook",
        owner=user,
        publisher=publisher,
        is_for_barter=True,
        trade_status="available"
    )
    book.authors.add(auth)
    
    post = Post.objects.create(author=user, content="test", related_book=book)
    
    client.force_authenticate(user)
    res = client.post(f"/posts/{post.id}/barter/", {"message": "trade?"}, format="json")
    assert res.status_code == 403
    assert "own book" in res.data["error"].lower()


@pytest.mark.django_db
def test_barter_book_not_for_barter():
    """Test cannot request book that owner disabled for trading."""
    client = APIClient()
    owner = User.objects.create(username="owner", email="o@test.com", first_name="O", last_name="wner")
    requester = User.objects.create(username="req", email="r@test.com", first_name="R", last_name="eq")
    
    publisher = Publisher.objects.create(name="Pub")
    auth = BookAuthor.objects.create(name="Auth")
    book = Book.objects.create(
        title="NoTrade",
        owner=owner,
        publisher=publisher,
        is_for_barter=False,  # Not for barter
        trade_status="available"
    )
    book.authors.add(auth)
    
    post = Post.objects.create(author=owner, content="test", related_book=book)
    
    client.force_authenticate(requester)
    res = client.post(f"/posts/{post.id}/barter/", {"message": "trade?"}, format="json")
    assert res.status_code == 400
    assert "disabled trading" in res.data["error"].lower()


@pytest.mark.django_db
def test_barter_book_already_in_trade():
    """Test cannot request book that is already in pending trade."""
    client = APIClient()
    owner = User.objects.create(username="owner", email="o@test.com", first_name="O", last_name="wner")
    requester = User.objects.create(username="req", email="r@test.com", first_name="R", last_name="eq")
    
    publisher = Publisher.objects.create(name="Pub")
    auth = BookAuthor.objects.create(name="Auth")
    book = Book.objects.create(
        title="Pending",
        owner=owner,
        publisher=publisher,
        is_for_barter=True,
        trade_status="not_available"  # Already in pending trade
    )
    book.authors.add(auth)
    
    post = Post.objects.create(author=owner, content="test", related_book=book)
    
    client.force_authenticate(requester)
    res = client.post(f"/posts/{post.id}/barter/", {"message": "trade?"}, format="json")
    assert res.status_code == 400
    assert "pending trade" in res.data["error"].lower()
