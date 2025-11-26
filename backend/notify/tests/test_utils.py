import pytest
from accounts.models import Follow
from barter.models import BarterCounter, BarterRequest
from books.models import BookCopy, BookPublication, BookReview
from django.contrib.auth import get_user_model
from notify.models import Notification
from notify.utils import create_notification
from social.models import Post

User = get_user_model()


class _Request:
    def __init__(self, user: User) -> None:
        self.user = user


def _make_users():
    sender = User.objects.create_user(
        username="sender",
        email="sender@example.com",
        password="pass1234",
        first_name="Sender",
    )
    recipient = User.objects.create_user(
        username="recipient",
        email="recipient@example.com",
        password="pass1234",
        first_name="Recipient",
    )
    return sender, recipient


def _make_book(owner: User):
    publication = BookPublication.objects.create(title="Book Title")
    return BookCopy.objects.create(publication=publication, owner=owner)


@pytest.mark.django_db
def test_create_notification_for_social_actions():
    sender, recipient = _make_users()
    post = Post.objects.create(author=recipient, content="Hello world")
    review = BookReview.objects.create(
        book=_make_book(recipient),
        reviewer=recipient,
        book_title="Book Title",
        content="Great book",
        rating=5,
    )
    follow = Follow.objects.create(follower=sender, following=recipient)

    like_notification = create_notification(
        _Request(sender), "post_like", post_id=post.id
    )
    comment_notification = create_notification(
        _Request(sender), "post_comment", post_id=post.id
    )
    review_notification = create_notification(
        _Request(sender), "review_like", review_id=review.id
    )
    follow_notification = create_notification(
        _Request(sender), "new_follow", follow_id=follow.id
    )

    assert like_notification.recipient == recipient
    assert comment_notification.content_object == post
    assert review_notification.notification_type == "review_like"
    assert follow_notification.content_object == follow
    assert Notification.objects.count() == 4


@pytest.mark.django_db
def test_create_notification_for_barter_branches():
    requester, recipient = _make_users()
    barter_request = BarterRequest.objects.create(
        requester=requester,
        recipient=recipient,
        message="Trade with me",
    )
    counter = BarterCounter.objects.create(
        original_request=barter_request,
        counter_by=recipient,
        message="Different book?",
    )

    request_notif = create_notification(
        _Request(requester), "barter_request", barter_id=barter_request.id
    )
    counter_notif = create_notification(
        _Request(recipient), "barter_counter", barter_id=counter.id
    )
    completed_from_requester = create_notification(
        _Request(requester), "barter_completed", barter_id=barter_request.id
    )
    completed_from_recipient = create_notification(
        _Request(recipient), "barter_completed", barter_id=barter_request.id
    )

    assert request_notif.recipient == recipient
    assert counter_notif.recipient == requester
    assert completed_from_requester.recipient == recipient
    assert completed_from_recipient.recipient == requester


@pytest.mark.django_db
def test_invalid_type_returns_none_without_creating_notification():
    sender, _ = _make_users()

    notification = create_notification(_Request(sender), "unknown_type")

    assert notification is None
    assert Notification.objects.count() == 0
