package com.jwg.grunert.ajgsensor;


import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.zip.GZIPOutputStream;

public class MainFragment extends Fragment implements SensorEventListener, LocationListener {
    static final int LONG = 0;
    static final int SHORT = 1;
    static final long MIN_TIME_IN_MILLISECONDS = 1000;
    static final float MIN_DISTANCE_IN_METERS = 1;
    static final float START_SPEED = 2.2f ; // 7.92 km/h start logging

    static double LastLatitude, LastLongitude, TotalDistance, MaxSpeed, AverageSpeed, TotalSpeed, CurrentSpeed;
    static int log_counter;
    static int all_log_counter;
    static int sensor_counter;
    static int valid_log_counter;
    static boolean first_location = true;
    float [] Distance = new float[10];
    static long[] previous_sensor_system_timestamp;
    static long gps_previous_system_timestamp = 0L,gps_previous_location_timestamp = 0L;

    // static final String directory_name = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + File.separator + "PaddleSensorBis";

    static String directory_name = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + File.separator + "PaddleSensorBis";

    TextView textViewRunning, textViewFile, textViewGPSFile, textViewLocation,textViewLatitude,textViewLongitude;
    TextView textViewDistance, textViewAverageSpeed, textViewMaxSpeed, textViewCurrentSpeed, textViewStartLogging, textViewTrackPoints,textViewSensorValues,textViewAllTrackPoints;
    Button buttonStart, buttonStop, buttonDim;
    SimpleDateFormat gpx_simpleDateFormat;


    static BufferedWriter sensor_file = null;
    static BufferedWriter gps_file = null;
    static String sensor_file_name = null;
    static String gps_file_name = null;

    SensorManager sensorManager = null;
    LocationManager locationManager = null;

    public MainFragment() {
        // Required empty public constructor

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        //getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);



        previous_sensor_system_timestamp = new long[100];

        previous_sensor_system_timestamp[Sensor.TYPE_LINEAR_ACCELERATION] = 0L;
        previous_sensor_system_timestamp[Sensor.TYPE_ACCELEROMETER] = 0L;
        previous_sensor_system_timestamp[Sensor.TYPE_STEP_DETECTOR] = 0L;
        previous_sensor_system_timestamp[Sensor.TYPE_STEP_COUNTER] = 0L;
        previous_sensor_system_timestamp[Sensor.TYPE_GYROSCOPE] = 0L;
        previous_sensor_system_timestamp[Sensor.TYPE_GYROSCOPE_UNCALIBRATED] = 0L;
        previous_sensor_system_timestamp[Sensor.TYPE_ROTATION_VECTOR] = 0L;

        textViewFile = (TextView) view.findViewById(R.id.textViewFile);
        textViewGPSFile = (TextView) view.findViewById(R.id.textViewGPSFile);
        textViewRunning = (TextView) view.findViewById(R.id.textViewRunning);
        textViewLocation = (TextView) view.findViewById(R.id.textViewLocation);
        textViewLatitude = (TextView) view.findViewById(R.id.textViewLatitude);
        textViewLongitude = (TextView) view.findViewById(R.id.textViewLongitude);
        textViewDistance = (TextView) view.findViewById(R.id.textViewDistance);
        textViewAverageSpeed = (TextView) view.findViewById(R.id.textViewAverageSpeed);
        textViewMaxSpeed = (TextView) view.findViewById(R.id.textViewMaxSpeed);
        textViewCurrentSpeed = (TextView) view.findViewById(R.id.textViewCurrentSpeed);
        textViewStartLogging = (TextView) view.findViewById(R.id.textViewStartLogging);
        textViewTrackPoints = (TextView) view.findViewById(R.id.textViewTrackPoints);
        textViewSensorValues = (TextView) view.findViewById(R.id.textViewSensorValues);
        textViewAllTrackPoints = (TextView) view.findViewById(R.id.textViewAllTrackPoints);
        buttonStart = (Button) view.findViewById(R.id.buttonStart);
        buttonStop = (Button) view.findViewById(R.id.buttonStop);
        buttonDim = (Button) view.findViewById(R.id.buttonDim);
        File sdRoot = null;
        String sdRootString = null;
        String[] ids;
        String sdRootMount = null;

        // File[] externalFilesDirs = Context.getExternalFilesDirs(null);
        File[] extMounts  = ContextCompat.getExternalFilesDirs(getActivity().getApplicationContext(), null);

        if (extMounts.length < 2) {
            if (extMounts[0].canRead() && extMounts[0].canWrite()) {
                directory_name = extMounts[0].toString();
            }
        } else {
            if (extMounts[1].canRead() && extMounts[1].canWrite() ) {
                directory_name = extMounts[1].toString();
            } else if (extMounts[0].canRead() && extMounts[0].canWrite()) {
                directory_name = extMounts[0].toString();
            } else {
                Toast.makeText(getActivity().getApplicationContext(), "Could not use directory: "+ directory_name, Toast.LENGTH_LONG).show();
            }
        }
        /*
        if (extMounts.length <2) {
            directory_name = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + File.separator + "PaddleSensorBis";
        } else {
            sdRoot = extMounts[1];
            if(sdRoot == null) {
                directory_name = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + File.separator + "PaddleSensorBis";
            } else if (sdRoot.canWrite() && sdRoot.canRead()) {
                sdRootString = sdRoot.getAbsolutePath();
                ids = sdRootString.split("/");
                sdRootMount = File.separator + ids[1] + File.separator + ids[2];
                directory_name = sdRootMount + File.separator + Environment.DIRECTORY_DOWNLOADS + File.separator + "PaddleSensorBis";
                // directory_name = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + File.separator + "PaddleSensorBis";

            } else {
                directory_name = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + File.separator + "PaddleSensorBis";
            }
        }
        */

        textViewGPSFile.setText(directory_name);

        File directory = new File(directory_name);

        gpx_simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        gpx_simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        if (directory.exists() == false) {
            boolean success = directory.mkdirs();
            if (!success) {
                Toast.makeText(getActivity().getApplicationContext(), "Could not create directory: "+ directory_name, Toast.LENGTH_LONG).show();
            }
        }

        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    MainActivity.dim_state = MainActivity.dim_screen(false, getActivity());
                }
                return false;
            }
        });

        buttonDim.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.dim_state = MainActivity.dim_screen(MainActivity.dim_state, getActivity());
            }
        });

        buttonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String timestamp = getCurrentTimeStamp(SHORT);
                sensor_logging(true, timestamp);
                sensor(true);
                location_logging(true, timestamp);
                gpx_header();
                location(true);
                buttonStart.setEnabled(false);
                textViewGPSFile.setEnabled(false);
                textViewFile.setEnabled(false);
                sensor_counter = 0 ;
                log_counter = 0 ;
                all_log_counter = 0;
                valid_log_counter = 0;
                TotalDistance = 0.0f;
                MaxSpeed = 0.0f;
                AverageSpeed = 0.0f;
                TotalSpeed = 0.0f;
                CurrentSpeed = 0.0f;
                textViewDistance.setText(String.format(Locale.US,"Distance: %.2f km",TotalDistance / 1000.0f));
                textViewAverageSpeed.setText(String.format(Locale.US,"Average Speed: %d km/h",(int)Math.round(AverageSpeed * 3.6)));
                textViewMaxSpeed.setText(String.format(Locale.US,"Max Speed: %d km/h",(int)Math.round(MaxSpeed * 3.6)));
                textViewCurrentSpeed.setText(String.format(Locale.US,"Current Speed: %d km/h",(int)Math.round(CurrentSpeed * 3.6)));
            }
        });

        buttonStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sensor(false);
                sensor_logging(false, null);
                location(false);
                gpx_footer();
                location_logging(false, null);
                buttonStart.setEnabled(true);
                textViewGPSFile.setEnabled(true);
                textViewFile.setEnabled(true);
                Toast.makeText(getActivity().getApplicationContext(), "Click on file name to share.", Toast.LENGTH_SHORT).show();
                textViewDistance.setText(String.format(Locale.US, "Distance: %.2f km", TotalDistance / 1000.0f));
                textViewAverageSpeed.setText(String.format(Locale.US, "Average Speed: %d km/h", (int) Math.round(AverageSpeed * 3.6)));
                textViewMaxSpeed.setText(String.format(Locale.US, "Max Speed: %d km/h", (int) Math.round(MaxSpeed * 3.6)));
                textViewCurrentSpeed.setText(String.format(Locale.US, "Current Speed: %d km/h", (int) Math.round(CurrentSpeed * 3.6)));
            }
        });

        textViewFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView textView = ((TextView) v);
                String string = textView.getText().toString();
                Uri uri = Uri.fromFile(new File(getAbsoluteFileName(string)));
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                sendIntent.putExtra(Intent.EXTRA_STREAM, uri);
                sendIntent.setType("text/plain");
                // sendIntent.setType("application/zip");
                Toast.makeText(getActivity().getApplicationContext(), "Sharing file: " + string, Toast.LENGTH_SHORT).show();

                startActivity(sendIntent);
            }
        });

        textViewGPSFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (MainActivity.log[MainActivity.TYPE_GPS]) {
                    TextView textView = ((TextView) v);
                    String string = textView.getText().toString();
                    Uri uri = Uri.fromFile(new File(getAbsoluteFileName(string)));
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    sendIntent.putExtra(Intent.EXTRA_STREAM, uri);
                    sendIntent.setType("text/plain");
                    // sendIntent.setType("application/zip");
                    Toast.makeText(getActivity().getApplicationContext(), "Sharing file: " + string, Toast.LENGTH_SHORT).show();
                    startActivity(sendIntent);
                }
            }
        });

        return view;
    }

    @Override
    public void onStop() {
        super.onStop();
        if (gps_file_name != null) {
            MediaScannerConnection.scanFile(getActivity().getApplicationContext(), new String[]{getAbsoluteFileName(gps_file_name)}, null, null);
        }

        if (sensor_file_name != null) {
            MediaScannerConnection.scanFile(getActivity().getApplicationContext(), new String[]{getAbsoluteFileName(sensor_file_name)}, null, null);
        }
        /*
        sensor(false);
        sensor_logging(false, null);
        location(false);
        location_logging(false, null);
        */
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sensor(false);
        sensor_logging(false, null);
        location(false);
        location_logging(false, null);
    }

    public String getCurrentTimeStamp(int which) {
        if (which == SHORT) {
            return new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(new Date());
        } else {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
        }
    }

    public static String getAbsoluteFileName(String filename) {
        File path = new File(directory_name);
        if (filename.matches(path.toString())) {
            return filename;
        } else {
            return (path.toString() + File.separator + filename);
        }
    }

    void sensor (boolean on) {
        if (on == true) {
            if (sensorManager == null) {
                sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
            }

            if (MainActivity.log[Sensor.TYPE_ACCELEROMETER]) {
                sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), MainActivity.SENSOR_DELAY);
            }
            if (MainActivity.log[Sensor.TYPE_LINEAR_ACCELERATION]) {
                sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), MainActivity.SENSOR_DELAY);
            }
            if (MainActivity.log[Sensor.TYPE_STEP_DETECTOR]) {
                sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR), MainActivity.SENSOR_DELAY);
            }
            if (MainActivity.log[Sensor.TYPE_STEP_COUNTER]) {
                sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER), MainActivity.SENSOR_DELAY);
            }
            if (MainActivity.log[Sensor.TYPE_GYROSCOPE]) {
                sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), MainActivity.SENSOR_DELAY);
            }
            if (MainActivity.log[Sensor.TYPE_GYROSCOPE_UNCALIBRATED]) {
                sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED), MainActivity.SENSOR_DELAY);
            }
            if (MainActivity.log[Sensor.TYPE_ROTATION_VECTOR]) {
                sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR), MainActivity.SENSOR_DELAY);
            }
        } else {
            if (sensorManager != null) {
                sensorManager.unregisterListener(this);
            }
        }
    }

    void location (boolean on) {
        first_location = true;
        if (on && MainActivity.log[MainActivity.TYPE_GPS]) {
            if (locationManager == null) {
                locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
            }
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) == false) {
                Toast.makeText(getActivity().getApplicationContext(), "Please enable Location.", Toast.LENGTH_LONG).show();
            }
            locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, this, null);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_IN_MILLISECONDS, MIN_DISTANCE_IN_METERS, this);
            locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, MIN_TIME_IN_MILLISECONDS, MIN_DISTANCE_IN_METERS, this);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME_IN_MILLISECONDS, MIN_DISTANCE_IN_METERS, this);
        } else {
            if (locationManager != null) {
                locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
                locationManager.removeUpdates(this);
                locationManager = null;
            }
        }
    }

    void sensor_logging (boolean on, String timestamp) {
        if (on == true) {
            if (sensor_file_name == null || MainActivity.file_mode == MainActivity.NEW) {
                if (MainActivity.COMPRESS) {
                    sensor_file_name = String.format("%s-%s.txt.gz", "sensor", timestamp);
                } else {
                    sensor_file_name = String.format("%s-%s.txt", "sensor", timestamp);
                }
            }
            try {
                if (MainActivity.COMPRESS) {
                    sensor_file = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(getAbsoluteFileName(sensor_file_name)))));
                } else {
                    sensor_file = new BufferedWriter(new FileWriter(getAbsoluteFileName(sensor_file_name)));
                }
            } catch (IOException e) {
                Toast.makeText(getActivity().getApplicationContext(), "Could not open file: "+ "" + e + " " + getAbsoluteFileName(sensor_file_name), Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }

            textViewFile.setText(sensor_file_name);
        } else {
            if (sensor_file != null) {
                try {
                    sensor_file.flush();
                    sensor_file.close();
                    sensor_file = null;
                    MediaScannerConnection.scanFile(getActivity().getApplicationContext(), new String[]{getAbsoluteFileName(sensor_file_name)}, null, null);
                } catch (IOException e) {
                    Toast.makeText(getActivity().getApplicationContext(), "Could not close file: "+ "" + e + " " + getAbsoluteFileName(sensor_file_name), Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            }
        }
    }

    void location_logging (boolean on, String timestamp) {
        FileWriter file;
        if (on && MainActivity.log[MainActivity.TYPE_GPS]) {
            if (gps_file_name == null || MainActivity.file_mode == MainActivity.NEW) {
                gps_file_name = String.format("%s-%s.gpx","location", timestamp);
            }
            try {
                file = new FileWriter(getAbsoluteFileName(gps_file_name));
                gps_file = new BufferedWriter(file);
            } catch (IOException e) {
                Toast.makeText(getActivity().getApplicationContext(), "Could not open file: "+ "" + e + " " + getAbsoluteFileName(gps_file_name), Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
            textViewGPSFile.setText(gps_file_name);
        } else {
            if (gps_file != null) {
                try {
                    gps_file.flush();
                    gps_file.close();
                    gps_file = null;
                    MediaScannerConnection.scanFile(getActivity().getApplicationContext(), new String[]{getAbsoluteFileName(gps_file_name)}, null, null);
                } catch (IOException e) {
                    Toast.makeText(getActivity().getApplicationContext(), "Could not close file: "+ "" + e + " " + getAbsoluteFileName(gps_file_name), Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float value[];
        long timestamp, system_timestamp;
        String event_type;

        event_type=null;
        timestamp = event.timestamp;
        system_timestamp = System.currentTimeMillis();
        switch (event.sensor.getType()) {
            case Sensor.TYPE_LINEAR_ACCELERATION:
                if (previous_sensor_system_timestamp[Sensor.TYPE_LINEAR_ACCELERATION] != system_timestamp) {
                    value = event.values.clone();
                    event_type = String.format(Locale.US, "%d %d %d %.8f %.8f %.8f\n", timestamp, system_timestamp, event.sensor.getType(), value[0], value[1], value[2]);
                    textViewRunning.setText("Sensor.TYPE_LINEAR_ACCELERATION");
                } else {
                    event_type = null;
                }
                previous_sensor_system_timestamp[Sensor.TYPE_LINEAR_ACCELERATION] = system_timestamp;
                break;
            case Sensor.TYPE_ACCELEROMETER:
                if (previous_sensor_system_timestamp[Sensor.TYPE_ACCELEROMETER] != system_timestamp) {
                    value = event.values.clone();
                    event_type = String.format(Locale.US, "%d %d %d %.8f %.8f %.8f\n", timestamp, system_timestamp, event.sensor.getType(), value[0], value[1], value[2]);
                    textViewRunning.setText("Sensor.TYPE_ACCELEROMETER");
                } else {
                    event_type = null;
                }
                previous_sensor_system_timestamp[Sensor.TYPE_ACCELEROMETER] = system_timestamp;
                break;
            case Sensor.TYPE_STEP_DETECTOR:
                if (previous_sensor_system_timestamp[Sensor.TYPE_STEP_DETECTOR] != system_timestamp) {
                    value = event.values.clone();
                    event_type = String.format(Locale.US, "%d %d %d %.8f %.8f %.8f\n", timestamp, system_timestamp, event.sensor.getType(), value[0], value[0], value[0]);
                    textViewRunning.setText("Sensor.TYPE_STEP_DETECTOR");
                } else {
                    event_type = null;
                }
                previous_sensor_system_timestamp[Sensor.TYPE_STEP_DETECTOR] = system_timestamp;
                break;
            case Sensor.TYPE_STEP_COUNTER:
                if (previous_sensor_system_timestamp[Sensor.TYPE_STEP_COUNTER] != system_timestamp) {

                    value = event.values.clone();
                    event_type = String.format(Locale.US, "%d %d %d %.8f %.8f %.8f\n", timestamp, system_timestamp, event.sensor.getType(), value[0], value[0], value[0]);
                    textViewRunning.setText("Sensor.TYPE_STEP_COUNTER");
                } else {
                    event_type = null;
                }
                previous_sensor_system_timestamp[Sensor.TYPE_STEP_COUNTER] = system_timestamp;
                break;
            case Sensor.TYPE_GYROSCOPE:
                if (previous_sensor_system_timestamp[Sensor.TYPE_GYROSCOPE] != system_timestamp) {
                    value = event.values.clone();
                    event_type = String.format(Locale.US, "%d %d %d %.8f %.8f %.8f\n", timestamp, system_timestamp, event.sensor.getType(), value[0], value[1], value[2]);
                    textViewRunning.setText("Sensor.TYPE_GYROSCOPE");
                } else {
                    event_type = null;
                }
                previous_sensor_system_timestamp[Sensor.TYPE_GYROSCOPE] = system_timestamp;
                break;
            case Sensor.TYPE_GYROSCOPE_UNCALIBRATED:
                if (previous_sensor_system_timestamp[Sensor.TYPE_GYROSCOPE_UNCALIBRATED] != system_timestamp) {
                    value = event.values.clone();
                    event_type = String.format(Locale.US, "%d %d %d %.8f %.8f %.8f\n", timestamp, system_timestamp, event.sensor.getType(), value[0], value[1], value[2]);
                    textViewRunning.setText("Sensor.TYPE_GYROSCOPE_UNCALIBRATED");
                } else {
                    event_type = null;
                }
                previous_sensor_system_timestamp[Sensor.TYPE_GYROSCOPE_UNCALIBRATED] = system_timestamp;
                break;
            case Sensor.TYPE_ROTATION_VECTOR:
                if (previous_sensor_system_timestamp[Sensor.TYPE_ROTATION_VECTOR] != system_timestamp) {
                    value = event.values.clone();
                    event_type = String.format(Locale.US, "%d %d %d %.8f %.8f %.8f\n", timestamp, system_timestamp, event.sensor.getType(), value[0], value[1], value[2]);
                    textViewRunning.setText("Sensor.TYPE_ROTATION_VECTOR");
                } else {
                    event_type = null;
                }
                previous_sensor_system_timestamp[Sensor.TYPE_ROTATION_VECTOR] = system_timestamp;
                break;
        }
        try {
            if (event_type!=null) {
                textViewSensorValues.setText("Sensor values: " + sensor_counter++);
                sensor_file.append(event_type);
            }
        } catch (IOException e) {
            Toast.makeText(getActivity().getApplicationContext(), "Could append to file: "+ "" + e + " " + getAbsoluteFileName(sensor_file_name), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void gpx_header() {
        if (gps_file != null) {
            try {
                gps_file.append("<?xml version='1.0' encoding='UTF-8' standalone='yes' ?>\n");
                gps_file.append("<gpx version=\"1.1\" creator=\"OsmAnd+\" " +
                        "xmlns=\"http://www.topografix.com/GPX/1/1\" " +
                        "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                        "xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 " +
                        "http://www.topografix.com/GPX/1/1/gpx.xsd\">\n");
                gps_file.append("  <trk>\n");
                gps_file.append("    <trkseg>\n");
            } catch (IOException e) {
                Toast.makeText(getActivity().getApplicationContext(), "Could append to file: " + "" + e + " " + getAbsoluteFileName(gps_file_name), Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        }
    }

    public void gpx_footer() {
        if (gps_file != null) {
            try {
                gps_file.append("    </trkseg>\n");
                gps_file.append("  </trk>\n");
                gps_file.append("</gpx>\n");
            } catch (IOException e) {
                Toast.makeText(getActivity().getApplicationContext(), "Could append to file: " + "" + e + " " + getAbsoluteFileName(gps_file_name), Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        }
    }

    // http://stackoverflow.com/questions/6993322/how-do-i-get-the-hdop-or-vdop-values-from-the-gps-locationmanager
    @Override
    public void onLocationChanged(Location location) {
        String location_time_string,time_string,trkpt_start,trkpt_end,speed=null,hdop=null,provider_name=null,speed_cmt=null;
        String hasSpeed_name=null, hasAccuracy_name=null, hasBearing_name=null, Accuracy_name=null;
        String locationTimeString, systemTimeString;

        long system_timestamp,location_timestamp;
        boolean valid_data = false;

        // time from from last location fix :
        location_timestamp = location.getTime();

        // current time:
        system_timestamp = System.currentTimeMillis();

        textViewLatitude.setText(String.format(Locale.US, "%.8f", location.getLatitude()));
        textViewLongitude.setText(String.format(Locale.US, "%.8f", location.getLongitude()));
        textViewLocation.setText(location.getProvider());
        textViewAllTrackPoints.setText("All track points: " + all_log_counter++);

        trkpt_start = String.format(Locale.US,"     <trkpt lat=\"%.6f\" lon=\"%.6f\">\n", location.getLatitude(), location.getLongitude());
        time_string = String.format(Locale.US,"        <time>%s</time>\n",gpx_simpleDateFormat.format(system_timestamp));
        location_time_string = String.format(Locale.US,"        <cmt>LocationTime %s</cmt>\n        <cmt>SystemTime %s</cmt>\n",
                gpx_simpleDateFormat.format(location_timestamp), gpx_simpleDateFormat.format(system_timestamp));
        systemTimeString = String.format(Locale.US,"        <cmt>SystemTimeMS %d</cmt>\n",system_timestamp);
        locationTimeString = String.format(Locale.US,"        <cmt>LocationTimeMS %d</cmt>\n",location_timestamp);
        trkpt_end = String.format("     </trkpt>\n");

        if (location.hasAccuracy()) {
            hdop = String.format(Locale.US,"       <hdop>%.1f</hdop>\n", location.getAccuracy()/5.0);
        } else {
            hdop = null;
        }

        if ((((location.hasSpeed() && location.getAccuracy() < 30 && location.hasBearing() && location.hasAccuracy())
                ||(location.hasSpeed() && location.getAccuracy() < 10 && location.hasAccuracy()))
                && location_timestamp != gps_previous_location_timestamp && system_timestamp != gps_previous_system_timestamp)
                || MainActivity.FILTERED == false) {

            valid_data = true;

            textViewTrackPoints.setText("Valid track points: " + valid_log_counter++);

            if (first_location) {
                first_location = false;
                LastLatitude = location.getLatitude();
                LastLongitude = location.getLongitude();
            }

            Location.distanceBetween(location.getLatitude(), location.getLongitude(), LastLatitude, LastLongitude, Distance);

            TotalDistance  = TotalDistance + Distance[0];
            LastLatitude = location.getLatitude();
            LastLongitude = location.getLongitude();

            if (location.hasSpeed()) {
                if (MaxSpeed < location.getSpeed()) {
                    MaxSpeed = location.getSpeed();
                }

                CurrentSpeed = location.getSpeed();

                TotalSpeed = TotalSpeed + location.getSpeed();
                log_counter++;
                AverageSpeed = TotalSpeed / log_counter;
                speed = String.format(Locale.US, "        <extensions>\n          <speed>%.12f</speed>\n        </extensions>\n", CurrentSpeed);
                speed_cmt = String.format(Locale.US,"       <cmt> Speed %.12f LocationTimeStamp %d SystemTimeStamp %d </cmt>\n",CurrentSpeed, location_timestamp, system_timestamp);
            }

            if (location.hasAccuracy()) {
                Accuracy_name = String.format(Locale.US,"       <cmt>Accuracy %.6f</cmt>\n",location.getAccuracy());
            }

            textViewDistance.setText(String.format(Locale.US,"Distance: %.2f km",TotalDistance / 1000.0f));
            textViewAverageSpeed.setText(String.format(Locale.US,"Average Speed: %d km/h",(int)Math.round(AverageSpeed * 3.6)));
            textViewMaxSpeed.setText(String.format(Locale.US,"Max Speed: %d km/h",(int)Math.round(MaxSpeed * 3.6)));
            textViewCurrentSpeed.setText(String.format(Locale.US,"Current Speed: %d km/h",(int)Math.round(CurrentSpeed * 3.6)));

            provider_name = String.format(Locale.US,"       <cmt>Provider %s</cmt>\n",location.getProvider());
            hasSpeed_name = String.format(Locale.US,"       <cmt>hasSpeed %s</cmt>\n",location.hasSpeed());
            hasAccuracy_name = String.format(Locale.US,"       <cmt>hasAccuracy %s</cmt>\n",location.hasAccuracy());
            hasBearing_name = String.format(Locale.US,"       <cmt>hasBearing %s</cmt>\n",location.hasBearing());

        } else {
            valid_data = false;
            speed = null;
        }

        if (gps_file != null && valid_data) {
            try {
                gps_file.append(trkpt_start);
                gps_file.append(time_string);
                if (location_time_string != null) { gps_file.append(location_time_string); }
                if (systemTimeString != null) { gps_file.append(systemTimeString); }
                if (locationTimeString != null) { gps_file.append(locationTimeString); }
                if (hdop != null) { gps_file.append(hdop); }
                if (speed != null) { gps_file.append(speed); }
                if (speed != null) { gps_file.append(speed_cmt); }
                if (provider_name != null) { gps_file.append(provider_name); }
                if (hasSpeed_name != null) { gps_file.append(hasSpeed_name); }
                if (hasAccuracy_name != null) { gps_file.append(hasAccuracy_name); }
                if (hasBearing_name != null) { gps_file.append(hasBearing_name); }
                if (Accuracy_name != null) { gps_file.append(Accuracy_name); }
                gps_file.append(trkpt_end);
            } catch (IOException e) {
                Toast.makeText(getActivity().getApplicationContext(), "Could append to file: " + "" + e + " " + getAbsoluteFileName(gps_file_name), Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        }
        gps_previous_location_timestamp = location_timestamp;
        gps_previous_system_timestamp = system_timestamp;
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

    @Override
    public void onPause() {
        super.onPause();
        if (gps_file_name != null) {
            MediaScannerConnection.scanFile(getActivity().getApplicationContext(), new String[]{getAbsoluteFileName(gps_file_name)}, null, null);
        }

        if (sensor_file_name != null) {
            MediaScannerConnection.scanFile(getActivity().getApplicationContext(), new String[]{getAbsoluteFileName(sensor_file_name)}, null, null);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }

    @Override
    public void onResume() {
        super.onResume();
        // textViewDistance.setText(String.format(Locale.US, "TYPE_GPS: %s",MainActivity.log[MainActivity.TYPE_GPS]) );
        if (MainActivity.log[MainActivity.TYPE_GPS]) {
            location(true);
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if (MainActivity.log[MainActivity.TYPE_GPS]) {
            if (isVisibleToUser && isResumed()) {
                location(true);
            } else if (isResumed()) {
                // location(false);
            }
        } else {
            if (textViewGPSFile != null) {
                textViewGPSFile.setText("GPS file name (if GPS is enabled)");
            }
        }
    }
}
