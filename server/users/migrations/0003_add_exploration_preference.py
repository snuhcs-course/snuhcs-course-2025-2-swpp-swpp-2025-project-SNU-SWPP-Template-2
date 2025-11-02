# Generated manually for exploration_preference field

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('users', '0002_user_created_at_user_nickname_userpreference_and_more'),
    ]

    operations = [
        migrations.AddField(
            model_name='userpreference',
            name='exploration_preference',
            field=models.FloatField(default=2.5, help_text='0=familiar foods, 5=adventurous'),
        ),
    ]

