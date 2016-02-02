package com.google.j2cl.transpiler.integration.casttoprimitives;

public class Main {
  public static void main(String[] args) {
    byte b = 1;
    byte mb = 127; // Byte.MAX_VALUE
    char c = 'a';
    char mc = 65535; // Character.MAX_VALUE;
    short s = 2;
    short ms = 32767; // Short.MAX_VALUE;
    int i = 3;
    int mi = 2147483647; // Integer.MAX_VALUE;
    long l = 4L;
    long ll = 2415919103L; // max_int < ll < max_int * 2, used for testing for signs.
    long ml = 9223372036854775807L; // Long.MAX_VALUE;
    float f = 2.7f;
    float mf = 3.4028235E38f; // Float.MAX_VALUE;
    double d = 2.6;
    double dd = 2415919103.7; // dd > max_int
    double md = 1.7976931348623157E308; // Double.MAX_VALUE;

    assert ((char) b == 1);
    assert ((short) b == 1);
    assert ((int) b == 1);
    assert ((long) b == 1L);
    assert ((float) b == 1.0);
    assert ((double) b == 1.0);

    assert ((char) mb == 127);
    assert ((short) mb == 127);
    assert ((int) mb == 127);
    assert ((long) mb == 127L);
    assert ((float) mb == 127.0);
    assert ((double) mb == 127.0);

    assert ((byte) c == 97);
    assert ((short) c == 97);
    assert ((int) c == 97);
    assert ((long) c == 97L);
    assert ((float) c == 97.0);
    assert ((double) c == 97.0);

    assert ((byte) mc == -1);
    assert ((short) mc == -1);
    assert ((int) mc == 65535);
    assert ((long) mc == 65535L);
    assert ((float) mc == 65535.0);
    assert ((double) mc == 65535.0);

    assert ((byte) s == 2);
    assert ((char) s == 2);
    assert ((int) s == 2);
    assert ((long) s == 2L);
    assert ((float) s == 2.0);
    assert ((double) s == 2.0);

    assert ((byte) ms == -1);
    assert ((char) ms == 32767);
    assert ((int) ms == 32767);
    assert ((long) ms == 32767L);
    assert ((float) ms == 32767.0);
    assert ((double) ms == 32767.0);

    assert ((byte) i == 3);
    assert ((char) i == 3);
    assert ((short) i == 3);
    assert ((long) i == 3L);
    assert ((float) i == 3.0);
    assert ((double) i == 3.0);

    assert ((byte) mi == -1);
    assert ((char) mi == 65535);
    assert ((short) mi == -1);
    assert ((long) mi == 2147483647L);
    // assert ((float) mi == 2.14748365E9f); // it is casted to double-precision 2.147483647E9
    assert ((double) mi == 2.147483647E9);

    assert ((byte) l == 4);
    assert ((char) l == 4);
    assert ((short) l == 4);
    assert ((int) l == 4);
    assert ((float) l == 4);
    assert ((double) l == 4);

    assert ((byte) ll == -1);
    assert ((char) ll == 65535);
    assert ((short) ll == -1);
    assert ((int) ll == -1879048193);
    // assert ((float) ll == 2.4159191E9); // it is casted to double-precision 2.415919103E9.
    assert ((double) ll == 2.415919103E9);

    assert ((byte) ml == -1);
    assert ((char) ml == 65535);
    assert ((short) ml == -1);
    assert ((int) ml == -1);
    assert ((float) ml == 9.223372036854776E18);
    assert ((double) ml == 9.223372036854776E18);

    assert ((byte) f == 2);
    assert ((char) f == 2);
    assert ((short) f == 2);
    assert ((int) f == 2);
    assert ((long) f == 2L);
    assert (((double) f - 2.7) < 1e-7);

    assert ((byte) mf == -1);
    assert ((char) mf == 65535);
    assert ((short) mf == -1);
    assert ((int) mf == 2147483647);
    assert ((long) mf == 9223372036854775807L);
    assert ((double) mf == 3.4028234663852886E38);

    assert ((byte) d == 2);
    assert ((char) d == 2);
    assert ((short) d == 2);
    assert ((int) d == 2);
    assert ((long) d == 2L);
    // assert ((float) d == 2.6f); // float is emitted as 2.5999999046325684

    assert ((byte) dd == -1);
    assert ((char) dd == 65535);
    assert ((short) dd == -1);
    assert ((int) dd == 2147483647);
    assert ((long) dd == 2415919103L);
    // assert ((float) dd == 2.4159191E9); // it is casted to double-precision 2415919103.7

    assert ((byte) md == -1);
    assert ((char) md == 65535);
    assert ((short) md == -1);
    assert ((int) md == 2147483647);
    assert ((long) md == 9223372036854775807L);
    
    testReferenceToPrimitive();
    testBoxedToPrimitive();
  }

  @SuppressWarnings("unused")
  public static void testReferenceToPrimitive() {
    Object o = new Object();
    try {
      boolean bool = (boolean) o;
      assert false : "An expected failure did not occur.";
    } catch (ClassCastException e) {
      // expected.
    }
    try {
      byte b = (byte) o;
      assert false : "An expected failure did not occur.";
    } catch (ClassCastException e) {
      // expected.
    }
    try {
      char c = (char) o;
      assert false : "An expected failure did not occur.";
    } catch (ClassCastException e) {
      // expected.
    }
    try {
      short s = (short) o;
      assert false : "An expected failure did not occur.";
    } catch (ClassCastException e) {
      // expected.
    }
    try {
      int i = (int) o;
      assert false : "An expected failure did not occur.";
    } catch (ClassCastException e) {
      // expected.
    }
    try {
      long l = (long) o;
      assert false : "An expected failure did not occur.";
    } catch (ClassCastException e) {
      // expected.
    }
    try {
      float f = (float) o;
      assert false : "An expected failure did not occur.";
    } catch (ClassCastException e) {
      // expected.
    }
    try {
      double d = (double) o;
      assert false : "An expected failure did not occur.";
    } catch (ClassCastException e) {
      // expected.
    }

    o = Boolean.FALSE;
    boolean bool = (boolean) o;
    assert !bool;

    o = Byte.MAX_VALUE;
    byte b = (byte) o;
    assert b == 127;

    o = new Character('a');
    char c = (char) o;
    assert c == 'a';

    o = Short.MAX_VALUE;
    short s = (short) o;
    assert s == 32767;

    o = new Integer(1);
    int i = (int) o;
    assert i == 1;

    o = new Long(1L);
    long l = (long) o;
    assert l == 1L;

    o = new Float(1.1f);
    float f = (float) o;
    assert f == 1.1f;

    o = new Double(1.2);
    double d = (double) o;
    assert d == 1.2;
  }

  @SuppressWarnings("unused")
  public static void testBoxedToPrimitive() {
    Byte b = new Byte((byte) 1);
    Character c = new Character('a');
    Short s = new Short((short) 1);
    Integer i = new Integer(1);
    Long l = new Long(1L);
    Float f = new Float(1.1f);
    Double d = new Double(1.1);

    short ss = b;
    assert ss == 1;

    int ii = b;
    assert ii == 1;
    ii = c;
    assert ii == 97;
    ii = s;
    assert ii == 1;

    long ll = b;
    assert ll == 1L;
    ll = s;
    assert ll == 1L;
    ll = c;
    assert ll == 97L;
    ll = i;
    assert ll == 1L;

    float ff = b;
    assert ff == 1;
    ff = s;
    assert ff == 1;
    ff = c;
    assert ff == 97;
    ff = i;
    assert ff == 1;
    ff = l;
    assert ff == 1;

    double dd = b;
    assert dd == 1;
    dd = s;
    assert dd == 1;
    dd = c;
    assert dd == 97;
    dd = i;
    assert dd == 1;
    dd = l;
    assert dd == 1;
    dd = f;
    assert dd - 1.1 < 1e-7;
  }
}
