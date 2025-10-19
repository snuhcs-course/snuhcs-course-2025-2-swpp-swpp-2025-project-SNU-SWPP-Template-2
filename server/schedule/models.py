from django.db import models

# Create your models here.
class Session(models.Model):
    userId=models.ManyToManyField('User',related_name='sessions')
    title = models.CharField(max_length=200)
    description = models.TextField()
    sport=models.ForeignKey('Sport', on_delete=models.CASCADE,related_name='sessions')
    difficulty_level = models.CharField(max_length=50)
    length=models.IntegerField(help_text="Length in minutes")
    previous_session=models.ForeignKey('self', on_delete=models.SET_NULL, null=True, blank=True, related_name='next_sessions')


    def __str__(self):
        return self.title
class User(models.Model):
    username = models.CharField(max_length=150, unique=True)
    email = models.EmailField(unique=True)
    password = models.CharField(max_length=128)


    def __str__(self):
        return self.username

class Schedule(models.Model):
    userId=models.ForeignKey(User, on_delete=models.CASCADE,related_name='schedules')
    sessionId=models.ForeignKey(Session, on_delete=models.CASCADE)
    date=models.DateField()
    startTime=models.TimeField()
    endTime=models.TimeField()
    name=models.CharField(max_length=200)
    isFinished=models.BooleanField(default=False)

    def __str__(self):
        return self.name
class Sport(models.Model):
    userId=models.ManyToManyField('User',related_name='sports')
    name = models.CharField(max_length=100)
    description = models.TextField()
    total_sessions=models.IntegerField()

    def __str__(self):
        return self.name
class SportStatus(models.Model):
    userId=models.ForeignKey(User, on_delete=models.CASCADE,related_name='sport_statuses')
    sportId=models.ForeignKey(Sport, on_delete=models.CASCADE)
    proficiency_level = models.CharField(max_length=50)
    session=models.ForeignKey(Session, on_delete=models.CASCADE,related_name='sport_statuses')
    last_practiced = models.DateField()

    def __str__(self):
        return f"{self.userId.username} - {self.sportId.name}"
