from rest_framework import status
from rest_framework.response import Response
from rest_framework.permissions import IsAuthenticated
from rest_framework.views import APIView
from notify import create_notification
from .models import BarterRequest, BarterTransaction, BarterCounter, BarterRating
from .serializers import (
    BarterRequestSerializer,
    BarterRequestCreateSerializer,
    BarterTransactionSerializer,
    BarterCounterSerializer,
    BarterRatingSerializer,
)

@api_view(["GET", "POST"])
@permission_classes([IsAuthenticated])
def barter_request_list_create(request):

    """
    GET: List barter requests sent by the authenticated user.
    POST: Send a new barter request to another user.
    """

    user = request.user

    #GET /barter/requests/?type=sent
    #GET /barter/requests/?type=received

    if request.method == "GET":
        req_type = request.query_params.get("type")  # 'sent' or 'received'

        if req_type == "sent":
            barter_requests = BarterRequest.objects.filter(requester=user)
        elif req_type == "received":
            barter_requests = BarterRequest.objects.filter(recipient=user)
        else:
            # default: show both
            sent_requests = BarterRequest.objects.filter(requester=user)
            received_requests = BarterRequest.objects.filter(recipient=user)
            return Response({
                "sent_requests": BarterRequestSerializer(sent_requests, many=True).data,
                "received_requests": BarterRequestSerializer(received_requests, many=True).data,
            })

        serializer = BarterRequestSerializer(barter_requests, many=True)
        return Response(serializer.data)

    elif request.method == "POST":
        serializer = BarterRequestCreateSerializer(
            data=request.data,
            context={"request": request}
        )
        if serializer.is_valid():
            barter_request = serializer.save(requester=request.user)
            
            create_notification(request, 'barter_request', barter_id=barter_request.id)

            return Response(serializer.data, status=status.HTTP_201_CREATED)
        return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)


@api_view(["GET", "PUT", "PATCH"])
@permission_classes([IsAuthenticated])
def barter_request_detail(request, barter_id):
    try:
        barter_request = BarterRequest.objects.get(id=barter_id)
    except BarterRequest.DoesNotExist:
        return Response({"detail": "Barter request not found."}, status=404)

    if request.user not in [barter_request.requester, barter_request.recipient]:
        return Response({"detail": "Not authorized."}, status=403)

    if request.method == "GET":
        serializer = BarterRequestSerializer(barter_request)
        return Response(serializer.data)

    elif request.method == "PUT":
        if barter_request.status != "pending" or request.user != barter_request.requester:
            return Response({"detail": "Cannot edit this request."}, status=403)
        serializer = BarterRequestCreateSerializer(barter_request, data=request.data)
        if serializer.is_valid():
            serializer.save()
            return Response(serializer.data)
        return Response(serializer.errors, status=400)

    elif request.method == "PATCH":
        serializer = BarterRequestUpdateSerializer(barter_request, data=request.data, partial=True)
        if serializer.is_valid():
            serializer.save()
            return Response(serializer.data)
        return Response(serializer.errors, status=400)


"""
This function implements accept, reject, or cancel logic.
Accept -> user accepts the barter request and is ready to exchange books
Reject -> user doesn't want to accept the request
Cancel -> user cancels his/her request before tranaction
"""
@api_view(["POST"])
@permission_classes([IsAuthenticated])
def barter_request_action(request, barter_id, action):
    try:
        barter_request = BarterRequest.objects.get(id=barter_id)
    except BarterRequest.DoesNotExist:
        return Response({"detail": "Not found."}, status=status.HTTP_404_NOT_FOUND)

    user = request.user

    # --- Accept ---
    if action == "accept":
        if barter_request.recipient != user:
            return Response({"detail": "Only the recipient can accept this request."}, status=403)
        barter_request.status = "accepted"
        barter_request.save()
        barter_request.create_transaction()

        create_notification(request, "barter_accepted", barter_id=barter_id)

        return Response({"status": "Request accepted and transaction created."})

    # --- Reject ---
    elif action == "reject":
        if barter_request.recipient != user:
            return Response({"detail": "Only the recipient can reject this request."}, status=403)
        barter_request.status = "rejected"
        barter_request.save()

        create_notification(request, "barter_rejected", barter_id=barter_id)

        return Response({"status": "Request rejected."})

    # --- Cancel ---
    elif action == "cancel":
        if barter_request.requester != user:
            return Response({"detail": "Only the requester can cancel this request."}, status=403)
        barter_request.status = "cancelled"
        barter_request.save()
        return Response({"status": "Request cancelled."})

    return Response({"detail": "Invalid action."}, status=status.HTTP_400_BAD_REQUEST)

"""
user creates counter offer
"""
@api_view(["POST"])
@permission_classes([IsAuthenticated])
def create_counter_offer(request, barter_id):
    try:
        original_request = BarterRequest.objects.get(id=barter_id)
    except BarterRequest.DoesNotExist:
        return Response({"detail": "Original request not found."}, status=status.HTTP_404_NOT_FOUND)

    serializer = BarterCounterSerializer(data=request.data)
    if serializer.is_valid():
        counter_offer = serializer.save(counter_by=request.user, original_request=original_request)
        create_notification(request, 'barter_counter', barter_id=counter_offer.id)
        return Response(serializer.data, status=status.HTTP_201_CREATED)
    return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)

@api_view(["POST"])
@permission_classes([IsAuthenticated])
def complete_barter(request, barter_id):
    """
    Endpoint for requester or recipient to confirm barter completion.
    Once both confirm, transaction is marked as completed.
    """
    try:
        barter_request = BarterRequest.objects.get(id=barter_id)
    except BarterRequest.DoesNotExist:
        return Response({"detail": "Barter not found."}, status=status.HTTP_404_NOT_FOUND)

    # Ensure the user is part of the barter
    if request.user not in [barter_request.requester, barter_request.recipient]:
        return Response({"detail": "You are not part of this barter."}, status=status.HTTP_403_FORBIDDEN)

    # Get or create the transaction record
    transaction, created = BarterTransaction.objects.get_or_create(barter_request=barter_request)

    # Update confirmation based on which user is confirming
    if request.user == barter_request.requester:
        transaction.completion_confirmed_by_requester = True
    elif request.user == barter_request.recipient:
        transaction.completion_confirmed_by_recipient = True

    # If both confirmed → mark as completed
    if transaction.is_completed:
        transaction.status = "completed"
        transaction.completed_date = timezone.now()
        transaction.save()

        # Notify both users
        create_notification(request, "barter_completed", barter_id=barter_request.id)

        return Response(
            {"message": "Barter fully completed. Both parties confirmed."},
            status=status.HTTP_200_OK
        )

    transaction.save()

    return Response(
        {"message": "Your completion has been recorded. Waiting for the other user to confirm."},
        status=status.HTTP_200_OK
    )

"""
list all transactions
"""
@api_view(["GET"])
@permission_classes([IsAuthenticated])
def list_transactions(request):
    user = request.user
    transactions = BarterTransaction.objects.filter(
        Q(barter_request__requester=user) | Q(barter_request__recipient=user)
    )
    serializer = BarterTransactionSerializer(transactions, many=True)
    return Response(serializer.data)


"""
list a single transaction using their pk.
"""
@api_view(["GET"])
@permission_classes([IsAuthenticated])
def transaction_detail(request, barter_id):
    try:
        transaction = BarterTransaction.objects.get(id=barter_id)
    except BarterTransaction.DoesNotExist:
        return Response({"detail": "Transaction not found."}, status=status.HTTP_404_NOT_FOUND)

    serializer = BarterTransactionSerializer(transaction)
    return Response(serializer.data)


"""
transaction rating
"""
@api_view(["POST"])
@permission_classes([IsAuthenticated])
def rate_transaction(request, barter_id):
    try:
        transaction = BarterTransaction.objects.get(id=barter_id)
    except BarterTransaction.DoesNotExist:
        return Response({"detail": "Transaction not found."}, status=status.HTTP_404_NOT_FOUND)

    # Determine who is being rated
    rated_user = (
        transaction.barter_request.recipient
        if transaction.barter_request.requester == request.user
        else transaction.barter_request.requester
    )

    serializer = BarterRatingSerializer(data=request.data)
    if serializer.is_valid():
        serializer.save(transaction=transaction, rater=request.user, rated_user=rated_user)
        return Response(serializer.data, status=status.HTTP_201_CREATED)
    return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)
