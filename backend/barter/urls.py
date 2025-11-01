"""
URL configuration for the barter app.
"""
from django.urls import path
from . import views

urlpatterns = [
    path("requests/", views.barter_request_list_create, name="barter-request-list-create"),
    path("requests/<int:barter_id>/", views.barter_request_detail, name="barter-request-detail"),
    path("requests/<int:barter_id>/action/", views.barter_request_action, name="barter-request-action"),
    path("transactions/", views.list_transactions, name="list-transaction"),
    path("transactions/<int:barter_id>/", views.transaction_detail, name="transaction-detail"),
    path("ratings/<int:barter_id>/", views.rate_transaction, name="rate-transaction"),
]