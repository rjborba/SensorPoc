package com.moto_summerjobs.busevents;

import android.util.Log;

import java.util.LinkedList;
import java.util.List;

/**
 * Defines if event on ACC should be considered a valid event
 */
public class OnGoingAccEvent {
//    Parameters of config:
//    Every parameter used to define event as a Hard brake must be declared below

    //Minimal amount of samples to consider event valid
    private static final int MIN_SAMPLES = 10;
    //Minimal G force to consider event valid
    private static final long MIN_G_AVG = 3;
    //Minimal event time in milliseconds to consider event valid
    private static final long MIN_EVENT_TIME = 200L;


    private boolean eventStarted = false;
    private long initialEventTime;
    private long finalEventTime;
    private List<Double> eventGList;

    private double avgOfG = 0;
    private double maxG = 0;
    private int numberOfSamples = 0;
    private long totalEventTimeInMillisec = 0;

    public OnGoingAccEvent(){
        eventGList = new LinkedList<>();
    }


    public void startEvent(long initialTime){
        if(eventStarted == false){
            initialEventTime = initialTime;
            Log.d("eventStart", "time: " + initialTime);
            eventStarted = true;
            eventGList.clear();
            maxG = -1;
            avgOfG = -1;
            numberOfSamples = -1;
            totalEventTimeInMillisec = -1;
        }
    }

    public void updateCurrentG(double newG){
        eventGList.add(newG);
    }

    public void finishEvent(long finalTime){
        if(eventStarted == true){
            finalEventTime = finalTime;
            afterEventProcess();
            Log.d("eventEnded", "time: " + finalEventTime);
            Log.d("eventSummary", "Event took: " + this.totalEventTimeInMillisec + "ms"
                    + "\n " + "Max G: " + this.maxG
                    + "\n " + "AVG of G: " + this.avgOfG
                    + "\n " + "Number of samples: " + this.numberOfSamples
                    + "\n " + "Is valid event: " + this.isHardBrakeValidEvent());
            eventStarted = false;
        }
    }

    public boolean isEventStarted(){
        return eventStarted;
    }

    /**
     * Analyzes using config parameters (samples, time and avg) if the event should be considered
     * as a hard break
     *
     * @return TRUE is event is a valid hard brake. FALSE otherwise
     */
    public boolean isHardBrakeValidEvent(){
        return this.numberOfSamples >= MIN_SAMPLES
                && this.totalEventTimeInMillisec >= MIN_EVENT_TIME
                && this.avgOfG >= MIN_G_AVG;
    }

    private void afterEventProcess() {
        double sumOfG = 0;
        double maxG = 0;
        for (Double currentG : eventGList) {
            sumOfG = sumOfG + currentG;

            if (currentG > maxG) {
                maxG = currentG;
            }
        }

        this.maxG = maxG;
        this.avgOfG = sumOfG / eventGList.size();
        this.numberOfSamples = eventGList.size();
        //Divided by 100000 because of event time is given in nanoseconds
        this.totalEventTimeInMillisec = (finalEventTime/1000000) - (initialEventTime/1000000);
    }
}
