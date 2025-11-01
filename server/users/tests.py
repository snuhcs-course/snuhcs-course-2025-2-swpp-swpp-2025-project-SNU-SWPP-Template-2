from django.test import TestCase
from django.urls import reverse
from rest_framework.test import APIClient
from rest_framework import status
from .models import User, UserGalleryImage

# Create your tests here.

class PhotoViewSetTests(TestCase):
    def setUp(self):
        self.client = APIClient()
        self.user = User.objects.create_user(username='testuser', password='password123', email='test@example.com')

    def authenticate(self):
        self.client.force_authenticate(user=self.user)

    def test_requires_authentication(self):
        # Should get 401 if not authenticated
        response_get = self.client.get(reverse('photos-list'))
        response_post = self.client.post(reverse('photos-list'), {'photo_url': 'http://example.com/image.jpg'})
        self.assertEqual(response_get.status_code, status.HTTP_403_FORBIDDEN)
        self.assertEqual(response_post.status_code, status.HTTP_403_FORBIDDEN)

    def test_list_photos_empty(self):
        self.authenticate()
        response = self.client.get(reverse('photos-list'))
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.json(), [])

    def test_create_photo(self):
        self.authenticate()
        url = reverse('photos-list')
        photo_url = 'http://example.com/image1.jpg'
        response = self.client.post(url, {'photo_url': photo_url})
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        self.assertIn('image_url', response.json())
        self.assertEqual(response.json()['image_url'], photo_url)
        self.assertTrue(UserGalleryImage.objects.filter(image_url=photo_url, user=self.user).exists())

    def test_list_photos_after_creation(self):
        self.authenticate()
        photo1 = UserGalleryImage.objects.create(user=self.user, image_url='http://example.com/img1.jpg')
        photo2 = UserGalleryImage.objects.create(user=self.user, image_url='http://example.com/img2.jpg')
        response = self.client.get(reverse('photos-list'))
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        image_urls = [photo['image_url'] for photo in response.json()]
        self.assertIn('http://example.com/img1.jpg', image_urls)
        self.assertIn('http://example.com/img2.jpg', image_urls)

    def test_user_is_isolated(self):
        self.authenticate()
        other_user = User.objects.create_user(username='otheruser', password='testpass', email='other@example.com')
        UserGalleryImage.objects.create(user=other_user, image_url='http://example.com/other.jpg')
        response = self.client.get(reverse('photos-list'))
        image_urls = [photo['image_url'] for photo in response.json()]
        self.assertNotIn('http://example.com/other.jpg', image_urls)
