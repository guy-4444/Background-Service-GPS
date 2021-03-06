package com.guy.backgroundgps;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.Manifest;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.gson.Gson;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    public static final String BROADCAST_NEW_LOCATION_DETECTED = "guy.cyclingtracker.NEW_LOCATION_DETECTED";

    private final int LOCATION_PERMISSIONS_REQUEST_CODE = 125;

    private AppCompatButton main_BTN_start;
    private AppCompatButton main_BTN_pause;
    private AppCompatButton main_BTN_stop;
    private AppCompatButton main_BTN_info;
    private AppCompatTextView main_LBL_info;
    private LocalBroadcastManager localBroadcastManager;


    private BroadcastReceiver myReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(BROADCAST_NEW_LOCATION_DETECTED)) {
                String json = intent.getStringExtra("EXTRA_LOCATION");
                try {
                    MyLoc lastLocation = new Gson().fromJson(json, MyLoc.class);
                    newLocation(lastLocation);
                } catch(Exception ex) { }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("pttt", "MainActivity - onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        findViews();
        initViews();
        askLocationPermissions();
        validateButtons();
    }

    private void newLocation(final MyLoc lastLocation) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                main_LBL_info.setText(lastLocation.getLatitude() + "\n" + lastLocation.getLongitude());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter(BROADCAST_NEW_LOCATION_DETECTED);
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        localBroadcastManager.registerReceiver(myReceiver, intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        localBroadcastManager.unregisterReceiver(myReceiver);
    }

    private void validateButtons() {
        if (isMyServiceRunning(LocationService.class)) {
            main_BTN_start.setEnabled(false);
            main_BTN_pause.setEnabled(true);
            main_BTN_stop.setEnabled(true);
        } else {
            main_BTN_start.setEnabled(true);
            main_BTN_pause.setEnabled(false);
            main_BTN_stop.setEnabled(false);
        }
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        int counter = 0;
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

        List<ActivityManager.RunningServiceInfo> runs = manager.getRunningServices(Integer.MAX_VALUE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                counter++;
                //return true;
            }
        }

        Log.d("pttt", "Counter= " + counter);
        if (counter > 0)
            return true;
        return false;
    }

    private void startService() {
        actionToService(LocationService.START_FOREGROUND_SERVICE);
        validateButtons();
    }

    private void pauseService() {
        actionToService(LocationService.PAUSE_FOREGROUND_SERVICE);
        validateButtons();
    }

    private void stopService() {
        actionToService(LocationService.STOP_FOREGROUND_SERVICE);
        validateButtons();
    }

    private void actionToService(String action) {
        Intent startIntent = new Intent(MainActivity.this, LocationService.class);
        startIntent.setAction(action);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(startIntent);
            // or
            //ContextCompat.startForegroundService(this, startIntent);
        } else {
            startService(startIntent);
        }
    }




    // // // // // // // // // // // // // // // // Permissions  // // // // // // // // // // // // // // //

    private void askLocationPermissions() {
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{
                        Manifest.permission.ACCESS_COARSE_LOCATION
                        ,Manifest.permission.ACCESS_FINE_LOCATION
                        ,Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        ,Manifest.permission.FOREGROUND_SERVICE
                },
                LOCATION_PERMISSIONS_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case LOCATION_PERMISSIONS_REQUEST_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    Toast.makeText(MainActivity.this, "Result code = " + grantResults[0], Toast.LENGTH_SHORT).show();

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(MainActivity.this, "Permission denied to read your External storage", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }

    private void openAppSettingsManually() {
        Intent intent = new Intent();
        intent.setAction(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }

    // // // // // // // // // // // // // // // // Views  // // // // // // // // // // // // // // //

    private void initViews() {
        main_BTN_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startService();
            }
        });

        main_BTN_pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pauseService();
            }
        });

        main_BTN_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopService();
                validateButtons();
            }
        });

        main_BTN_info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validateButtons();
            }
        });
    }

    private void findViews() {
        main_BTN_start = findViewById(R.id.main_BTN_start);
        main_BTN_pause = findViewById(R.id.main_BTN_pause);
        main_BTN_stop = findViewById(R.id.main_BTN_stop);
        main_BTN_info = findViewById(R.id.main_BTN_info);
        main_LBL_info = findViewById(R.id.main_LBL_info);
    }
}
