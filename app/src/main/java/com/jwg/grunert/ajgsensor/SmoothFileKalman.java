package com.jwg.grunert.ajgsensor;

import com.jwg.grunert.ajgsensor.Kalman;
import java.io.*;
import java.util.zip.*;

// http://www.xyzws.com/javafaq/how-to-make-a-gzip-file-in-java/215

public class SmoothFileKalman {
    public static void main(String[] args) {
	String fname;

	if (args.length > 0) {
	  fname = args[0];
	} else {
	  fname = "sensor-2017_04_04_17_23_45.txt";
	}

	// process_sensor(fname,"strokes.dat");
	process_speed("location-2017_04_04_17_23_45.gpx","speed.dat",process_sensor(fname,"strokes.dat"));

    }

    static void process_speed(String gpx_input, String gpx_output) {
        BufferedWriter speeds = null;
        long loc_timestamp,timestamp,offset = 0;
	float time,speed = 0.0f;
        Smooth smooth = new Smooth(10,10.0f);
	Kalman kalman = new Kalman(1f,1f,10.5f);

	String[] array;

	String line = null;

	try {
          FileReader fileReader = new FileReader(gpx_input);
          BufferedReader bufferedReader = new BufferedReader(fileReader);
          speeds = new BufferedWriter(new FileWriter(gpx_output));

          while((line = bufferedReader.readLine()) != null) {
            array = line.split("\\s+");

	    if ( array.length == 9 && array[2].matches("Speed") ) {
	      timestamp = Long.parseLong(array[7]);
	      loc_timestamp = Long.parseLong(array[5]);
              speed = Float.parseFloat(array[3]) * 3.6f;

              if (offset == 0 ) {
                offset = timestamp;
              }

              time = (float)(timestamp - offset)/1000.0f;

	      speeds.append(String.format("%.6f %.6f %.6f %.6f %d %d\n",time,speed,smooth.avg(speed),kalman.filter(speed),loc_timestamp,timestamp));

              // System.out.println( "Time: " + time + " " + "Speed: " + speed);                  
	    }
	  }

  	  bufferedReader.close();
  	  speeds.close();
	} catch(FileNotFoundException ex) {
           System.out.println( "Unable to open file '" + gpx_input + "'");                
        } catch(IOException ex) {
           System.out.println( "Error reading file '" + gpx_input + "'");                  
        }
    }

    static void process_speed(String gpx_input, String gpx_output, long offset) {
        BufferedWriter speeds = null;
        long loc_timestamp,timestamp;
	float time,speed = 0.0f;
        Smooth smooth = new Smooth(10,10.0f);
	Kalman kalman = new Kalman(1f,1f,10.5f);

	String[] array;

	String line = null;

	try {
          FileReader fileReader = new FileReader(gpx_input);
          BufferedReader bufferedReader = new BufferedReader(fileReader);
          speeds = new BufferedWriter(new FileWriter(gpx_output));

          while((line = bufferedReader.readLine()) != null) {
            array = line.split("\\s+");

	    if ( array.length == 9 && array[2].matches("Speed") ) {
	      timestamp = Long.parseLong(array[7]);
	      loc_timestamp = Long.parseLong(array[5]);
              speed = Float.parseFloat(array[3]) * 3.6f;

              time = (float)(timestamp - offset)/1000.0f;

	      speeds.append(String.format("%.6f %.6f %.6f %.6f %d %d\n",time,speed,smooth.avg(speed),kalman.filter(speed),loc_timestamp,timestamp));

              // System.out.println( "Time: " + time + " " + "Speed: " + speed);                  
	    }
	  }

  	  bufferedReader.close();
  	  speeds.close();
	} catch(FileNotFoundException ex) {
           System.out.println( "Unable to open file '" + gpx_input + "'");                
        } catch(IOException ex) {
           System.out.println( "Error reading file '" + gpx_input + "'");                  
        }
    }

    static long process_sensor(String sensor_input, String sensor_output) {
        BufferedWriter strokes = null;
        float time,x,sxa,xs,ys,zs;
	Kalman kalman = new Kalman(0.2f,100f,70.5f);
        Smooth smooth_x = new Smooth(200,70.0f);
        long timestamp,offset = 0;

	Stroke stroke = new Stroke();
	Biquad lpFilterLP = new Biquad();
	Biquad lpFilterHP = new Biquad();

        lpFilterLP.setBiquad(Biquad.bq_type_lowpass,1/100.0,0.707,0.0);
	lpFilterHP.setBiquad(Biquad.bq_type_highpass,0.2/100.0,0.707,0.0);

	String[] array;

	String line = null;

	try {
          FileReader fileReader = new FileReader(sensor_input);
          BufferedReader bufferedReader = new BufferedReader(fileReader);
          strokes = new BufferedWriter(new FileWriter(sensor_output));
          // bufferedWriter = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream("liner_acceleration_smooth.dat.gz"))));
  
          while((line = bufferedReader.readLine()) != null) {
            array = line.split("\\s+");
	    if ( array[2].matches("1") ) {
	      timestamp = Long.parseLong(array[1]);
              if (offset == 0 ) {
                offset = timestamp;
              }

              time = (float)(timestamp - offset)/1000.0f;
              // System.out.println( "time: " + time);                  

              xs = Float.parseFloat(array[3]);
              ys = Float.parseFloat(array[4]);
              zs = Float.parseFloat(array[5]);

	      x = (float)Math.sqrt(xs*xs+ys*ys+zs*zs);
	    } else {
              continue;
	    }

	    sxa = stroke.strokes(time,(float)lpFilterLP.process((float)lpFilterHP.process(x)));
	    
	    if ( sxa > 0.0f ) {
	      sxa = 60.0f / (sxa * 2.0f);
	      if ( sxa > 50.0f && sxa < 90.f ) {
	        strokes.append(String.format("%.6f %.6f %.6f %.6f %d\n",time,sxa,smooth_x.avg(sxa),kalman.filter(sxa),timestamp));
	      }
	    }
  	  }

  	  bufferedReader.close();
  	  strokes.close();
	} catch(FileNotFoundException ex) {
           System.out.println( "Unable to open file '" + sensor_input + "'");                
        } catch(IOException ex) {
           System.out.println( "Error reading file '" + sensor_input + "'");                  
        }
        return offset;
    }
}

