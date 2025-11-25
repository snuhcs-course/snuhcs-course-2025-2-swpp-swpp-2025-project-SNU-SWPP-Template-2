from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ("books", "0006_delete_book_and_more"),
    ]

    operations = [
        migrations.AlterField(
            model_name="bookpublication",
            name="external_url",
            field=models.URLField(
                blank=True,
                max_length=500,
                help_text="URL to the book detail page on external API (e.g., Kakao)",
            ),
        ),
    ]
