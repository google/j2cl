package com.google.j2cl.transpiler.readable.compiletimeconstant;

public class CompileTimeConstant {
  public static final int A = 10;
  public static final int B = 20;
  public static final int C = A * B;

  public static final String D = "Tur\"tle";
  public static final String E = "Do\"ve";
  public static final String F = D + E;

  public static final long G = 10000L;
  public static final char H = 'A';
  public static final boolean I = G > 100;

  public final int A2 = 10;
  public final int B2 = 20;
  public final int C2 = A * B;

  public final String D2 = "Tur\"tle";
  public final String E2 = "Do\"ve";
  public final String F2 = D2 + E2;

  public final long G2 = 10000L;
  public final char H2 = 'A';
  public final boolean I2 = G2 > 100;
}
