package com.moto_summerjobs.busevents;

public class EventHandler {
    private final float MIN_G_TO_START_EVENT = 2f;
    private final float MIN_G_TO_STOP_EVENT = 1.2f;

    private final float EARTH_ACC = 9.8f;
    private static float currentX, currentY, currentZ;
    private static long lastUpdate;
    private static float currentSpeed;

    private EventListener eventListener;

    private OnGoingAccEvent onGoingAccEvent;

    private MainActivity mainActivity;

    public EventHandler(EventListener eventListener, MainActivity mainActivity){
        this.eventListener = eventListener;
        this.mainActivity = mainActivity;
        this.onGoingAccEvent = new OnGoingAccEvent();
    }

    public void updateAccValues(float newX, float newY, float newZ, long updateTime){
        currentX = newX;
        currentY = newY;
        currentZ = newZ;
        lastUpdate = updateTime;
        process();
    }

    public void updateGpsValues(float currentSpeed){
        this.currentSpeed = currentSpeed;

        if(onGoingAccEvent.isEventPending()){
            onGoingAccEvent.updateCurrentSpeed(this.currentSpeed);
            onFinishedEvent();
        }
    }

    private void process(){
        // https://physics.stackexchange.com/questions/41653/how-do-i-get-the-total-acceleration-from-3-axes
        double accTotalInG = Math.sqrt((currentX * currentX)
        + (currentY * currentY)
        + (currentZ * currentZ)) / EARTH_ACC;

        if(accTotalInG > MIN_G_TO_START_EVENT || onGoingAccEvent.isEventStarted()){
            onGoingAccEvent.startAccEvent(lastUpdate, currentSpeed);
            onGoingAccEvent.updateCurrentG(accTotalInG);

            if(accTotalInG < MIN_G_TO_STOP_EVENT){
                onGoingAccEvent.finishAccEvent(lastUpdate);
            }
        }
    }

    private void onFinishedEvent(){
        if(onGoingAccEvent.isHardAccEvent()){
            //Not distinguishing Accelerations and brakes. Putting everything as a brake event

            this.eventListener.onEventRaised(onGoingAccEvent.getEventType());
        }
    }
}
