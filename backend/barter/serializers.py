from rest_framework import serializers
from .models import BarterRequest, BarterTransaction, BarterCounter, BarterRating
from books.models import Book

class BookSerializer(serializers.ModelSerializer):
    class Meta:
        model = Book
        fields = ["id", "title", "author", "isbn"]

class BarterRequestSerializer(serializers.ModelSerializer):
    requester = serializers.StringRelatedField(read_only=True)
    recipient = serializers.StringRelatedField(read_only=True)
    offered_books = BookSerializer(many=True, read_only=True)
    requested_books = BookSerializer(many=True, read_only=True)

    class Meta:
        model = BarterRequest
        fields = "__all__"
        read_only_fields = [
            "status", "response_date", "completed_date", "created_at", "updated_at"
        ]

class BarterRequestCreateSerializer(serializers.ModelSerializer):
    offered_books = serializers.PrimaryKeyRelatedField(queryset=Book.objects.all(), many=True)
    requested_books = serializers.PrimaryKeyRelatedField(queryset=Book.objects.all(), many=True)

    class Meta:
        model = BarterRequest
        fields = [
            "recipient", "offered_books", "requested_books", "message",
            "preferred_meeting_type", "proposed_meeting_location", "proposed_meeting_time"
        ]

    def create(self, validated_data):
        offered_books = validated_data.pop("offered_books")
        requested_books = validated_data.pop("requested_books")
        request = self.context["request"]

        barter_request = BarterRequest.objects.create(requester=request.user, **validated_data)
        barter_request.offered_books.set(offered_books)
        barter_request.requested_books.set(requested_books)
        return barter_request

class BarterTransactionSerializer(serializers.ModelSerializer):
    barter_request = BarterRequestSerializer(read_only=True)

    class Meta:
        model = BarterTransaction
        fields = "__all__"

class BarterCounterSerializer(serializers.ModelSerializer):
    class Meta:
        model = BarterCounter
        fields = "__all__"
        read_only_fields = ["original_request", "counter_by"]

class BarterRatingSerializer(serializers.ModelSerializer):
    class Meta:
        model = BarterRating
        fields = "__all__"
        read_only_fields = ["rater", "rated_user", "created_at"]
