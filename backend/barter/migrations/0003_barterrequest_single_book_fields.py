from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):

    dependencies = [
        ("barter", "0002_initial"),
        ("books", "0002_alter_bookreview_unique_together_and_more"),
    ]

    operations = [
        # Remove previous ManyToMany fields
        migrations.RemoveField(
            model_name="barterrequest",
            name="offered_books",
        ),
        migrations.RemoveField(
            model_name="barterrequest",
            name="requested_books",
        ),
        # Add single ForeignKey fields for 1:1 exchange
        migrations.AddField(
            model_name="barterrequest",
            name="offered_book",
            field=models.ForeignKey(
                to="books.book",
                on_delete=django.db.models.deletion.CASCADE,
                related_name="offered_in_barters",
                null=True,
                blank=True,
                help_text="Book offered by the requester",
            ),
        ),
        migrations.AddField(
            model_name="barterrequest",
            name="requested_book",
            field=models.ForeignKey(
                to="books.book",
                on_delete=django.db.models.deletion.CASCADE,
                related_name="requested_in_barters",
                null=True,
                blank=True,
                help_text="Book requested from the recipient",
            ),
        ),
    ]
