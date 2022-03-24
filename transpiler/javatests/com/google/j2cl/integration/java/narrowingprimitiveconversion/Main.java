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
package narrowingprimitiveconversion;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;

public class Main {
  public static void main(String[] args) {
    testCoercions();
    testStaticCoercions();
  }

  private static void testCoercions() {
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

    assertTrue(((char) b == 1));

    assertTrue(((char) mb == 127));

    assertTrue(((byte) c == 97));
    assertTrue(((short) c == 97));

    assertTrue(((byte) mc == -1));
    assertTrue(((short) mc == -1));

    assertTrue(((byte) s == 2));
    assertTrue(((char) s == 2));

    assertTrue(((byte) ms == -1));
    assertTrue(((char) ms == 32767));

    assertTrue(((byte) i == 3));
    assertTrue(((char) i == 3));
    assertTrue(((short) i == 3));

    assertTrue(((byte) mi == -1));
    assertTrue(((char) mi == 65535));
    assertTrue(((short) mi == -1));

    assertTrue(((byte) l == 4));
    assertTrue(((char) l == 4));
    assertTrue(((short) l == 4));
    assertTrue(((int) l == 4));

    assertTrue(((byte) ll == -1));
    assertTrue(((char) ll == 65535));
    assertTrue(((short) ll == -1));
    assertTrue(((int) ll == -1879048193));

    assertTrue(((byte) ml == -1));
    assertTrue(((char) ml == 65535));
    assertTrue(((short) ml == -1));
    assertTrue(((int) ml == -1));

    assertTrue(((byte) f == 2));
    assertTrue(((char) f == 2));
    assertTrue(((short) f == 2));
    assertTrue(((int) f == 2));
    assertTrue(((long) f == 2L));

    assertTrue(((byte) mf == -1));
    assertTrue(((char) mf == 65535));
    assertTrue(((short) mf == -1));
    assertTrue(((int) mf == 2147483647));
    assertTrue(((long) mf == 9223372036854775807L));

    assertTrue(((byte) d == 2));
    assertTrue(((char) d == 2));
    assertTrue(((short) d == 2));
    assertTrue(((int) d == 2));
    assertTrue(((long) d == 2L));

    assertTrue(((byte) dd == -1));
    assertTrue(((char) dd == 65535));
    assertTrue(((short) dd == -1));
    assertTrue(((int) dd == 2147483647));
    assertTrue(((long) dd == 2415919103L));

    assertTrue(((byte) md == -1));
    assertTrue(((char) md == 65535));
    assertTrue(((short) md == -1));
    assertTrue(((int) md == 2147483647));
    assertTrue(((long) md == 9223372036854775807L));

    int n = 5;
    assertTrue(2 * (n / 2) == 4);

    // JLS 5.1.4
    float fmin = Float.NEGATIVE_INFINITY;
    float fmax = Float.POSITIVE_INFINITY;
    float fnan = Float.NaN;

    assertTrue((long) fmin == Long.MIN_VALUE);
    assertTrue((long) fmax == Long.MAX_VALUE);
    assertTrue((long) fnan == 0L);

    assertTrue((int) fmin == Integer.MIN_VALUE);
    assertTrue((int) fmax == Integer.MAX_VALUE);
    assertTrue((int) fnan == 0);

    assertTrue((short) fmin == (short) Integer.MIN_VALUE);
    assertTrue((short) fmax == (short) Integer.MAX_VALUE);
    assertTrue((short) fnan == (short) 0);

    assertTrue((byte) fmin == (byte) Integer.MIN_VALUE);
    assertTrue((byte) fmax == (byte) Integer.MAX_VALUE);
    assertTrue((byte) fnan == (byte) 0);

    assertTrue((char) fmin == (char) Integer.MIN_VALUE);
    assertTrue((char) fmax == (char) Integer.MAX_VALUE);
    assertTrue((char) fnan == (char) 0);

    byte three = 0b11;
    assertTrue(-128 == (byte) (three << 7));
  }

  private static void testStaticCoercions() {
    long max = 9223372036854775807L;

    byte b = (byte) 9223372036854775807L; // -1
    assertTrue(b == (byte) max);

    char c = (char) 9223372036854775807L; // 65535
    assertTrue(c == (char) max);

    short s = (short) 9223372036854775807L; // -1
    assertTrue(s == (short) max);

    int i = (int) 9223372036854775807L; // -1
    assertTrue(i == (int) max);

    float f = (float) 9223372036854775807L; //  9.223372036854776E18
    assertTrue(f == (float) max);

    double d = (double) 9223372036854775807L; //  9.223372036854776E18
    assertTrue(d == (double) max);
  }
}
