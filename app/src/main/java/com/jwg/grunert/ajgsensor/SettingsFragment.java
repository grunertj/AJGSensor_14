package com.jwg.grunert.ajgsensor;


import android.content.Context;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SettingsFragment extends Fragment {
    CheckBox checkBox, checkBoxCompress, checkBoxGPS, checkBoxDelayed;
    ArrayList<CheckBox> checkBoxes;
    SensorManager sensorManager = null;
    RadioGroup radioGroup, radioGroup2;

    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        //getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        LinearLayout linearLayout = (LinearLayout)view.findViewById(R.id.linearLayout);
        radioGroup = (RadioGroup)view.findViewById(R.id.radioGroup);
        radioGroup2 = (RadioGroup)view.findViewById(R.id.radioGroup2);
        checkBoxCompress = (CheckBox)view.findViewById(R.id.checkBoxCompress);
        checkBoxDelayed = (CheckBox)view.findViewById(R.id.checkBoxDelayed);

        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    MainActivity.dim_screen(false, getActivity());
                }
                return false;
            }
        });

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.radioButtonOverride:
                        MainActivity.file_mode = MainActivity.OVERRIDE;
                        break;
                    case R.id.radioButtonNew:
                        MainActivity.file_mode = MainActivity.NEW;
                        break;
                    default:
                        MainActivity.file_mode = MainActivity.NEW;
                        break;
                }
            }
        });

        radioGroup2.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.radioButtonDefault:
                        MainActivity.SENSOR_DELAY = SensorManager.SENSOR_DELAY_NORMAL;
                        break;
                    case R.id.radioButtonGame:
                        MainActivity.SENSOR_DELAY = SensorManager.SENSOR_DELAY_GAME;
                        break;
                    case R.id.radioButtonFastest:
                        MainActivity.SENSOR_DELAY = SensorManager.SENSOR_DELAY_FASTEST;
                        break;
                    default:
                        MainActivity.SENSOR_DELAY = SensorManager.SENSOR_DELAY_NORMAL;
                        break;
                }
            }
        });

        checkBoxCompress.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    MainActivity.COMPRESS = true;
                } else {
                    MainActivity.COMPRESS = false;
                }
            }
        });

        checkBoxDelayed.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    MainActivity.FILTERED = true;
                } else {
                    MainActivity.FILTERED = false;
                }
            }
        });

        checkBoxCompress.setChecked(true);
        checkBoxDelayed.setChecked(true);

        sensorManager = (SensorManager)getActivity().getSystemService(Context.SENSOR_SERVICE);
        List<Sensor> list = sensorManager.getSensorList(Sensor.TYPE_ALL);
        ArrayList<Sensor> sensors = new ArrayList<Sensor>();
        sensors.addAll(list);

        Collections.sort(sensors, new Comparator<Sensor>() {
            @Override
            public int compare(Sensor lhs, Sensor rhs) {
                return lhs.getType() - rhs.getType();
            }
        });


        checkBoxes = new ArrayList<CheckBox>();

        checkBoxGPS = new CheckBox(view.getContext());
        checkBoxGPS.setText("GPS Location");
        checkBoxGPS.setChecked(true);
        checkBoxGPS.setId(MainActivity.TYPE_GPS);

        checkBoxGPS.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    LocationManager locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
                    if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) == false) {
                        MainActivity.log[MainActivity.TYPE_GPS] = false;
                        checkBoxGPS.setChecked(false);
                        Toast.makeText(getActivity().getApplicationContext(), "Please enable Location.", Toast.LENGTH_LONG).show();
                    } else {
                        MainActivity.log[MainActivity.TYPE_GPS] = true;
                    }
                } else {
                    MainActivity.log[MainActivity.TYPE_GPS] = false;
                }
                MainActivity.log[MainActivity.TYPE_GPS] = true;
            }
        });

        checkBoxes.add(checkBoxGPS);
        linearLayout.addView(checkBoxGPS);

        for (Sensor sensor: sensors) {
            switch (sensor.getType()) {
                case Sensor.TYPE_ACCELEROMETER:
                case Sensor.TYPE_LINEAR_ACCELERATION:
                case Sensor.TYPE_GYROSCOPE:
                    checkBox = new CheckBox(view.getContext());
                    checkBox.setText(sensor.getType() + " " + sensor.getName() + " " + sensor.getVendor());
                    checkBox.setChecked(false);
                    checkBox.setId(sensor.getType());
                    checkBoxes.add(checkBox);
                    linearLayout.addView(checkBox);
                    break;
                // case Sensor.TYPE_STEP_DETECTOR:
                // case Sensor.TYPE_STEP_COUNTER:
                case Sensor.TYPE_GYROSCOPE_UNCALIBRATED:
                case Sensor.TYPE_ROTATION_VECTOR:
                    checkBox = new CheckBox(view.getContext());
                    checkBox.setText(sensor.getType() + " " + sensor.getName() + " " + sensor.getVendor());
                    checkBox.setChecked(false);
                    checkBox.setId(sensor.getType());
                    checkBoxes.add(checkBox);
                    linearLayout.addView(checkBox);
                    break;
                default:
                    break;
            }
        }

        sensorManager = null;

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if (isVisibleToUser && isResumed()) {
            MainActivity.dim_screen(false, getActivity());
        } else if (isResumed()) {
            for (CheckBox checkBox: checkBoxes) {
                MainActivity.log[checkBox.getId()] = checkBox.isChecked();
            }
        }
    }
}