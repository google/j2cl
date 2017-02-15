package com.google.j2cl.transpiler.integration.narrowingprimitiveconversion;

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

    assert ((char) mb == 127);

    assert ((byte) c == 97);
    assert ((short) c == 97);

    assert ((byte) mc == -1);
    assert ((short) mc == -1);

    assert ((byte) s == 2);
    assert ((char) s == 2);

    assert ((byte) ms == -1);
    assert ((char) ms == 32767);

    assert ((byte) i == 3);
    assert ((char) i == 3);
    assert ((short) i == 3);

    assert ((byte) mi == -1);
    assert ((char) mi == 65535);
    assert ((short) mi == -1);

    assert ((byte) l == 4);
    assert ((char) l == 4);
    assert ((short) l == 4);
    assert ((int) l == 4);

    assert ((byte) ll == -1);
    assert ((char) ll == 65535);
    assert ((short) ll == -1);
    assert ((int) ll == -1879048193);

    assert ((byte) ml == -1);
    assert ((char) ml == 65535);
    assert ((short) ml == -1);
    assert ((int) ml == -1);

    assert ((byte) f == 2);
    assert ((char) f == 2);
    assert ((short) f == 2);
    assert ((int) f == 2);
    assert ((long) f == 2L);

    assert ((byte) mf == -1);
    assert ((char) mf == 65535);
    assert ((short) mf == -1);
    assert ((int) mf == 2147483647);
    assert ((long) mf == 9223372036854775807L);

    assert ((byte) d == 2);
    assert ((char) d == 2);
    assert ((short) d == 2);
    assert ((int) d == 2);
    assert ((long) d == 2L);
    assert ((float) d == d); // we don't honor float-double precision differences

    assert ((byte) dd == -1);
    assert ((char) dd == 65535);
    assert ((short) dd == -1);
    assert ((int) dd == 2147483647);
    assert ((long) dd == 2415919103L);
    assert ((float) dd == dd); // we don't honor float-double precision differences

    assert ((byte) md == -1);
    assert ((char) md == 65535);
    assert ((short) md == -1);
    assert ((int) md == 2147483647);
    assert ((long) md == 9223372036854775807L);
    assert ((float) md == md); // we don't honor float-double precision differences

    int n = 5;
    assert 2 * (n / 2) == 4;

    // JLS 5.1.4
    float fmin = Float.NEGATIVE_INFINITY;
    float fmax = Float.POSITIVE_INFINITY;
    float fnan = Float.NaN;

    assert (long) fmin == Long.MIN_VALUE;
    assert (long) fmax == Long.MAX_VALUE;
    assert (long) fnan == 0L;

    assert (int) fmin == Integer.MIN_VALUE;
    assert (int) fmax == Integer.MAX_VALUE;
    assert (int) fnan == 0;

    assert (short) fmin == (short) Integer.MIN_VALUE;
    assert (short) fmax == (short) Integer.MAX_VALUE;
    assert (short) fnan == (short) 0;

    assert (byte) fmin == (byte) Integer.MIN_VALUE;
    assert (byte) fmax == (byte) Integer.MAX_VALUE;
    assert (byte) fnan == (byte) 0;

    assert (char) fmin == (char) Integer.MIN_VALUE;
    assert (char) fmax == (char) Integer.MAX_VALUE;
    assert (char) fnan == (char) 0;
  }
}
