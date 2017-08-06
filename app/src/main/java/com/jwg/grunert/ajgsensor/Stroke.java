package com.jwg.grunert.ajgsensor;

public class Stroke {
  float previous = 0.0f;
  float previous_time = 0.0f;
  float ouput = 0.0f;
  int start_rising = 0;
  float t_start_rising = 0.0f;
  float t_stop_rising = 0.0f;

  public Stroke () {
  }

  float strokes(float time, float input) {
    float t;
    if ( previous == 0.0f && previous_time == 0.0f ) {
      previous_time = time;
      previous = input;
    } else if ((input < 0 && previous > 0) || (input > 0 && previous < 0)) {
      ouput = 1;
      t = time - previous_time;
      previous_time = time;
      ouput = t;
    } else {
      ouput = 0.0f;
    }
    previous = input;
    if ( ouput < 0 ) {
      ouput = ouput * -1.0f;
    }
    return ouput;
  }

  float strokes(float time, float input, float threshold) {
    if (start_rising == 0 && input < (-1.0f * threshold)) {
      start_rising = 1;
      t_start_rising = time;
    } else if (start_rising == 1 && input > threshold && time > t_start_rising) {
      start_rising = 0;
      t_stop_rising = time;
      ouput = t_stop_rising - t_start_rising;
    } else {
      ouput = 0.0f;
    }
    if ( ouput < 0 ) {
      ouput = ouput * -1.0f;
    }
    return ouput;
  }
}

