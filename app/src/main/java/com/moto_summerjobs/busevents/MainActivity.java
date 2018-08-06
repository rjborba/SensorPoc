package com.moto_summerjobs.busevents;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements SensorEventListener, EventListener, LocationListener {
    private TextView tvHardBrakeCount;
    private TextView tvHardAccelCount;

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;

    private int hardBrakeIntegerCount = 0, hardAccelIntegerCount = 0;
    private EventHandler eventHandler;

    private TextView acc;
    private TextView dir;
    private TextView lat;
    private TextView lng;
    private TextView vel;

    long speed = 0;

    LocationManager locationManager = null;
    Location secondLastLocation = new Location("Second Last");

    String locationProvider = LocationManager.GPS_PROVIDER;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        tvHardBrakeCount = findViewById(R.id.hardBrakeCount);
        tvHardAccelCount = findViewById(R.id.hardAccelCount);

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        eventHandler = new EventHandler(this, this);

        acc = findViewById(R.id.acc);

        lat = (TextView) findViewById(R.id.latitude);
        lng = (TextView) findViewById(R.id.longitude);
        dir = (TextView) findViewById(R.id.direcao);
        vel = (TextView) findViewById(R.id.velocidade);

        if (ActivityCompat
                .checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
        if (ActivityCompat
                .checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET}, 2);
        }
        if (ActivityCompat
                .checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 3);
        }

        locationManager.requestLocationUpdates(locationProvider,
                SensorManager.SENSOR_DELAY_GAME,
                10, this);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                SensorManager.SENSOR_DELAY_GAME,
                10, this);

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

        eventHandler.updateValues(sensorEvent.values[0],
                                  sensorEvent.values[1],
                                  sensorEvent.values[2], sensorEvent.timestamp, speed);

        acc.setText(String.format("Acc: %.3f",
                Math.sqrt((sensorEvent.values[0] * sensorEvent.values[0])
                + (sensorEvent.values[1] * sensorEvent.values[1])
                + (sensorEvent.values[2] * sensorEvent.values[2])) / 9.8));
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
                hardBrakeIntegerCount++;
                tvHardBrakeCount.setText(String.valueOf(hardBrakeIntegerCount));
                break;
            case HardAccel:
                hardAccelIntegerCount++;
                tvHardAccelCount.setText(String.valueOf(hardAccelIntegerCount));
                break;
        }
    }

    @Override
    public void onLocationChanged(Location location) {

        if(secondLastLocation != null){
            speed = (long) (Math.sqrt(
                                Math.pow(location.getLongitude() - secondLastLocation.getLongitude(), 2)
                                + Math.pow(location.getLatitude() - secondLastLocation.getLatitude(), 2)
                        ) / (location.getTime() - secondLastLocation.getTime()));
        }
//        if(secondLastLocation != null){
//            double elapsedTime = (location.getTime() - secondLastLocation.getTime()) / 1000; // Convert milliseconds to seconds
//            speed = (long) (secondLastLocation.distanceTo(location) / elapsedTime);
//        }
        if(location.hasSpeed())
            speed = (long) location.getSpeed();

        Log.d("Speed Summary", "\n" + speed);

        dir.setText("Direção: " + secondLastLocation.bearingTo(location));
        vel.setText("Velocidade: " + speed);

        lat.setText("Latitude: " + location.getLatitude());
        lng.setText("Longitude: " + location.getLongitude());

        secondLastLocation.setLatitude(location.getLatitude());
        secondLastLocation.setLongitude(location.getLongitude());
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }
    @Override
    public void onProviderEnabled(String provider) {

    }
    @Override
    public void onProviderDisabled(String provider) {

    }
}
