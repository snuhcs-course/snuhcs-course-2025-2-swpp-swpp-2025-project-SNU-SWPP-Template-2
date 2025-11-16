from django.db.models import Q
from rest_framework import status
from rest_framework.decorators import api_view, permission_classes
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response

from .models import BookCopy
from .serializers import BookSerializer


@api_view(["GET"])
@permission_classes([IsAuthenticated])
def book_search(request):
    query = request.query_params.get("q", "").strip()
    if not query:
        return Response(
            {"error": "Missing query parameter 'q'."},
            status=status.HTTP_400_BAD_REQUEST,
        )

    books = (
        BookCopy.objects.select_related("publication", "owner")
        .prefetch_related("publication__authors")
        .filter(
            Q(publication__title__icontains=query)
            | Q(publication__authors__name__icontains=query)
        )
        .distinct()
    )

    if books.exists():
        serializer = BookSerializer(books, many=True)
        return Response(serializer.data)

    return Response(
        {"message": f"Not found '{query}'."},
        status=status.HTTP_404_NOT_FOUND,
    )
