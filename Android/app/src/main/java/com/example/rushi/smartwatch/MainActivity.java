package com.example.rushi.smartwatch;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private Button ConnectButton, SVButton, AlarmButton, SOSButton, FMWButton;
    private ImageView ConnectImage;
    private String myDate;
    private String deviceName = null;

    LocationManager locationManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ConnectButton = (Button) findViewById(R.id.ConnectButton);
        SVButton = (Button) findViewById(R.id.SVButton);
        AlarmButton = (Button) findViewById(R.id.AlarmButton);
        SOSButton = (Button) findViewById(R.id.SOSButton);
        FMWButton = (Button) findViewById(R.id.FMWButton);
        ConnectImage = (ImageView) findViewById(R.id.ConnectImage);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        int a = BluetoothCommService.getSTATUS();
        int b = FirebaseFetchService.getSTATUS();
        if(a==-1 && b==-1)
        {
            ConnectImage.setImageAlpha(32);
            SVButton.setEnabled(false);
            AlarmButton.setEnabled(false);
            SOSButton.setEnabled(false);
            FMWButton.setEnabled(false);
        }

        ConnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onConnectButtonClick();
            }
        });

        FMWButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent nextActivity = new Intent(MainActivity.this, FMWActivity.class);
                startActivity(nextActivity);
            }
        });

        SVButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent nextActivity = new Intent(MainActivity.this, SVActivity.class);
                startActivity(nextActivity);
            }
        });

        AlarmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent nextActivity = new Intent(MainActivity.this, AlarmActivity.class);
                startActivity(nextActivity);
            }
        });

        SOSButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent nextActivity = new Intent(MainActivity.this, SOSActivity.class);
                startActivity(nextActivity);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.SyncButton)
        {
            Handler handler1 = new Handler();
            handler1.postDelayed(new Runnable() {
                public void run()
                {
                    BluetoothCommService.updateAlarmTime();
                    BluetoothCommService.updateSV();
                    BluetoothCommService.updateModes();
                }
            }, 2000);

            Handler handler2 = new Handler();
            handler2.postDelayed(new Runnable() {
                public void run()
                {
                    BluetoothCommService.updateAlarmMessage();
                }
            }, 7000);
        }
        return super.onOptionsItemSelected(item);
    }

    public Location getDeviceLoc()
    {
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
        else
        {
            Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            Location location1 = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            Location location2 = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
            if (location != null)
            {
                return location;
            }
            else if (location1 != null)
            {
                return location1;
            }
            else if (location2 != null)
            {
                return location2;
            }
            else
            {
                msg("Unable to trace your location");
            }
        }
        return null;
    }

    public static void sendEmailAndSMS()
    {
        String latitude = FirebaseFetchService.getLatitude();
        String longitude = FirebaseFetchService.getLongitude();

        final String locationURL = "https://maps.google.com/?q="+latitude+","+longitude;

        final int numberOfContacts = FirebaseFetchService.getNumberOfContacts();
        ArrayList<Contact> SOSContacts = FirebaseFetchService.getContacts();
        final String SOSContactNames[] = new String[numberOfContacts];
        final String SOSContactEmails[] = new String[numberOfContacts];
        final String SOSContactPhone[] = new String[numberOfContacts];

        for(int i=0;i<numberOfContacts; i++)
        {
            SOSContactNames[i] = SOSContacts.get(i).getName();
            SOSContactEmails[i] = SOSContacts.get(i).getEmail();
            SOSContactPhone[i] = SOSContacts.get(i).getPhone();
        }

        final String bodySMS =  "SOS! I need emergency help (Sent from SmartWatch).\nMy location is: "+locationURL;

        SmsManager smsManager = SmsManager.getDefault();

        for(int i=0; i<numberOfContacts; i++)
        {
            smsManager.sendTextMessage(SOSContactPhone[i], null, SOSContactNames[i]+",\n"+bodySMS, null, null);
        }

        final String username = FirebaseFetchService.getGMailUsername();
        final String password = FirebaseFetchService.getGMailPassword();
        final String sub = "SOS Emergency Email from SmartWatch";
        final String body = "SOS! I need emergency help (Sent from SmartWatch).\nMy location is: "+locationURL;

        final Thread GMailThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    GMailSender sender = new GMailSender(username, password);
                    for(int i=0;i<numberOfContacts; i++)
                    {
                        sender.sendMail(sub, SOSContactNames[i]+",\n"+body, username, SOSContactEmails[i]);
                        Log.e("GMailThread", "Email Sent to "+SOSContactEmails[i]);
                    }
                } catch (Exception e)
                {
                    Log.e("GMailThread", e.getMessage(), e);
                }
            }
        });
        GMailThread.start();
    }


    private void onConnectButtonClick()
    {
        Intent BTForeground = new Intent(MainActivity.this, BluetoothCommService.class);
        if(deviceName == null)
        {
            BTForeground.setAction("connect");
        }
        else
        {
            BTForeground.setAction("disconnect");
        }
        startService(BTForeground);

        final Thread BTThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    while (BluetoothCommService.getSTATUS() == 0)
                    {
                        this.wait(100);
                        Log.e("MainActivity", "Refreshing");
                    }
                } catch (InterruptedException e)
                {
                    finish();
                }
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        uiFunction();
                    }
                });
            }
        });
        BTThread.start();

        Intent FBForeground = new Intent(MainActivity.this, FirebaseFetchService.class);
        FBForeground.setAction("fetch");
        startService(FBForeground);

        final Thread FBThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try
                {
                    while(FirebaseFetchService.getSTATUS() == 0)
                    {
                        this.wait(10);
                        Log.e("MainActivity", "Fetching");
                    }
                }
                catch (InterruptedException e)
                {
                    msg("Firebase data fetch failed");
                    finish();
                }
            }
        });
        FBThread.start();
    }

    void uiFunction()
    {
        try
        {
            deviceName = BluetoothCommService.getConnectedDevice();
        } catch (Exception e)
        {
            deviceName = null;
        }

        if(deviceName == null)
        {
            msg("Couldn't connect to HC-05. Please Try Again");
        }
        else
        {
            msg("Successfully connected with "+deviceName);
            onConnect();
        }
    }

    private void onConnect()
    {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run()
            {
                ConnectImage.setImageAlpha(255);
                SVButton.setEnabled(true);
                AlarmButton.setEnabled(true);
                SOSButton.setEnabled(true);
                FMWButton.setEnabled(true);

                Location currentLocation = getDeviceLoc();
                FirebaseFetchService.setLongitude(currentLocation.getLongitude()+"");
                FirebaseFetchService.setLatitude(currentLocation.getLatitude()+"");
            }
        }, 2000);

        Intent BTReceiver = new Intent(MainActivity.this, BluetoothConnectReceiver.class);
        BTReceiver.setAction("inputExtra");
        startService(BTReceiver);
        Log.e("MainActivity", "BT Rx Service called");
    }

    private void msg(String s)
    {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
    }

}