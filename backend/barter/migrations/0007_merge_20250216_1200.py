from django.db import migrations


class Migration(migrations.Migration):
    """
    Merge migration to join the BookCopy refactor with later barter updates.
    """

    dependencies = [
        ("barter", "0003_alter_bartercounter_offered_books_and_more"),
        ("barter", "0006_alter_barterrequest_id"),
    ]

    operations = []
