from django.apps import AppConfig


class PsqlDataConfig(AppConfig):
    default_auto_field = 'django.db.models.BigAutoField'
    name = 'psql_data'
    verbose_name = 'PostgreSQL Data'