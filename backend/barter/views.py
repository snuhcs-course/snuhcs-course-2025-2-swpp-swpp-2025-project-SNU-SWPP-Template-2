"""
Views for the barter app.
"""

from django.utils import timezone
from django.db import transaction
from rest_framework import permissions, status
from rest_framework.decorators import api_view, permission_classes
from rest_framework.response import Response

from django.contrib.auth import get_user_model

from barter.models import BarterRequest
from barter.serializers import (
    BarterAcceptSerializer,
    BarterRejectSerializer,
    BarterRequestSerializer,
    BarterCreateSerializer,
)
from books.models import Book
from books.serializers import BookSummarySerializer
from accounts.serializers import UserBarterInfoSerializer
from notify.models import Notification
from notify.models import Notification

User = get_user_model()


@api_view(["GET"])
@permission_classes([permissions.IsAuthenticated])
def get_barter_request_detail(request, request_id):
    """
    Get barter request detail for approval screen.
    Returns requester info and 3 proposed books with messages.
    
    GET /barter/requests/<uuid:request_id>/
    """
    try:
        barter_request = BarterRequest.objects.select_related(
            "requester", "recipient", "requested_book"
        ).get(pk=request_id)
    except BarterRequest.DoesNotExist:
        return Response(
            {"error": "Barter request not found"},
            status=status.HTTP_404_NOT_FOUND,
        )

    # Check permission - only requester or recipient can view
    if request.user.id not in [barter_request.requester_id, barter_request.recipient_id]:
        return Response(
            {"error": "You don't have permission to view this request"},
            status=status.HTTP_403_FORBIDDEN,
        )

    # Get the 3 offered books
    offered_books = Book.objects.filter(
        id__in=barter_request.offered_book_ids
    ).select_related('publisher').prefetch_related('authors', 'genres')
    
    books_data = BookSummarySerializer(offered_books, many=True).data
    
    # Split messages (3 messages separated by \n---\n)
    messages = barter_request.message.split("\n---\n") if barter_request.message else []

    user_serializer = UserBarterInfoSerializer(barter_request.requester)
    
    response_data = {
        "id": str(barter_request.id),
        "requesterName": barter_request.requester.username,
        "requesterAvatarUrl": user_serializer.data.get('profile_picture'),
        "createdAt": barter_request.created_at.isoformat(),
        "books": books_data,
        "message": messages,
    }

    return Response(response_data, status=status.HTTP_200_OK)

@api_view(["POST"])
@permission_classes([permissions.IsAuthenticated])
def create_barter_request(request):
    """
    Create a new barter request.
    Requester (A) selects recipient's (B) book.
    Backend automatically selects 3 of A's available books and generates default messages.
    
    POST /barter/requests/create/
    Body:
      - recipient_id: int (user ID)
      - requested_book_id: uuid (book ID from recipient B)
    """
    serializer = BarterCreateSerializer(data=request.data)
    if not serializer.is_valid():
        return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)

    validated_data = serializer.validated_data
    recipient_id = request.data.get("recipient_id")
    requested_book_id = validated_data["requested_book_id"]

    if not recipient_id:
        return Response(
            {"error": "recipient_id is required"},
            status=status.HTTP_400_BAD_REQUEST,
        )

    try:
        recipient = User.objects.get(pk=recipient_id)
    except User.DoesNotExist:
        return Response(
            {"error": "Recipient user not found"},
            status=status.HTTP_404_NOT_FOUND,
        )

    try:
        requested_book = Book.objects.get(pk=requested_book_id)
    except Book.DoesNotExist:
        return Response(
            {"error": "Requested book not found"},
            status=status.HTTP_404_NOT_FOUND,
        )

    if requested_book.owner_id != recipient_id:
        return Response(
            {"error": "Requested book must belong to recipient"},
            status=status.HTTP_400_BAD_REQUEST,
        )

    if not requested_book.is_for_barter or requested_book.trade_status != "available":
        return Response(
            {"error": "Requested book is not available for barter"},
            status=status.HTTP_400_BAD_REQUEST,
        )

    if requested_book.owner_id == request.user.id:
        return Response(
            {"error": "Cannot request your own book"},
            status=status.HTTP_403_FORBIDDEN,
        )

    # Automatically select up to 3 available books from requester
    available_books = Book.objects.filter(
        owner=request.user,
        is_for_barter=True,
        trade_status="available"
    ).order_by('?')[:3]  # Random up to 3 books

    offered_books = list(available_books)
    
    if not offered_books:
        return Response(
            {"error": "You need at least 1 book available for barter."},
            status=status.HTTP_400_BAD_REQUEST,
        )
    
    # Generate default messages for each book (up to 3)
    message_templates = [
        "I'd like to offer '{}' for exchange.",
        "Would you be interested in '{}'?",
        "This is '{}' from my collection."
    ]
    
    messages = []
    for i, book in enumerate(offered_books):
        template = message_templates[i % len(message_templates)]
        messages.append(template.format(book.title))

    # Mark all books as not_available
    requested_book.trade_status = "not_available"
    requested_book.save(update_fields=["trade_status"])
    
    for book in offered_books:
        book.trade_status = "not_available"
        book.save(update_fields=["trade_status"])

    # Create barter request with messages stored as JSON
    barter = BarterRequest.objects.create(
        requester=request.user,
        recipient=recipient,
        requested_book=requested_book,
        offered_book_ids=[str(b.id) for b in offered_books],
        message="\n---\n".join(messages),  # Store 3 messages separated
        status="pending",
    )

    # Notify recipient
    Notification.objects.create(
        recipient=recipient,
        notification_type="barter_request",
        message=f"{request.user.username} wants to trade for '{requested_book.title}'.",
        content_object=barter,
    )

    serializer = BarterRequestSerializer(barter, context={"request": request})
    return Response({"barter": serializer.data}, status=status.HTTP_201_CREATED)


@api_view(["POST"])
@permission_classes([permissions.IsAuthenticated])
def accept_book_for_counter_propose(request, request_id, book_id):
    """
    Recipient (B) accepts one of the 3 proposed books.
    This completes the barter transaction.
    
    POST /barter/requests/<uuid:request_id>/accept/<uuid:book_id>/
    """
    try:
        barter_request = BarterRequest.objects.select_related(
            "requester", "recipient", "requested_book"
        ).get(pk=request_id)
    except BarterRequest.DoesNotExist:
        return Response(
            {"error": "Barter request not found"},
            status=status.HTTP_404_NOT_FOUND,
        )

    # Verify the user is the recipient
    if request.user.id != barter_request.recipient_id:
        return Response(
            {"error": "Only the recipient can accept a book"},
            status=status.HTTP_403_FORBIDDEN,
        )

    # Verify request is still pending
    if barter_request.status != "pending":
        return Response(
            {"error": f"Request is already {barter_request.status}"},
            status=status.HTTP_400_BAD_REQUEST,
        )

    # Verify the selected book is one of the 3 offered books
    book_id_str = str(book_id)
    if book_id_str not in barter_request.offered_book_ids:
        return Response(
            {"error": "Selected book is not in the proposed books"},
            status=status.HTTP_400_BAD_REQUEST,
        )

    try:
        selected_book = Book.objects.get(pk=book_id)
    except Book.DoesNotExist:
        return Response(
            {"error": "Selected book not found"},
            status=status.HTTP_404_NOT_FOUND,
        )

    # Verify book is still available
    if selected_book.trade_status != "available":
        return Response(
            {"error": "Selected book is no longer available"},
            status=status.HTTP_400_BAD_REQUEST,
        )

    requested_book = barter_request.requested_book

    # Verify requested book is still available
    if requested_book.trade_status != "available":
        return Response(
            {"error": "Your book is no longer available for barter"},
            status=status.HTTP_400_BAD_REQUEST,
        )

    with transaction.atomic():
        # Exchange ownership
        original_requester = barter_request.requester
        original_recipient = barter_request.recipient

        selected_book.owner = original_recipient
        selected_book.is_for_barter = False
        selected_book.trade_status = "traded"
        selected_book.save()

        requested_book.owner = original_requester
        requested_book.is_for_barter = False
        requested_book.trade_status = "traded"
        requested_book.save()

        # Restore the 2 non-selected books to available status
        non_selected_book_ids = [
            bid for bid in barter_request.offered_book_ids if bid != book_id_str
        ]
        Book.objects.filter(id__in=non_selected_book_ids).update(
            trade_status="available"
        )

        # Update the barter request
        barter_request.offered_book = selected_book
        barter_request.status = "completed"
        barter_request.completed_date = timezone.now()
        barter_request.save()

        # Create notifications for both users
        Notification.objects.create(
            recipient=original_requester,
            notification_type="barter_accepted",
            message=f"{original_recipient.username} accepted your barter proposal",
            content_object=barter_request,
        )

        Notification.objects.create(
            recipient=original_recipient,
            notification_type="barter_completed",
            message=f"Barter with {original_requester.username} completed",
            content_object=barter_request,
        )

    return Response(
        {"message": "Barter accepted and completed successfully"},
        status=status.HTTP_200_OK,
    )


@api_view(["POST"])
@permission_classes([permissions.IsAuthenticated])
def reject_barter_request(request, request_id):
    """
    Reject a barter request (can be done by recipient or requester).
    Restores availability of all books (1 requested + 3 offered) to 'available'.
    
    POST /barter/requests/<uuid:request_id>/reject/
    Optional body:
      - response_message: str
    """
    try:
        barter_request = BarterRequest.objects.select_related(
            "requester", "recipient", "requested_book"
        ).get(pk=request_id)
    except BarterRequest.DoesNotExist:
        return Response(
            {"error": "Barter request not found"},
            status=status.HTTP_404_NOT_FOUND,
        )

    # Both requester and recipient can reject
    if barter_request.recipient_id != request.user.id and barter_request.requester_id != request.user.id:
        return Response(
            {"error": "Only the requester or recipient can reject this request"},
            status=status.HTTP_403_FORBIDDEN,
        )

    # Check if already finalized
    if barter_request.status in ["completed", "rejected"]:
        return Response(
            {"error": f"Request already {barter_request.status}"},
            status=status.HTTP_400_BAD_REQUEST,
        )

    serializer = BarterRejectSerializer(data=request.data)
    if not serializer.is_valid():
        return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)

    with transaction.atomic():
        # Restore requested book trade_status
        if barter_request.requested_book:
            barter_request.requested_book.trade_status = "available"
            barter_request.requested_book.save(update_fields=["trade_status"])

        # Restore all 3 offered books trade_status
        Book.objects.filter(id__in=barter_request.offered_book_ids).update(
            trade_status="available"
        )

        # Update barter request
        barter_request.status = "rejected"
        barter_request.response_message = serializer.validated_data.get(
            "response_message", ""
        )
        barter_request.response_date = timezone.now()
        barter_request.save()

        # Notify the other party
        other_user = barter_request.requester if request.user.id == barter_request.recipient_id else barter_request.recipient
        Notification.objects.create(
            recipient=other_user,
            notification_type="barter_rejected",
            message=f"{request.user.username} declined the barter request.",
            content_object=barter_request,
        )

    result_serializer = BarterRequestSerializer(
        barter_request, context={"request": request}
    )
    return Response({"barter": result_serializer.data}, status=status.HTTP_200_OK)
