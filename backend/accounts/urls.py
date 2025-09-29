"""
URL patterns for the accounts app.
"""

from django.urls import path
from rest_framework_simplejwt.views import TokenRefreshView
from . import views

app_name = 'accounts'

urlpatterns = [
    # Authentication endpoints matching frontend expectations
    path('login/', views.CustomTokenObtainPairView.as_view(), name='login'),
    path('signup/', views.UserRegistrationView.as_view(), name='signup'),
    path('token/refresh/', TokenRefreshView.as_view(), name='token_refresh'),
    
    # Password reset endpoints
    path('forgot/start/', views.PasswordResetRequestView.as_view(), name='forgot_password_start'),
    path('forgot/verify/', views.PasswordResetVerifyView.as_view(), name='forgot_password_verify'),
    path('forgot/reset/', views.PasswordResetConfirmView.as_view(), name='forgot_password_reset'),
    
    # Social authentication
    path('social/', views.SocialAuthView.as_view(), name='social_auth'),
    
    # Profile management
    path('profile/', views.user_profile, name='user_profile'),
    path('profile/update/', views.update_profile, name='update_profile'),
]
