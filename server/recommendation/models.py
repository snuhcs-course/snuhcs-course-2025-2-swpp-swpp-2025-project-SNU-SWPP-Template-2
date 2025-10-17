from django.db import models


class RecommendationResult(models.Model):
    user = models.ForeignKey("users.User", on_delete=models.CASCADE, related_name="recommendation_results")
    query_text = models.TextField(blank=True)
    input_context = models.JSONField(default=dict, blank=True)
    recommended_menu_ids = models.JSONField(default=list, blank=True)
    scores = models.JSONField(default=list, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        indexes = [
            models.Index(fields=["user", "created_at"]),
        ]


class RecommendationRanking(models.Model):
    recommendation = models.ForeignKey(RecommendationResult, on_delete=models.CASCADE, related_name="rankings")
    menu = models.ForeignKey("menu.Menu", on_delete=models.CASCADE, related_name="recommendation_rankings")
    rank = models.PositiveIntegerField()

    class Meta:
        constraints = [
            models.UniqueConstraint(fields=["recommendation", "menu"], name="uniq_recommendation_menu"),
        ]
        indexes = [models.Index(fields=["recommendation", "rank"])]


class EmbeddingCache(models.Model):
    class EntityType(models.TextChoices):
        USER = "user", "user"
        MENU = "menu", "menu"
        IMAGE = "image", "image"
        RESTAURANT = "restaurant", "restaurant"

    entity_type = models.CharField(max_length=20, choices=EntityType.choices)
    entity_id = models.PositiveBigIntegerField()
    embedding = models.JSONField(default=list, blank=True)

    class Meta:
        constraints = [
            models.UniqueConstraint(fields=["entity_type", "entity_id"], name="uniq_embedding_entity"),
        ]
        indexes = [models.Index(fields=["entity_type", "entity_id"])]


