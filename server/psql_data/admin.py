from django.contrib import admin
from .models import DbRestaurant, DbMenu


@admin.register(DbRestaurant)
class DbRestaurantAdmin(admin.ModelAdmin):
    list_display = ('name', 'category_normalized', 'avg_rating', 'review_count', 'phone', 'address', 'inferred_menu', 'external_id')
    list_filter = ('category_normalized', 'meaningful_name', 'avg_rating', 'group3')
    search_fields = ('name', 'address', 'phone', 'category_normalized', 'inferred_menu')
    readonly_fields = ('id', 'created_at', 'updated_at')
    
    fieldsets = (
        ('Basic Info', {
            'fields': ('id', 'external_id', 'name', 'category', 'category_normalized', 'phone')
        }),
        ('Location', {
            'fields': ('address', 'road_address')
        }),
        ('Categories', {
            'fields': ('group1', 'group2', 'group3', 'category_code', 'category_code_list')
        }),
        ('Ratings & Reviews', {
            'fields': ('avg_rating', 'review_count')
        }),
        ('Images & Media', {
            'fields': ('place_images',)
        }),
        ('AI Analysis', {
            'fields': ('meaningful_name', 'inferred_menu', 'embedding_vector')
        }),
        ('Timestamps', {
            'fields': ('created_at', 'updated_at')
        }),
    )


@admin.register(DbMenu)
class DbMenuAdmin(admin.ModelAdmin):
    list_display = ('name', 'name_clean', 'restaurant', 'price', 'recommend', 'taste_profile', 'allergen_info', 'external_id')
    list_filter = ('restaurant__category', 'restaurant__category_normalized', 'recommend')
    search_fields = ('name', 'name_clean', 'restaurant__name', 'description', 'external_id')
    readonly_fields = ('id', 'created_at', 'updated_at')
    raw_id_fields = ('restaurant',)
    
    fieldsets = (
        ('Basic Info', {
            'fields': ('id', 'external_id', 'restaurant', 'name', 'name_clean')
        }),
        ('Details', {
            'fields': ('price', 'description', 'recommend', 'index_in_rest')
        }),
        ('Images & Media', {
            'fields': ('images',)
        }),
        ('Analysis', {
            'fields': ('taste_profile', 'allergen_info', 'embedding_vector')
        }),
        ('Timestamps', {
            'fields': ('created_at', 'updated_at')
        }),
    )