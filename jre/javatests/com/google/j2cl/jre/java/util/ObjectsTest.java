/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.j2cl.jre.java.util;

import static com.google.j2cl.jre.testing.TestUtils.isWasm;
import static org.junit.Assert.assertThrows;

import com.google.j2cl.jre.testing.J2ktIncompatible;
import java.util.Comparator;
import java.util.Objects;
import junit.framework.TestCase;

/** Tests {@link Objects}. */
public class ObjectsTest extends TestCase {

  public void testCompare() {
    Comparator<Integer> intComparator =
        new Comparator<Integer>() {
          @SuppressWarnings({"BoxedPrimitiveEquality", "NumberEquality"})
          @Override
          public int compare(Integer a, Integer b) {
            if (a == b) {
              fail("comparator must not be called if a == b");
            }

            if (a == null) {
              return -1;
            }
            if (b == null) {
              return 1;
            }
            return a.compareTo(b);
          }
        };

    assertEquals(0, Objects.compare(null, null, intComparator));
    assertEquals(-1, Objects.compare(null, new Integer(12345), intComparator));
    assertEquals(1, Objects.compare(new Integer(12345), null, intComparator));
    assertEquals(-1, Objects.compare(new Integer(12345), new Integer(12346), intComparator));
    assertEquals(1, Objects.compare(new Integer("12345"), new Integer(12344), intComparator));
    assertEquals(0, Objects.compare(new Integer("12345"), new Integer(12345), intComparator));
  }

  public void testDeepEquals() {
    assertTrue(Objects.deepEquals(null, null));
    assertFalse(Objects.deepEquals(null, "not null"));
    assertFalse(Objects.deepEquals("not null", null));
    assertTrue(Objects.deepEquals(new Integer("1234"), new Integer(1234)));
    assertFalse(Objects.deepEquals(new Object(), new Object()));

    Object obj = new Object();
    assertTrue(Objects.deepEquals(obj, obj));

    assertFalse(Objects.deepEquals(new int[] {1}, new double[] {1}));
    assertFalse(Objects.deepEquals(new int[0], new double[0]));
    assertTrue(Objects.deepEquals((Object) new Object[] {"a"}, (Object) new String[] {"a"}));
    assertTrue(Objects.deepEquals((Object) new String[] {"a"}, (Object) new Object[] {"a"}));

    int[] intArray1 = new int[] {2, 3, 5};
    int[] intArray2 = new int[] {3, 1};
    int[] intArray3 = new int[] {2, 3, 5};
    assertFalse(Objects.deepEquals(intArray1, intArray2));
    assertFalse(Objects.deepEquals(intArray2, intArray3));
    assertTrue(Objects.deepEquals(intArray1, intArray1));
    assertTrue(Objects.deepEquals(intArray1, intArray3));

    assertTrue(Objects.deepEquals(new int[][] {new int[] {1}}, new int[][] {new int[] {1}}));
    assertFalse(Objects.deepEquals(new int[][] {new int[] {1}}, new double[][] {new double[] {1}}));
  }

  public void testEquals() {
    assertTrue(Objects.equals(null, null));
    assertFalse(Objects.equals(null, "not null"));
    assertFalse(Objects.equals("not null", null));

    assertTrue(Objects.equals("a", "a"));
    assertFalse(Objects.equals("a", "b"));
    assertTrue(Objects.equals(new String("a"), new String("a")));

    assertTrue(Objects.equals(true, true));
    assertFalse(Objects.equals(true, false));
    assertTrue(Objects.equals(new Boolean(true), new Boolean(true)));

    assertTrue(Objects.equals(1d, 1d));
    assertFalse(Objects.equals(1d, 2d));
    assertFalse(Objects.equals(new Double(1d), new Double(2d)));

    Object obj = new Object();
    assertTrue(Objects.equals(obj, obj));
    assertFalse(Objects.equals(new Object(), new Object()));
  }

  public void testHashCode() {
    assertEquals(0, Objects.hashCode(null));
    Object obj = new Object();
    assertEquals(obj.hashCode(), Objects.hashCode(obj));
  }

  @SuppressWarnings("DangerousLiteralNull") // Intentionally misusing Optional to test bug parity.
  @J2ktIncompatible // Not emulated
  public void testRequireNonNull() {
    Integer one = 1;
    Integer anotherOne = 1;
    assertEquals(one, Objects.requireNonNull(anotherOne, "message"));
    assertEquals(one, Objects.requireNonNullElse(1, 2));
    assertEquals(one, Objects.requireNonNullElseGet(1, () -> 2));
    assertEquals(one, Objects.requireNonNullElse(null, 1));
    assertEquals(one, Objects.requireNonNullElseGet(null, () -> 1));

    Exception e =
        assertThrows(
            NullPointerException.class, () -> Objects.requireNonNull(null, "expected message"));
    assertTrue(e.getMessage().contains("expected message"));

    assertThrows(NullPointerException.class, () -> Objects.requireNonNullElse(null, null));
    assertThrows(NullPointerException.class, () -> Objects.requireNonNullElseGet(null, () -> null));
  }

  @J2ktIncompatible // Not emulated
  public void testRequireNonNull_nullSupplier() {
    if (isWasm()) {
      // TODO(b/183769034): Re-enable when NPE on dereference is supported
      return;
    }
    assertThrows(NullPointerException.class, () -> Objects.requireNonNullElseGet(null, null));
  }
}
