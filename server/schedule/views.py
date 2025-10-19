from django.shortcuts import render
from django.http import HttpResponse
from django.views.decorators.csrf import csrf_exempt
from .models import Schedule
# Create your views here.
@csrf_exempt
def schedule_list(request):
    if(request.method=='GET'):
        schedules = Schedule.objects.all()
        return JsonResponse(list(schedules.values()), safe=False, status=200)

    elif(request.method=='POST'):
        userId = User.objects.get(id=request.POST.get('userId'))
        sessionId = Session.objects.get(id=request.POST.get('sessionId'))
        date = request.POST.get('date')
        startTime = request.POST.get('startTime')
        endTime = request.POST.get('endTime')
        name=sessionId.title
        isFinished = false

        schedule = Schedule(
            userId_id=userId,
            sessionId_id=sessionId,
            date=date,
            startTime=startTime,
            endTime=endTime,
            name=name,
            isFinished=isFinished
        )
        schedule.save()
        return JsonResponse({'message': 'Schedule created successfully'}, status=201)
    elif (request.method=='DELETE'):
        schedule_id=request.GET.get('date')
        schedule=Schedule.objects.get(date=schedule_id)
        schedule.delete()
        return JsonResponse({'message': 'Schedule deleted successfully'}, status=200)
    else:
        return JsonResponse({'error': 'Method not allowed'}, status=405)
