package com.moto_summerjobs.busevents;

import android.util.Log;

/**
 * Created by rjbf2 on 08/02/2018.
 */

public class BrakeHandler {
    private final float MIN_G_TO_START_EVENT = 2.0f;
    private final float MIN_G_TO_STOP_EVENT = 1.2f;

    private final float EARTH_ACC = 9.8f;
    private static float currentX, currentY, currentZ;
    private static long lastUpdate;

    private long onGoingEventInitTime;
    private long onGoingEventFinalTime;

    private EventListener eventListener;

    private OnGoingAccEvent onGoingAccEvent;

    private MainActivity mainActivity;

    public BrakeHandler(EventListener eventListener, MainActivity mainActivity){
        this.eventListener = eventListener;
        this.mainActivity = mainActivity;
        this.onGoingAccEvent = new OnGoingAccEvent();
    }

    public void updateValues(float newX, float newY, float newZ, long updateTime){
        currentX = newX;
        currentY = newY;
        currentZ = newZ;
        lastUpdate = updateTime;
        process();
    }

    private void process(){
        // https://physics.stackexchange.com/questions/41653/how-do-i-get-the-total-acceleration-from-3-axes
        double accTotalInG = Math.sqrt((currentX * currentX)
        + (currentY * currentY)
        + (currentZ * currentZ)) / EARTH_ACC;

        Log.d("totalAccInG", "G's: " + accTotalInG);

        if(accTotalInG > MIN_G_TO_START_EVENT || onGoingAccEvent.isEventStarted()){
            onGoingAccEvent.startEvent(lastUpdate);
            onGoingAccEvent.updateCurrentG(accTotalInG);

            if(accTotalInG < MIN_G_TO_STOP_EVENT){
                onGoingAccEvent.finishEvent(lastUpdate);

                if(onGoingAccEvent.isHardBrakeValidEvent()){
                    //Not distinguishing Accelerations and brakes. Putting everything as a brake event
                    this.eventListener.onEventRaised(EventType.HardBrake);
                }
            }
        }
    }
}
