package com.jwg.grunert.ajgsensor;

public class Biquad {
  public static final int bq_type_lowpass = 0;
  public static final int bq_type_highpass = 1;
  public static final int bq_type_bandpass = 2;
  public static final int bq_type_notch = 3;
  public static final int bq_type_peak = 4;
  public static final int bq_type_lowshelf = 5;
  public static final int bq_type_highshelf = 6;
  private int type;
  private double a0, a1, a2, b1, b2;
  private double Fc, Q, peakGain;
  private double z1, z2;
  private double out;

  public Biquad () {
    type = bq_type_lowpass;
    a0 = 1.0;
    a1 = a2 = b1 = b2 = 0.0;
    Fc = 0.50;
    Q = 0.707;
    peakGain = 0.0;
    z1 = z2 = 0.0;
  }

  public Biquad(int _type, double _Fc, double _Q, double _peakGainDB) {
    setBiquad(_type, _Fc, _Q, _peakGainDB);
    z1 = z2 = 0.0;
  }

  void setType(int _type) {
      type = _type;
      calcBiquad();
  }
  
  void setQ(double _Q) {
      Q = _Q;
      calcBiquad();
  }
  
  void setFc(double _Fc) {
      Fc = _Fc;
      calcBiquad();
  }
  
  void setPeakGain(double _peakGainDB) {
      peakGain = _peakGainDB;
      calcBiquad();
  }
      
  void setBiquad(int _type, double _Fc, double _Q, double _peakGainDB) {
      type = _type;
      Q = _Q;
      Fc = _Fc;
      setPeakGain(_peakGainDB);
  }

  double fabs(double in) {
    if (in < 0) {
      return -1.0 *in;
    } else {
      return in;
    }
  }

  void calcBiquad() {
      double norm = 0;
      double V = Math.pow(10, fabs(peakGain) / 20.0);
      // System.out.println(String.format("V: %.6f rest: %.6f\n",V,peakGain));
      double K = Math.tan(Math.PI * Fc);
      switch (type) {
          case bq_type_lowpass:
              norm = 1 / (1 + K / Q + K * K);
              a0 = K * K * norm;
              a1 = 2 * a0;
              a2 = a0;
              b1 = 2 * (K * K - 1) * norm;
              b2 = (1 - K / Q + K * K) * norm;
      // System.out.println(String.format("a0: %.6f a1: %.6f b1: %.6f b2: %.6f V: %.6f K: %.6f type: %d norm: %.6f Q: %.6f Fc: %.6f\n",a0,a1,b1,b2,V,K,type,norm,Q,Fc));
              break;
              
          case bq_type_highpass:
              norm = 1 / (1 + K / Q + K * K);
              a0 = 1 * norm;
              a1 = -2 * a0;
              a2 = a0;
              b1 = 2 * (K * K - 1) * norm;
              b2 = (1 - K / Q + K * K) * norm;
              break;
              
          case bq_type_bandpass:
              norm = 1 / (1 + K / Q + K * K);
              a0 = K / Q * norm;
              a1 = 0;
              a2 = -a0;
              b1 = 2 * (K * K - 1) * norm;
              b2 = (1 - K / Q + K * K) * norm;
              break;
              
          case bq_type_notch:
              norm = 1 / (1 + K / Q + K * K);
              a0 = (1 + K * K) * norm;
              a1 = 2 * (K * K - 1) * norm;
              a2 = a0;
              b1 = a1;
              b2 = (1 - K / Q + K * K) * norm;
              break;
          case bq_type_peak:
              if (peakGain >= 0) {    // boost
                  norm = 1 / (1 + 1/Q * K + K * K);
                  a0 = (1 + V/Q * K + K * K) * norm;
                  a1 = 2 * (K * K - 1) * norm;
                  a2 = (1 - V/Q * K + K * K) * norm;
                  b1 = a1;
                  b2 = (1 - 1/Q * K + K * K) * norm;
              }
              else {    // cut
                  norm = 1 / (1 + V/Q * K + K * K);
                  a0 = (1 + 1/Q * K + K * K) * norm;
                  a1 = 2 * (K * K - 1) * norm;
                  a2 = (1 - 1/Q * K + K * K) * norm;
                  b1 = a1;
                  b2 = (1 - V/Q * K + K * K) * norm;
              }
              break;
          case bq_type_lowshelf:
              if (peakGain >= 0) {    // boost
                  norm = 1 / (1 + Math.sqrt(2) * K + K * K);
                  a0 = (1 + Math.sqrt(2*V) * K + V * K * K) * norm;
                  a1 = 2 * (V * K * K - 1) * norm;
                  a2 = (1 - Math.sqrt(2*V) * K + V * K * K) * norm;
                  b1 = 2 * (K * K - 1) * norm;
                  b2 = (1 - Math.sqrt(2) * K + K * K) * norm;
              }
              else {    // cut
                  norm = 1 / (1 + Math.sqrt(2*V) * K + V * K * K);
                  a0 = (1 + Math.sqrt(2) * K + K * K) * norm;
                  a1 = 2 * (K * K - 1) * norm;
                  a2 = (1 - Math.sqrt(2) * K + K * K) * norm;
                  b1 = 2 * (V * K * K - 1) * norm;
                  b2 = (1 - Math.sqrt(2*V) * K + V * K * K) * norm;
              }
              break;
          case bq_type_highshelf:
              if (peakGain >= 0) {    // boost
                  norm = 1 / (1 + Math.sqrt(2) * K + K * K);
                  a0 = (V + Math.sqrt(2*V) * K + K * K) * norm;
                  a1 = 2 * (K * K - V) * norm;
                  a2 = (V - Math.sqrt(2*V) * K + K * K) * norm;
                  b1 = 2 * (K * K - 1) * norm;
                  b2 = (1 - Math.sqrt(2) * K + K * K) * norm;
              }
              else {    // cut
                  norm = 1 / (V + Math.sqrt(2*V) * K + K * K);
                  a0 = (1 + Math.sqrt(2) * K + K * K) * norm;
                  a1 = 2 * (K * K - 1) * norm;
                  a2 = (1 - Math.sqrt(2) * K + K * K) * norm;
                  b1 = 2 * (K * K - V) * norm;
                  b2 = (V - Math.sqrt(2*V) * K + K * K) * norm;
              }
              break;
      }
      
      return;
  }

  public double process(double in) {
    out = in * a0 + z1;
    z1 = in * a1 + z2 - b1 * out;
    z2 = in * a2 - b2 * out;
    // System.out.println(String.format("a0: %.6f a1: %.6f b1: %.6f b2: %.6f z1: %.6f z2: %.6f %.6f Q: %.6f\n",a0,a1,b1,b2,z1,z2,in,Q));
    // System.out.println(String.format("a0: %.6f z1: %.6f\n",a0,z1));

    return out;
  }
}
