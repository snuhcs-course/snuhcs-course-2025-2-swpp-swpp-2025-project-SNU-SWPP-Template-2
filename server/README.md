code structure
server/
├── sportify/
│   ├── __init__.py  # dummy
│   ├── asgi.py  # setting
│   ├── settings.py  # project setting
│   ├── urls.py  # url setting
|   └── wsgi.py # setting
|──schedule/
|   ├── migrations/
│   │   ├── __init__.py # dummy
│   │   ├── 0001_initial.py # first migration to DB
|   ├── __init__.py # dummy
|   ├── admin.py  # admin configuration
|   ├── apps.py # app configuration
|   ├── models.py # model class 
|   ├── tests.py # for testing
|   ├── views.py # for query