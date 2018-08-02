package com.moto_summerjobs.busevents;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements SensorEventListener, EventListener {
    private TextView tvHardBrakeCount;

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;

    private int hardBrakeIntegerCount = 0;
    private BrakeHandler brakeHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvHardBrakeCount = findViewById(R.id.hardBrakeCount);

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        brakeHandler = new BrakeHandler(this, this);
    }

    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this,
                mAccelerometer,
                SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onDestroy() {
        mSensorManager.unregisterListener(this);
        super.onDestroy();
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        //Considering only updates from accelerometer
        if (sensorEvent.sensor.getType() != Sensor.TYPE_ACCELEROMETER)
            return;

        brakeHandler.updateValues(sensorEvent.values[0],
                sensorEvent.values[1],
                sensorEvent.values[2], sensorEvent.timestamp);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        //Nothing to do here for now.
    }

    //Callback to update UI
    @Override
    public void onEventRaised(EventType type) {
        //Currently analyzing only accelerometer events (grouping brake and Acceleration)
        switch (type){
            case HardBrake:
            case HardAccel:
                hardBrakeIntegerCount++;
                tvHardBrakeCount.setText(String.valueOf(hardBrakeIntegerCount));
                break;
        }
    }
}
