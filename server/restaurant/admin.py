from django.contrib import admin
from .models import Restaurant, RestaurantMenu


@admin.register(Restaurant)
class RestaurantAdmin(admin.ModelAdmin):
    list_display = ('id', 'name', 'address', 'source', 'created_at')
    list_filter = ('source', 'created_at')
    search_fields = ('name', 'address', 'phone')
    readonly_fields = ('created_at',)
    ordering = ('-created_at',)


@admin.register(RestaurantMenu)
class RestaurantMenuAdmin(admin.ModelAdmin):
    list_display = ('restaurant', 'menu')
    search_fields = ('restaurant__name',)

