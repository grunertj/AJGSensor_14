package com.jwg.grunert.ajgsensor;
// http://code.hootsuite.com/orientation-changes-on-android/

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Debug;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ViewSpeedFragment extends ViewFragment {
    Bitmap bitmap;
    Button buttonSpeedFile;
    Button buttonFitSpeed,buttonShareSpeed;
    TextView textViewSpeedFile;
    LineGraphSeries<DataPoint> gpxseries;
    GraphView graph;
    View view;
    List<DataPoint> gpx_data_points;
    double time_min = -10, time_max = -10, speed_min = -10, speed_max = -10;
    double speed_m_per_s;
    String filename;

    public ViewSpeedFragment() {
        // Required empty public constructor
    }

    private class readFromFile extends AsyncTask<String, Integer, String > {
        File file;

        public readFromFile(File file) {
            this.file = file;
            filename = this.file.toString();
            String[] array;
            String[] array1;
            array = filename.split("-");
            array1 = array[2].split("_");
            filename = array1[2] + "." +  array1[1] + "." + array1[0] ;
        }

        @Override
        protected String doInBackground(String... params) {

            String[] array;
            ArrayList<String> array_list;
            long timestamp = 0 ,offset = 0;
            double speed = 0, time = 0;
            Smooth smooth = new Smooth(10,10.0f);
            Kalman kalman = new Kalman(1f,1f,10.5f);
            gpx_data_points = new ArrayList<DataPoint>();
            time_min = -10;
            time_max = -10; speed_min = -10; speed_max = -10;
            try {
                publishProgress(2);
                Scanner scan = new Scanner(file);
                scan.useDelimiter("\\Z");
                String content = scan.next();
                content = content.replaceAll("<cmt>","<cmt> ");
                content = content.replaceAll("</cmt>"," </cmt>");
                if (scan != null) {
                    scan.close();
                }

                String lines[] = content.split(System.getProperty("line.separator"));

                int in = 0;

                publishProgress(in++);
                // start tracing to "/sdcard/ajgsensor_read_file.trace"
                //Debug.startMethodTracing("ajgsensor_parse_file");
                /*
                for (String line: lines) {
                    array = line.split("\\s+");
                    array_list = splitBySingleChar(line.toCharArray(), ' ');
                    if ( array.length == 9 && array[2].matches("Speed") ) {
                        publishProgress(in++);
                        timestamp = Long.parseLong(array[7]);
                        if (offset == 0 ) {
                            offset = timestamp;
                        }
                        time = ((timestamp - offset)/1000.0)/60.0;
                        speed = smooth.avg((float)(Double.parseDouble(array[3]) * 3.6));
                        if (time_min == -10) {
                            time_min = time;
                            time_max = time;
                            speed_min = speed;
                            speed_max = speed;
                        } else {
                            if ( time > time_max ) { time_max = time; }
                            if ( speed > speed_max) { speed_max = speed; }
                            if ( speed < speed_min ) { speed_min = speed; }
                        }
                        gpx_data_points.add(new DataPoint(time, speed));
                    }
                }
                */
                for (String line: lines) {
                    array_list = splitBySingleChar(line.toCharArray(), ' ');
                    if ( array_list.size() == 8 && array_list.get(1).matches("Speed") && (speed_m_per_s = Double.parseDouble(array_list.get(2))) > 2 ) {
                        // System.out.println("Jens: " + array_list.size() + " " + line);
                        publishProgress(in++);
                        timestamp = Long.parseLong(array_list.get(6));
                        if (offset == 0 ) {
                            offset = timestamp;
                        }
                        time = ((timestamp - offset)/1000.0)/60.0;
                        speed = smooth.avg((float) (speed_m_per_s * 3.6));
                        if (time_min == -10) {
                            time_min = time;
                            time_max = time;
                            speed_min = speed;
                            speed_max = speed;
                        } else {
                            if ( time > time_max ) { time_max = time; }
                            if ( speed > speed_max) { speed_max = speed; }
                            if ( speed < speed_min ) { speed_min = speed; }
                        }
                        gpx_data_points.add(new DataPoint(time, speed));
                    }
                }

                //Debug.stopMethodTracing();

                publishProgress(2);

                int gpx_data_points_size = gpx_data_points.size();

                DataPoint[] points = new DataPoint[gpx_data_points_size];

                for (int i = 0; i < gpx_data_points_size; i++) {
                    points[i] = new DataPoint(gpx_data_points.get(i).getX(),gpx_data_points.get(i).getY());
                }

                gpxseries = new LineGraphSeries<>(points);
                gpx_data_points.clear();

                publishProgress(3);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);

            int index = values[0];

            textViewSpeedFile.setText("Loading: " + file.getName() + " " + index);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            textViewSpeedFile.setTextColor(Color.RED);
            textViewSpeedFile.setText("Loading: " + file.getName());
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            textViewSpeedFile.setTextColor(Color.GREEN);
            textViewSpeedFile.setText("Finished: " + file.getName() + " " + speed_max);
            graph = (GraphView) view.findViewById(R.id.graph);

            graph.getGridLabelRenderer().setNumVerticalLabels(10);

            graph.removeAllSeries();

            graph.getViewport().setXAxisBoundsManual(true);
            graph.getViewport().setMinX(time_min);
            graph.getViewport().setMaxX(time_max);

            graph.getViewport().setYAxisBoundsManual(true);
            graph.getViewport().setMinY(speed_min);
            graph.getViewport().setMaxY(speed_max);

            graph.getViewport().setScalable(true);
            graph.getViewport().setScalableY(true);

            GridLabelRenderer gridLabel = graph.getGridLabelRenderer();
            gridLabel.setHorizontalAxisTitle("Minutes");
            gridLabel.setVerticalAxisTitle("km/h");
            graph.setTitle(filename);

            gpxseries.setThickness(3);

            graph.addSeries(gpxseries);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_viewspeed, container, false);

        // getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        buttonSpeedFile = (Button) view.findViewById(R.id.buttonSpeedFile);
        buttonFitSpeed = (Button) view.findViewById(R.id.buttonFitSpeed);
        buttonShareSpeed = (Button) view.findViewById(R.id.buttonShareSpeed);
        textViewSpeedFile = (TextView) view.findViewById(R.id.textViewSpeedFile);
        graph = (GraphView) view.findViewById(R.id.graph);

        gpxseries = new LineGraphSeries<>(new DataPoint[] {
                new DataPoint(0, 1),
                new DataPoint(1, 5),
                new DataPoint(2, 3),
                new DataPoint(3, 2),
                new DataPoint(4, 6)
        });
        gpxseries.appendData(new DataPoint(5, 4), true, 1);
        gpxseries.appendData(new DataPoint(6, 0),true,1);
        graph.addSeries(gpxseries);

        time_min = 2;
        time_max = 6;
        speed_min = 0;
        speed_max = 8;

        textViewSpeedFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView textView = ((TextView) v);

                String string_from_text_view = textView.getText().toString();
                String[] string_array = string_from_text_view.split(" ");
                String string = string_array[1];

                share_gpx(string);
            }
        });

        buttonSpeedFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new FileChooser(getActivity(),MainFragment.directory_name,".*location.*").setFileListener(new FileChooser.FileSelectedListener() {
                    @Override
                    public void fileSelected(File file) {
                        new readFromFile(file).execute();
                    }
                }).showDialog();
            }
        });

        buttonFitSpeed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fit_graph(time_min, time_max, speed_min, speed_max, gpxseries, graph);
            }
        });

        buttonShareSpeed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                view_png("plot_speed_share.png", gpxseries, graph);
            }
        });

        return view;
    }
}