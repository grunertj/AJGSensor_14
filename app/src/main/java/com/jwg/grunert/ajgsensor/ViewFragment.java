package com.jwg.grunert.ajgsensor;


import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;


import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 */
public class ViewFragment extends Fragment {
    public ViewFragment() {
        // Required empty public constructor
    }

    public boolean isPackageExisted(String targetPackage) {
        PackageManager pm = getActivity().getPackageManager();
        try {
            PackageInfo info = pm.getPackageInfo(targetPackage, PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
        return true;
    }

    // http://stackoverflow.com/questions/691184/scanner-vs-stringtokenizer-vs-string-split
    public static ArrayList<String> splitBySingleChar(final char[] s,
                                                      final char splitChar) {
        final ArrayList<String> result = new ArrayList<String>();
        final int length = s.length;
        int offset = 0;
        int count = 0;
        for (int i = 0; i < length; i++) {
            if (s[i] == splitChar) {
                if (count > 0) {
                    result.add(new String(s, offset, count));
                }
                offset = i + 1;
                count = 0;
            } else {
                count++;
            }
        }
        if (count > 0) {
            result.add(new String(s, offset, count));
        }
        return result;
    }

    public  String formatSeconds(int timeInSeconds){
        int hours = timeInSeconds / 3600;
        int secondsLeft = timeInSeconds - hours * 3600;
        int minutes = secondsLeft / 60;
        int seconds = secondsLeft - minutes * 60;

        String formattedTime = "";
            /*
            if (hours < 10)
                formattedTime += "0";
            formattedTime += hours + ":";
            */

        if (minutes < 10)
            formattedTime += "0";
        formattedTime += minutes + ":";

        if (seconds < 10)
            formattedTime += "0";
        formattedTime += seconds ;

        return formattedTime;
    }

    public void share_gpx (String string) {
        Uri uri = Uri.fromFile(new File(MainFragment.getAbsoluteFileName(string)));

        if (isPackageExisted("com.vecturagames.android.app.gpxviewer")) {
            Intent email = new Intent(Intent.ACTION_SEND);
            email.setPackage("com.vecturagames.android.app.gpxviewer");
            email.setAction(Intent.ACTION_SEND);
            email.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            email.putExtra(Intent.EXTRA_STREAM,uri);
            email.setType("text/plain");
            startActivity(email);
        }
    }

    public void fit_graph (double x_min, double x_max, double y_min, double y_max, LineGraphSeries<DataPoint> gpx_series, GraphView graph) {
        graph.removeAllSeries();

        // Paint paint = new Paint();
        // paint.setStyle(Paint.Style.FILL_AND_STROKE);

        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(x_min);
        graph.getViewport().setMaxX(x_max);

        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinY(y_min);
        graph.getViewport().setMaxY(y_max);

        graph.getViewport().setScalable(true);
        graph.getViewport().setScalableY(true);

        gpx_series.setThickness(2);
        //gpxseries.setDrawDataPoints(true);
        //gpxseries.setDataPointsRadius(10);

        //gpxseries.setCustomPaint(paint);

        // gpxseries.setDrawBackground(true);
        // gpxseries.setBackgroundColor(10);

        graph.addSeries(gpx_series);
    }

    public void share_png (String png_file, LineGraphSeries<DataPoint> gpx_series, GraphView graph) {
        Bitmap bitmap = null;
        double time_min_zoom = 0, time_max_zoom = 0, speed_min_zoom = 0, speed_max_zoom = 0;

        time_min_zoom = graph.getViewport().getMinX(false);
        time_max_zoom = graph.getViewport().getMaxX(false);
        speed_min_zoom = graph.getViewport().getMinY(false);
        speed_max_zoom = graph.getViewport().getMaxY(false);

        final int backgroundPreviousColor = graph.getDrawingCacheBackgroundColor();

        graph.invalidate();

        graph.setDrawingCacheEnabled(true);
        graph.setDrawingCacheBackgroundColor(0xffffffff);

        graph.removeAllSeries();
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(time_min_zoom);
        graph.getViewport().setMaxX(time_max_zoom);
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinY(speed_min_zoom);
        graph.getViewport().setMaxY(speed_max_zoom);
        graph.getViewport().setScalable(true);
        graph.getViewport().setScalableY(true);
        gpx_series.setThickness(2);

        graph.addSeries(gpx_series);

        if (bitmap != null) {
            bitmap.recycle();
            bitmap = null;
        }

        bitmap = Bitmap.createScaledBitmap(graph.getDrawingCache(), 1920, 1200, true);
        graph.setDrawingCacheBackgroundColor(backgroundPreviousColor);
        graph.setDrawingCacheEnabled(false);

        File f;
        f = new File(MainFragment.getAbsoluteFileName(png_file));

        try {
            FileOutputStream ostream = new FileOutputStream(f);
            bitmap.compress(Bitmap.CompressFormat.PNG, 5, ostream);
            ostream.close();
            f.setReadable(true,false);

            Uri uri = Uri.fromFile(f);
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            sendIntent.putExtra(Intent.EXTRA_STREAM, uri);
            sendIntent.setData(uri);
            sendIntent.setType("image/png");

            Toast.makeText(getActivity().getApplicationContext(), "Sharing file: " + uri.toString(), Toast.LENGTH_SHORT).show();
            startActivity(sendIntent);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean copyFile(File src,File dst)throws IOException{
        if(src.getAbsolutePath().toString().equals(dst.getAbsolutePath().toString())){

            return true;

        }else{
            InputStream is=new FileInputStream(src);
            OutputStream os=new FileOutputStream(dst);
            byte[] buff=new byte[1024];
            int len;
            while((len=is.read(buff))>0){
                os.write(buff,0,len);
            }
            is.close();
            os.close();
        }
        return true;
    }

    private static void fileCopyUsingNIOChannelClass(String fileToCopyString, String newFileString) throws IOException {
        File fileToCopy = new File(fileToCopyString);
        FileInputStream inputStream = new FileInputStream(fileToCopy);
        FileChannel inChannel = inputStream.getChannel();

        File newFile = new File(newFileString);
        FileOutputStream outputStream = new FileOutputStream(newFile);
        FileChannel outChannel = outputStream.getChannel();

        inChannel.transferTo(0, fileToCopy.length(), outChannel);

        inputStream.close();
        outputStream.close();
    }

    public File exportFile(File src, File dst) throws IOException {
        //if folder does not exist
        if (!dst.exists()) {
            if (!dst.mkdir()) {
                return null;
            }
        }

        FileChannel inChannel = null;
        FileChannel outChannel = null;

        try {
            inChannel = new FileInputStream(src).getChannel();
            outChannel = new FileOutputStream(dst).getChannel();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        try {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } finally {
            if (inChannel != null)
                inChannel.close();
            if (outChannel != null)
                outChannel.close();
        }

        return dst;
    }

    public void view_png (String png_file, LineGraphSeries<DataPoint> gpx_series, GraphView graph) {
        Bitmap bitmap = null;
        double time_min_zoom = 0, time_max_zoom = 0, speed_min_zoom = 0, speed_max_zoom = 0;

        time_min_zoom = graph.getViewport().getMinX(false);
        time_max_zoom = graph.getViewport().getMaxX(false);
        speed_min_zoom = graph.getViewport().getMinY(false);
        speed_max_zoom = graph.getViewport().getMaxY(false);

        final int backgroundPreviousColor = graph.getDrawingCacheBackgroundColor();

        graph.invalidate();

        graph.setDrawingCacheEnabled(true);
        graph.setDrawingCacheBackgroundColor(0xffffffff);

        graph.removeAllSeries();
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(time_min_zoom);
        graph.getViewport().setMaxX(time_max_zoom);
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMinY(speed_min_zoom);
        graph.getViewport().setMaxY(speed_max_zoom);
        graph.getViewport().setScalable(true);
        graph.getViewport().setScalableY(true);
        gpx_series.setThickness(2);

        graph.addSeries(gpx_series);

        if (bitmap != null) {
            bitmap.recycle();
            bitmap = null;
        }

        bitmap = Bitmap.createScaledBitmap(graph.getDrawingCache(), 1920, 1200, true);
        graph.setDrawingCacheBackgroundColor(backgroundPreviousColor);
        graph.setDrawingCacheEnabled(false);

        File f;
        f = new File(MainFragment.getAbsoluteFileName(png_file));

        try {
            FileOutputStream ostream = new FileOutputStream(f);
            bitmap.compress(Bitmap.CompressFormat.PNG, 5, ostream);
            ostream.close();
            f.setReadable(true, false);

            String src = f.toString();
            // String dest = getContext().getFilesDir().getAbsolutePath().toString() + File.separator + Environment.DIRECTORY_DOWNLOADS + File.separator + "share.png";
            String dest = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + File.separator + png_file;

            fileCopyUsingNIOChannelClass(src,dest);

            File f_copy = new File(dest);
            f_copy.setReadable(true,false);

            String extension = android.webkit.MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(f_copy).toString());
            String mimetype = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);

            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_VIEW);

            sendIntent.setDataAndType(Uri.fromFile(f_copy), mimetype);

            Toast.makeText(getActivity().getApplicationContext(), "Sharing file: " + f_copy.toString(), Toast.LENGTH_SHORT).show();
            startActivity(sendIntent);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser && isResumed()) {
            MainActivity.dim_screen(false, getActivity());
            // getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        } else if (isResumed()) {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }
}
