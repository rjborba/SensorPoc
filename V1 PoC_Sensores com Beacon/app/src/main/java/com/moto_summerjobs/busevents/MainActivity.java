package com.moto_summerjobs.busevents;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements SensorEventListener, EventListener, LocationListener {

    private final static int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    private TextView tvHardBrakeCount;
    private TextView tvHardAccelCount;

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;

    private int hardBrakeIntegerCount = 0, hardAccelIntegerCount = 0;
    private EventHandler eventHandler;

    private TextView beacon;
    private TextView acc;
    private TextView dir;
    private TextView lat;
    private TextView lng;
    private TextView vel;
    private TextView tvLocationUpdate;

    long currentSpeed = 0;

    LocationManager locationManager = null;
    Location secondLastLocation = new Location("Second Last");

    BluetoothManager btManager;
    BluetoothAdapter btAdapter;
    BluetoothLeScanner btScanner;

    Button startScanningButton;
    Button stopScanningButton;

    String locationProvider = LocationManager.GPS_PROVIDER;

    @SuppressLint("NewApi")
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
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

        beacon = findViewById(R.id.beacon_id);

        acc = findViewById(R.id.acc);

        lat = findViewById(R.id.latitude);
        lng = findViewById(R.id.longitude);
        dir = findViewById(R.id.direcao);
        vel = findViewById(R.id.velocidade);
        tvLocationUpdate = findViewById(R.id.locationUpdate);

        startScanningButton = (Button) findViewById(R.id.StartScanButton);
        startScanningButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startScanning();
            }
        });

        stopScanningButton = (Button) findViewById(R.id.StopScanButton);
        stopScanningButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                stopScanning();
            }
        });
        stopScanningButton.setVisibility(View.INVISIBLE);

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

        /*Based on https://stackoverflow.com/questions/16956398/android-location-network-provider-device-speed
            network provider should not be used when we're working with currentSpeed
        */
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                100,
                0, this);

        mSensorManager.registerListener(this,
                mAccelerometer,
                SensorManager.SENSOR_DELAY_GAME);

        btManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();
        btScanner = btAdapter.getBluetoothLeScanner();

        if (btAdapter != null && !btAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent,REQUEST_ENABLE_BT);
        }

        if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("This app needs location access");
            builder.setMessage("Please grant location access so this app can detect peripherals.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                }
            });
            builder.show();
        }

    }

    public void stopScanning() {
        System.out.println("stopping scanning");
        beacon.append("Stopped Scanning");
        startScanningButton.setVisibility(View.VISIBLE);
        stopScanningButton.setVisibility(View.INVISIBLE);
        AsyncTask.execute(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void run() {
                btScanner.stopScan(leScanCallback);
            }
        });
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

        eventHandler.updateAccValues(sensorEvent.values[0],
                sensorEvent.values[1],
                sensorEvent.values[2],
                sensorEvent.timestamp);

        //For testing purposes
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


    int locationUpdatesCount = 0;
    @Override
    public void onLocationChanged(Location location) {
        locationUpdatesCount++;

        /*
           Speed is is being get in M/S. Must times by 3.6 in order to convert
           do KM/H
         */
        if(location.hasSpeed())
            currentSpeed = (long) (location.getSpeed() * 3.6);

        Log.d("SpeedSummary", "\n" + currentSpeed);

        dir.setText("Direção: " + secondLastLocation.bearingTo(location));
        vel.setText("Velocidade: " + currentSpeed);

        lat.setText("Latitude: " + location.getLatitude());
        lng.setText("Longitude: " + location.getLongitude());

        tvLocationUpdate.setText("Updates: " + locationUpdatesCount);

        secondLastLocation.setLatitude(location.getLatitude());
        secondLastLocation.setLongitude(location.getLongitude());

        if(location.hasSpeed()){
            eventHandler.updateGpsValues(location.getSpeed());
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.d("onStatusChanged", provider);
    }
    @Override
    public void onProviderEnabled(String provider) {
        Log.d("onProviderEnabled", provider);
    }
    @Override
    public void onProviderDisabled(String provider) {
        Log.d("onProviderDisabled", provider);
    }

    private ScanCallback leScanCallback = new ScanCallback() {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onScanResult(int callbackType, ScanResult result) {

            byte[] scanRecord = result.getScanRecord().getBytes();
            int startByte = 2;
            boolean patternFound = false;
            while (startByte <= 5)
            {
                if (    ((int) scanRecord[startByte + 2] & 0xff) == 0x02 && //Identifies an iBeacon
                        ((int) scanRecord[startByte + 3] & 0xff) == 0x15)
                { //Identifies correct data length
                    patternFound = true;
                    break;
                }
                startByte++;
            }

            if (patternFound)
            {
                //Convert to hex String
                byte[] uuidBytes = new byte[16];
                System.arraycopy(scanRecord, startByte + 4, uuidBytes, 0, 16);
                String hexString = bytesToHex(uuidBytes);

                //UUID detection
                String uuid =  hexString.substring(0,8) + "-" +
                        hexString.substring(8,12) + "-" +
                        hexString.substring(12,16) + "-" +
                        hexString.substring(16,20) + "-" +
                        hexString.substring(20,32);

                // major
                final int major = (scanRecord[startByte + 20] & 0xff) * 0x100 + (scanRecord[startByte + 21] & 0xff);

                // minor
                final int minor = (scanRecord[startByte + 22] & 0xff) * 0x100 + (scanRecord[startByte + 23] & 0xff);

                int linha = minor;

                beacon.setText("UUid: " + uuid+ "\nMinor: " + minor+ "\nMajor: " + major + "\n");

            }
        }
    };


    public void startScanning() {
        System.out.println("start scanning");
        beacon.setText("");
        AsyncTask.execute(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void run() {
                btScanner.startScan(leScanCallback);
            }
        });
    }

    static final char[] hexArray = "0123456789ABCDEF".toCharArray();
    private static String bytesToHex(byte[] bytes)
    {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ )
        {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
