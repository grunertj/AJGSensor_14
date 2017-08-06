package com.jwg.grunert.ajgsensor;

/**
 * Created by Werner-Jens Grunert on 4/7/2017.
 */

public class Kalman {
    private float x_est = 50f , x_est_last = 50f, x_temp_est = 50f;
    private float Q = 0.022f;
    private float R = 0.617f;
    private float P = 0.0f, P_last = 0.0f, P_temp = 0.0f;
    private float K = 0.5f;

    public Kalman (float q, float r, float est) {
        Q = q;
        R = r;
        x_est_last = est;
    }

    public float filter (String z_measured_string) {
        float z_measured = 0.0f;
        z_measured = Float.parseFloat(z_measured_string);
        //do a prediction
        x_temp_est = x_est_last;
        P_temp = P_last + Q;
        //calculate the Kalman gain
        K = P_temp * (1.0f/(P_temp + R));
        //correct
        x_est = x_temp_est + K * (z_measured - x_temp_est);
        P = (1.0f - K) * P_temp;
        //update our last's
        P_last = P;
        x_est_last = x_est;

        return x_est;
    }

    public float filter (float z_measured) {
        //do a prediction
        x_temp_est = x_est_last;
        P_temp = P_last + Q;
        //calculate the Kalman gain
        K = P_temp * (1.0f/(P_temp + R));
        //correct
        x_est = x_temp_est + K * (z_measured - x_temp_est);
        P = (1.0f - K) * P_temp;
        //update our last's
        P_last = P;
        x_est_last = x_est;

        return x_est;
    }
}

