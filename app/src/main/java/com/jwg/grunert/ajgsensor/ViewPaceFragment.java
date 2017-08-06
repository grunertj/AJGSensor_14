package com.jwg.grunert.ajgsensor;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.text.SimpleDateFormat;


/**
 * Created by werner-jens grunert on 6/22/2017.
 */
public class ViewPaceFragment extends ViewFragment {
    View view;
    // Bitmap bitmap;
    TextView textViewPaceFile;
    Button buttonPaceFile, buttonFitPace, buttonSharePace;
    GraphView graphPace;
    // RelativeLayout graphPaceLayout;
    LineGraphSeries<DataPoint> series;
    LineGraphSeries<DataPoint> gpxseries;
    List<DataPoint> gpx_data_points;
    double time_min = -10, time_max = -10, speed_min = -10, speed_max = -10;
    String filename;


    private class readFromFile extends AsyncTask<String, Integer, String > {
        File file;
        SimpleDateFormat dateFormat = new SimpleDateFormat("mm:ss", Locale.getDefault());

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
            double speed_m_per_s;

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
                        speed = -1 * ((60*60) / smooth.avg((float) (speed_m_per_s * 3.6)));
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

            textViewPaceFile.setText("Loading: " + file.getName() + " " + index);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            textViewPaceFile.setTextColor(Color.RED);
            textViewPaceFile.setText("Loading: " + file.getName());
        }


        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            textViewPaceFile.setTextColor(Color.GREEN);
            textViewPaceFile.setText("Finished: " + file.getName() + " " + speed_max);
            graphPace = (GraphView) view.findViewById(R.id.graphPace);
            graphPace.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
                @Override
                public String formatLabel(double value, boolean isValueX) {
                    if (isValueX) {
                        // show normal x values
                        return super.formatLabel(value, isValueX);
                    } else {
                        // return dateFormat.format(new Date (1 * (long) (-1 * value)));
                        // return dateFormat.format((600 * (long) (-1 * value)));
                        return formatSeconds((-1 * (int) value));
                        // return super.formatLabel(-1*value, isValueX);
                    }
                }
            });

            graphPace.getGridLabelRenderer().setNumVerticalLabels(10);

            graphPace.removeAllSeries();

            graphPace.getViewport().setXAxisBoundsManual(true);
            graphPace.getViewport().setMinX(time_min);
            graphPace.getViewport().setMaxX(time_max);

            graphPace.getViewport().setYAxisBoundsManual(true);
            graphPace.getViewport().setMinY(speed_min);
            graphPace.getViewport().setMaxY(speed_max);

            graphPace.getViewport().setScalable(true);
            graphPace.getViewport().setScalableY(true);

            GridLabelRenderer gridLabel = graphPace.getGridLabelRenderer();
            gridLabel.setHorizontalAxisTitle("Minutes");
            gridLabel.setVerticalAxisTitle("Minutes/km");
            graphPace.setTitle(filename);

            gpxseries.setThickness(3);
            graphPace.addSeries(gpxseries);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_viewpace, container, false);
        buttonPaceFile = (Button) view.findViewById(R.id.buttonPaceFile);
        buttonFitPace = (Button) view.findViewById(R.id.buttonFitPace);
        buttonSharePace = (Button) view.findViewById(R.id.buttonSharePace);
        textViewPaceFile = (TextView) view.findViewById(R.id.textViewPaceFile);
        graphPace = (GraphView) view.findViewById(R.id.graphPace);

        gpxseries = new LineGraphSeries<>(new DataPoint[] {
                new DataPoint(0, 1),
                new DataPoint(1, 5),
                new DataPoint(2, 3),
                new DataPoint(3, 2),
                new DataPoint(4, 6)
        });
        gpxseries.appendData(new DataPoint(5, 4), true, 1);
        gpxseries.appendData(new DataPoint(6, 0),true,1);
        graphPace.addSeries(gpxseries);

        time_min = 2;
        time_max = 6;
        speed_min = 0;
        speed_max = 8;

        textViewPaceFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView textView = ((TextView) v);

                String string_from_text_view = textView.getText().toString();
                String[] string_array = string_from_text_view.split(" ");
                String string = string_array[1];

                share_gpx(string);
            }
        });


        buttonPaceFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new FileChooser(getActivity(), MainFragment.directory_name, ".*location.*").setFileListener(new FileChooser.FileSelectedListener() {
                    @Override
                    public void fileSelected(File file) {
                        new readFromFile(file).execute();
                    }
                }).showDialog();
            }
        });

        buttonFitPace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fit_graph(time_min, time_max, speed_min, speed_max, gpxseries, graphPace);
            }
        });

        buttonSharePace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                share_png("plot_pace_share.png", gpxseries, graphPace);
            }
        });

        return view;
    }
}
