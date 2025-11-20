import django.db.models.deletion
from django.db import migrations, models


class Migration(migrations.Migration):
    """
    Add proposed book IDs list and refresh help text for offered_book.
    """

    dependencies = [
        ("barter", "0008_alter_barterrequest_bookcopy_refs"),
    ]

    operations = [
        migrations.AddField(
            model_name="barterrequest",
            name="offered_book_ids",
            field=models.JSONField(
                blank=True,
                default=list,
                help_text="List of 3 book IDs proposed by requester",
            ),
        ),
        migrations.AlterField(
            model_name="barterrequest",
            name="offered_book",
            field=models.ForeignKey(
                blank=True,
                help_text="Book copy offered by the requester (final selection by recipient)",
                null=True,
                on_delete=django.db.models.deletion.CASCADE,
                related_name="offered_in_barters",
                to="books.bookcopy",
            ),
        ),
    ]
