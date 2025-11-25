from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ("books", "0007_alter_bookpublication_external_url"),
    ]

    operations = [
        migrations.AlterField(
            model_name="bookpublication",
            name="external_url",
            field=models.URLField(
                blank=True,
                max_length=1024,
                help_text="URL to the book detail page on external API (e.g., Kakao)",
            ),
        ),
    ]
