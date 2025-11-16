from django.db import migrations


class Migration(migrations.Migration):
    """
    Merge migration to resolve parallel branches touching social models.
    Ensures both BookCopy-related updates and post/comment changes are applied.
    """

    dependencies = [
        ("social", "0003_alter_bookclub_current_book_and_more"),
        ("social", "0004_remove_post_social_post_author_idx_and_more"),
    ]

    operations = []
