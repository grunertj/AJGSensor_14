package com.jwg.grunert.ajgsensor;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.design.widget.TabLayout;
import android.support.v4.app.NotificationCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import android.os.Bundle;
import android.view.ViewGroup;
import android.view.WindowManager;

public class MainActivity extends AppCompatActivity {
    String global_fragments[];
    MainFragment mainFragment=null;
    SettingsFragment settingsFragment=null;
    DebugFragment debugFragment=null;
    ViewSpeedFragment viewSpeedFragment =null;
    ViewStrokeFragment viewStrokeFragment =null;
    ViewPaceFragment viewPaceFragment =null;

    TabLayout tabLayout;
    LockableViewPager  viewPager;

    static final int TYPE_GPS = 0;

    static final int OVERRIDE = 1;
    static final int APPEND = 2;
    static final int NEW = 3;

    static int SENSOR_DELAY = SensorManager.SENSOR_DELAY_FASTEST;

    static boolean COMPRESS = true;
    static boolean FILTERED = true;

    static boolean dim_state = true;

    static int file_mode = NEW;

    static boolean[] log;

    static int curBrightnessValue;
    protected PowerManager.WakeLock mWakeLock;

    private static final int NOTIFICATION_EX = 1;
    private NotificationManager notificationManager;
    private Notification notification;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        this.mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
        this.mWakeLock.acquire();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // GraphView graph = (GraphView) findViewById(R.id.graph);

        /*
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = new Notification(R.drawable.ic_stat_motorcycle, null, 0);

        notificationManager.notify(NOTIFICATION_EX, notification);
        */
        Context context = getApplicationContext();
        Intent notificationIntent = new Intent(context, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(context,
                0, notificationIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        notificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        Resources res = context.getResources();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);

        builder.setContentIntent(contentIntent)
                .setSmallIcon(R.drawable.ic_stat_motorcycle)
                .setLargeIcon(BitmapFactory.decodeResource(res, R.drawable.ic_stat_motorcycle))
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(false)
                .setContentTitle("AJGSensor is running");

        notification = builder.getNotification();
        notification.defaults |= Notification.DEFAULT_ALL;
        notificationManager.notify(NOTIFICATION_EX, notification);

        log = new boolean[100];

        log[TYPE_GPS] = true;
        log[Sensor.TYPE_ACCELEROMETER] = false; // was true
        log[Sensor.TYPE_LINEAR_ACCELERATION] = false; // was true
        log[Sensor.TYPE_STEP_DETECTOR] = false;
        log[Sensor.TYPE_STEP_COUNTER] = false;
        log[Sensor.TYPE_GYROSCOPE] = false; // was true
        log[Sensor.TYPE_GYROSCOPE_UNCALIBRATED] = false; // was true
        log[Sensor.TYPE_ROTATION_VECTOR] = false;

        if (mainFragment == null) {
            mainFragment = new MainFragment();
        }

        if (mainFragment == null) {
            mainFragment = new MainFragment();
            mainFragment.setRetainInstance(true);
        }

        if (settingsFragment == null) {
            settingsFragment = new SettingsFragment();
            settingsFragment.setRetainInstance(true);
        }

        if (debugFragment == null) {
            debugFragment = new DebugFragment();
            debugFragment.setRetainInstance(true);
        }

        if (viewSpeedFragment == null) {
            viewSpeedFragment = new ViewSpeedFragment();
            viewSpeedFragment.setRetainInstance(true);
        }

        if (viewStrokeFragment == null) {
            viewStrokeFragment = new ViewStrokeFragment();
            viewStrokeFragment.setRetainInstance(true);
        }

        if (viewPaceFragment == null) {
            viewPaceFragment = new ViewPaceFragment();
            viewPaceFragment.setRetainInstance(true);
        }

        global_fragments = new String[5];
        global_fragments[0] = "Main";
        global_fragments[1] = "Settings";
        global_fragments[2] = "Debug";
        global_fragments[3] = "View Speed";
        global_fragments[4] = "View Pace";

        viewPager = (LockableViewPager) findViewById(R.id.viewPager);
        viewPager.setAdapter(new CustomAdapter(getSupportFragmentManager(), getApplicationContext()));
        viewPager.setOffscreenPageLimit(100);
        viewPager.setSwipeable(false);

        tabLayout = (TabLayout) findViewById(R.id.tabLayout);
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }
        });
    }

    private class CustomAdapter extends FragmentPagerAdapter {
        private String fragments [] = global_fragments;

        public CustomAdapter(FragmentManager fm) {
            super(fm);
        }

        public CustomAdapter(FragmentManager fm, Context applicationContext) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return mainFragment;
                case 1:
                    return settingsFragment;
                case 2:
                    return debugFragment;
                case 3:
                    return viewSpeedFragment;
                case 4:
                    return viewPaceFragment;
                default:
                    return null;
            }
        }


        @Override
        public int getCount() {
            return fragments.length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return fragments[position];
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            super.destroyItem(container, position, object);
        }


    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            curBrightnessValue = android.provider.Settings.System.getInt(getContentResolver(),Settings.System.SCREEN_BRIGHTNESS);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
    }

    static public boolean dim_screen (boolean on, Context context) {
        if (on == true) {
            try {
                try {
                    curBrightnessValue = android.provider.Settings.System.getInt(context.getContentResolver(),Settings.System.SCREEN_BRIGHTNESS);
                } catch (Settings.SettingNotFoundException e) {
                    e.printStackTrace();
                }
                Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
                Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 0);
            } catch (Exception e) {}
            return false;
        } else {
            try {
                Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
                if (curBrightnessValue > 64) {
                    Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, curBrightnessValue);
                } else {
                    Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 64);
                }
            } catch (Exception e) {}
            return  true;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        dim_screen(false,this);
        if (isFinishing()) {
            notificationManager.cancel(NOTIFICATION_EX);
        }
    }
}
