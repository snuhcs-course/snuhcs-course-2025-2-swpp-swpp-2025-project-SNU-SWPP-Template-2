"""
Views for the barter app.
"""

from django.utils import timezone
from rest_framework import permissions, status
from rest_framework.decorators import api_view, permission_classes
from rest_framework.response import Response

from django.contrib.auth import get_user_model

from barter.models import BarterRequest
from barter.serializers import (
    BarterAcceptSerializer,
    BarterRejectSerializer,
    BarterRequestSerializer,
)
from books.models import Book
from notify.models import Notification

User = get_user_model()


@api_view(["POST"])
@permission_classes([permissions.IsAuthenticated])
def create_barter_request(request):
    """
    Create a 1:1 barter request from requester to a specific user.
    Requester offers one book in exchange for one requested book.
    
    POST /barter/requests/create/
    Body:
      - recipient_id: int (user ID)
      - requested_book_id: uuid (book ID from recipient)
      - offered_book_id: uuid (book ID from requester)
      - message: str (optional)
      - preferred_meeting_type: str (optional)
      - proposed_meeting_location: str (optional)
      - proposed_meeting_time: datetime (optional)
    """
    recipient_id = request.data.get("recipient_id")
    requested_book_id = request.data.get("requested_book_id")
    offered_book_id = request.data.get("offered_book_id")

    if not recipient_id or not requested_book_id or not offered_book_id:
        return Response(
            {"error": "recipient_id, requested_book_id, and offered_book_id are required"},
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

    try:
        offered_book = Book.objects.get(pk=offered_book_id)
    except Book.DoesNotExist:
        return Response(
            {"error": "Offered book not found"},
            status=status.HTTP_404_NOT_FOUND,
        )

    # Verify ownership
    if offered_book.owner_id != request.user.id:
        return Response(
            {"error": "You can only offer books you own"},
            status=status.HTTP_403_FORBIDDEN,
        )

    if requested_book.owner_id != recipient_id:
        return Response(
            {"error": "Requested book must belong to recipient"},
            status=status.HTTP_400_BAD_REQUEST,
        )

    # Build message with requester's info
    msg = request.data.get("message")
    if not msg:
        requester = request.user
        parts = [
            f"Hi {recipient.username}, I'd like to trade my '{offered_book.title}' "
            f"for your '{requested_book.title}'."
        ]
        if hasattr(requester, "taste") and requester.taste and requester.taste.trade_place_name:
            parts.append(
                f"Preferred place: {requester.taste.trade_place_name} ({requester.taste.trade_address or 'N/A'})"
            )
        if requester.latitude and requester.longitude:
            parts.append(f"My location: lat {requester.latitude}, lng {requester.longitude}")
        msg = "\n".join(parts)

    # Create barter request with 1:1 exchange
    barter = BarterRequest.objects.create(
        requester=request.user,
        recipient=recipient,
        offered_book=offered_book,
        requested_book=requested_book,
        message=msg,
        preferred_meeting_type=request.data.get("preferred_meeting_type", "in_person"),
        proposed_meeting_location=request.data.get("proposed_meeting_location", ""),
        proposed_meeting_time=request.data.get("proposed_meeting_time"),
    )

    # Notify recipient
    Notification.objects.create(
        recipient=recipient,
        sender=request.user,
        notification_type="barter_request",
        title="New barter request",
        message=f"{request.user.username} wants to trade for '{requested_book.title}'.",
        content_object=barter,
    )

    serializer = BarterRequestSerializer(barter, context={"request": request})
    return Response({"barter": serializer.data}, status=status.HTTP_201_CREATED)


@api_view(["POST"])
@permission_classes([permissions.IsAuthenticated])
def accept_barter_request(request, request_id):
    """
    Accept a barter request.
    
    POST /barter/requests/<uuid:request_id>/accept/
    Optional body:
      - response_message: str
      - proposed_meeting_time: datetime
      - proposed_meeting_location: str
    """
    try:
        barter_request = BarterRequest.objects.select_related(
            "requester", "recipient"
        ).get(pk=request_id)
    except BarterRequest.DoesNotExist:
        return Response(
            {"error": "Barter request not found"},
            status=status.HTTP_404_NOT_FOUND,
        )

    # Only recipient can accept
    if barter_request.recipient_id != request.user.id:
        return Response(
            {"error": "Only the recipient can accept this request"},
            status=status.HTTP_403_FORBIDDEN,
        )

    # Check if already responded
    if barter_request.status != "pending":
        return Response(
            {"error": f"Request already {barter_request.status}"},
            status=status.HTTP_400_BAD_REQUEST,
        )

    serializer = BarterAcceptSerializer(data=request.data)
    if not serializer.is_valid():
        return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)

    # Update barter request
    barter_request.status = "accepted"
    barter_request.response_message = serializer.validated_data.get(
        "response_message", ""
    )
    barter_request.response_date = timezone.now()
    
    # Optionally update meeting details from recipient
    if "proposed_meeting_time" in serializer.validated_data:
        barter_request.proposed_meeting_time = serializer.validated_data[
            "proposed_meeting_time"
        ]
    if "proposed_meeting_location" in serializer.validated_data:
        barter_request.proposed_meeting_location = serializer.validated_data[
            "proposed_meeting_location"
        ]
    
    barter_request.save()

    # Notify requester
    Notification.objects.create(
        recipient=barter_request.requester,
        sender=request.user,
        notification_type="barter_accepted",
        title="Barter request accepted",
        message=f"{request.user.username} accepted your barter request!",
        content_object=barter_request,
    )

    result_serializer = BarterRequestSerializer(
        barter_request, context={"request": request}
    )
    return Response({"barter": result_serializer.data}, status=status.HTTP_200_OK)


@api_view(["POST"])
@permission_classes([permissions.IsAuthenticated])
def reject_barter_request(request, request_id):
    """
    Reject a barter request.
    
    POST /barter/requests/<uuid:request_id>/reject/
    Optional body:
      - response_message: str
    """
    try:
        barter_request = BarterRequest.objects.select_related(
            "requester", "recipient"
        ).get(pk=request_id)
    except BarterRequest.DoesNotExist:
        return Response(
            {"error": "Barter request not found"},
            status=status.HTTP_404_NOT_FOUND,
        )

    # Only recipient can reject
    if barter_request.recipient_id != request.user.id:
        return Response(
            {"error": "Only the recipient can reject this request"},
            status=status.HTTP_403_FORBIDDEN,
        )

    # Check if already responded
    if barter_request.status != "pending":
        return Response(
            {"error": f"Request already {barter_request.status}"},
            status=status.HTTP_400_BAD_REQUEST,
        )

    serializer = BarterRejectSerializer(data=request.data)
    if not serializer.is_valid():
        return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)

    # Update barter request
    barter_request.status = "rejected"
    barter_request.response_message = serializer.validated_data.get(
        "response_message", ""
    )
    barter_request.response_date = timezone.now()
    barter_request.save()

    # Notify requester
    Notification.objects.create(
        recipient=barter_request.requester,
        sender=request.user,
        notification_type="barter_rejected",
        title="Barter request declined",
        message=f"{request.user.username} declined your barter request.",
        content_object=barter_request,
    )

    result_serializer = BarterRequestSerializer(
        barter_request, context={"request": request}
    )
    return Response({"barter": result_serializer.data}, status=status.HTTP_200_OK)
