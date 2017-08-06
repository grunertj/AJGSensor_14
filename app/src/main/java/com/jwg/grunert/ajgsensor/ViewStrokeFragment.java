package com.jwg.grunert.ajgsensor;


import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.File;


public class ViewStrokeFragment extends Fragment {
    Button buttonFitStroke;
    Button buttonStrokeFile;
    TextView textViewStrokeFile;
    LineGraphSeries<DataPoint> series;
    DataPoint[] points;
    GraphView graph;

    public ViewStrokeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_viewstroke, container, false);

        buttonStrokeFile = (Button) view.findViewById(R.id.buttonStrokeFile);
        buttonFitStroke = (Button) view.findViewById(R.id.buttonFitStroke);
        textViewStrokeFile = (TextView) view.findViewById(R.id.textViewStrokeFile);

        graph = (GraphView) view.findViewById(R.id.graph);

        points = new DataPoint[100];

        for (int i = 0; i < points.length; i++) {
            points[i] = new DataPoint(i, Math.sin(i*0.5) * 20*(Math.random()*10+1));
        }

        series = new LineGraphSeries<>(points);

        // set manual X bounds
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinY(-150);
        graph.getViewport().setMaxY(150);

        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(4);
        graph.getViewport().setMaxX(80);

        // enable scaling and scrolling
        graph.getViewport().setScalable(true);
        graph.getViewport().setScalableY(true);
        graph.getViewport().setScrollable(true); // enables horizontal scrolling
        graph.getViewport().setScrollableY(true); // enables vertical scrolling

        graph.addSeries(series);
        //getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

        buttonStrokeFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new FileChooser(getActivity()).setFileListener(new FileChooser.FileSelectedListener() {
                    @Override
                    public void fileSelected(File file) {
                        textViewStrokeFile.setText(file.getName());
                    }
                }).showDialog();
            }
        });

        buttonFitStroke.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                graph.removeAllSeries();
                graph.getViewport().setYAxisBoundsManual(true);
                graph.getViewport().setMinY(-150);
                graph.getViewport().setMaxY(150);

                graph.getViewport().setXAxisBoundsManual(true);
                graph.getViewport().setMinX(4);
                graph.getViewport().setMaxX(80);

                // enable scaling and scrolling
                graph.getViewport().setScalable(true);
                graph.getViewport().setScalableY(true);
                graph.getViewport().setScrollable(true); // enables horizontal scrolling
                graph.getViewport().setScrollableY(true); // enables vertical scrolling

                graph.addSeries(series);
            }
        });

        return view;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser && isResumed()) {
            MainActivity.dim_screen(false, getActivity());
            // getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        } else if (isResumed()) {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }
}