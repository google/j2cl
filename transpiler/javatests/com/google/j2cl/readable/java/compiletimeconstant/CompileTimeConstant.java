/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package compiletimeconstant;

public class CompileTimeConstant<T> {
  public static final Object OBJ = null;

  public static final int A = 10;
  public static final int B = 20;
  public static final int C = A * B;

  public static final String D = "Tur\"tle";
  public static final String E = "Do\'ve";
  public static final String F = D + E;
  public static final String J = F + F;
  public static final String K = null;
  public static final String L = "ThisIsALongString";
  public static final String M = "ThisIsALongStringAlso";
  public static final String N = "ThisIsALongStringAlsoButLonger";

  public static final long G = 10000L;
  public static final char H = 'A';
  public static final boolean I = G > 100;

  public static final byte MIN_BYTE = -128;
  public static final short MIN_SHORT = -32768;

  public static final byte MIN_BYTE_WITH_CAST = (byte) -128;
  public static final short MIN_SHORT_WITH_CAST = (short) -32768;

  // Note that currently this only compile time constant for Wasm purposes, not per JLS.
  public static final Class<?> classLiteral = CompileTimeConstant.class;

  public final int A2 = 10;
  public final int B2 = 20;
  public final int C2 = A * B;

  public final String D2 = "Tur\"tle";
  public final String E2 = "Do\"ve";
  public final String F2 = D2 + E2;

  public final long G2 = 10000L;
  public final char H2 = 'A';
  public final boolean I2 = G2 > 100;

  {
    boolean b = I2;
    String s = G + F;
    String l = L;
    String m = M;
    String n = N;
    Class<?> c = classLiteral;
  }
}
