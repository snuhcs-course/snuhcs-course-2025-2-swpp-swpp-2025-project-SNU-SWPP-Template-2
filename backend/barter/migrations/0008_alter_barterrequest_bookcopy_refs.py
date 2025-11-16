import django.db.models.deletion
from django.db import migrations, models


class Migration(migrations.Migration):
    """
    Point barter request book references to the new BookCopy model.
    """

    dependencies = [
        ("barter", "0007_merge_20250216_1200"),
        ("books", "0005_remove_book_authors_remove_book_genres_and_more"),
    ]

    operations = [
        migrations.AlterField(
            model_name="barterrequest",
            name="offered_book",
            field=models.ForeignKey(
                blank=True,
                help_text="Book copy offered by the requester",
                null=True,
                on_delete=django.db.models.deletion.CASCADE,
                related_name="offered_in_barters",
                to="books.bookcopy",
            ),
        ),
        migrations.AlterField(
            model_name="barterrequest",
            name="requested_book",
            field=models.ForeignKey(
                blank=True,
                help_text="Book copy requested from the recipient",
                null=True,
                on_delete=django.db.models.deletion.CASCADE,
                related_name="requested_in_barters",
                to="books.bookcopy",
            ),
        ),
    ]
