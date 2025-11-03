from django.db import models
from django.contrib.postgres.fields import ArrayField
from django.contrib.gis.db import models as gis_models


class DbRestaurant(models.Model):
    id = models.UUIDField(primary_key=True, db_column='id')
    external_id = models.CharField(max_length=20, unique=True, null=True, blank=True)
    name = models.TextField()
    category = models.TextField(null=True, blank=True)
    category_normalized = models.TextField(null=True, blank=True)
    phone = models.TextField(null=True, blank=True)
    address = models.TextField(null=True, blank=True)
    road_address = models.TextField(null=True, blank=True)
    group1 = models.TextField(null=True, blank=True)
    group2 = models.TextField(null=True, blank=True)
    group3 = models.TextField(null=True, blank=True)
    category_code = models.TextField(null=True, blank=True)
    category_code_list = ArrayField(models.TextField(), null=True, blank=True)
    geom = gis_models.PointField(srid=4326, null=True, blank=True)
    place_images = ArrayField(models.TextField(), null=True, blank=True)
    avg_rating = models.DecimalField(max_digits=3, decimal_places=2, null=True, blank=True)
    review_count = models.IntegerField(null=True, blank=True)
    meaningful_name = models.BooleanField(default=False)
    inferred_menu = models.TextField(default='', blank=True)
    embedding_vector = ArrayField(models.FloatField(), default=list, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = 'db_restaurants'
        verbose_name = 'Restaurant'
        verbose_name_plural = 'Restaurants'

    def __str__(self):
        return self.name


class DbMenu(models.Model):
    id = models.UUIDField(primary_key=True, db_column='id')
    restaurant = models.ForeignKey(DbRestaurant, on_delete=models.CASCADE, db_column='restaurant_id')
    external_id = models.TextField(null=True, blank=True)
    name = models.TextField()
    price = models.IntegerField(null=True, blank=True)
    description = models.TextField(null=True, blank=True)
    images = ArrayField(models.TextField(), null=True, blank=True)
    recommend = models.BooleanField(default=False)
    index_in_rest = models.IntegerField(null=True, blank=True)
    name_clean = models.TextField(null=True, blank=True)
    taste_profile = models.JSONField(null=True, blank=True)
    allergen_info = models.JSONField(null=True, blank=True)
    embedding_vector = ArrayField(models.FloatField(), default=list, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        db_table = 'db_menus'
        verbose_name = 'Menu'
        verbose_name_plural = 'Menus'

    def __str__(self):
        return f"{self.name} - {self.restaurant.name}"