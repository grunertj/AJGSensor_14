package com.jwg.grunert.ajgsensor;


import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;


public class DebugFragment extends Fragment {
    Button buttonSensors, buttonShare;
    TextView textViewSensors;

    SensorManager sensorManager = null;
    String phone = Build.MODEL+" "+Build.BRAND+" "+Build.MANUFACTURER+" "+Build.VERSION.RELEASE;

    public DebugFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_debug, container, false);
        //getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        textViewSensors = (TextView) view.findViewById(R.id.textViewSensors);
        buttonSensors = (Button) view.findViewById(R.id.buttonSensors);
        buttonShare = (Button) view.findViewById(R.id.buttonShare);
        buttonShare.setEnabled(false);

        buttonSensors.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textViewSensors.setText(""+phone+"\n");
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

                for (Sensor sensor: sensors) {
                    textViewSensors.append(sensor.getType()+"\n\t"+sensor.getName() + "\n\t"+sensor.getVendor()+"\n\n");
                }
                sensorManager = null;
                buttonShare.setEnabled(true);
            }
        });

        buttonShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                String string = textViewSensors.getText().toString();
                sharingIntent.setType("text/plain");
                sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Phone: "+phone);
                sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, string);
                startActivity(sharingIntent);
            }
        });


        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    MainActivity.dim_state = MainActivity.dim_screen(false, getActivity());
                }
                return false;
            }
        });



        return view;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser && isResumed()) {
            MainActivity.dim_screen(false, getActivity());
        } else if (isResumed()) {

        }
    }
}