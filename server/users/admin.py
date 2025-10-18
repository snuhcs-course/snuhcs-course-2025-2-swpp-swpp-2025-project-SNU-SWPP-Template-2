from django.contrib import admin
from django.contrib.auth.admin import UserAdmin
from .models import User, Profile, Follow, UserScrap


@admin.register(User)
class CustomUserAdmin(UserAdmin):
    list_display = ('username', 'email', 'first_name', 'last_name', 'is_staff')
    list_filter = ('is_staff', 'is_superuser', 'is_active', 'date_joined')
    search_fields = ('username', 'first_name', 'last_name', 'email')
    ordering = ('username',)


@admin.register(Profile)
class ProfileAdmin(admin.ModelAdmin):
    list_display = ('user', 'bio', 'updated_at')
    search_fields = ('user__username', 'user__email')
    readonly_fields = ('updated_at',)


@admin.register(Follow)
class FollowAdmin(admin.ModelAdmin):
    list_display = ('follower', 'following', 'status', 'created_at')
    list_filter = ('status', 'created_at')
    search_fields = ('follower__username', 'following__username')
    readonly_fields = ('created_at',)


@admin.register(UserScrap)
class UserScrapAdmin(admin.ModelAdmin):
    list_display = ('user', 'restaurant', 'created_at')
    list_filter = ('created_at',)
    search_fields = ('user__username', 'restaurant__name')
    readonly_fields = ('created_at',)
    
    def get_queryset(self, request):
        # 성능 최적화: user와 restaurant 정보를 함께 로드
        qs = super().get_queryset(request)
        return qs.select_related('user', 'restaurant')
