from rest_framework import serializers
from .models import EquipmentRecommendation

class EquipmentRecommendationSerializer(serializers.ModelSerializer):
    class Meta:
        model = EquipmentRecommendation
        fields = '__all__'
