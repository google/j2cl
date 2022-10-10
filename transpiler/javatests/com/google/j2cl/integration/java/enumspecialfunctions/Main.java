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
package enumspecialfunctions;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;
import static com.google.j2cl.integration.testing.Asserts.fail;

/**
 * This class tests the special functions of enum: .values() and .valueOf()
 */
public class Main {
  public static void main(String[] args) {
    testValues();
    testValueOf();
    testValueOf_exceptions();
  }

  private static void testValues() {
    assertTrue(Planet.values().length == 8);
    assertTrue(arrayContains(Planet.MERCURY, Planet.values()));
    assertTrue(arrayContains(Planet.VENUS, Planet.values()));
    assertTrue(arrayContains(Planet.EARTH, Planet.values()));
    assertTrue(arrayContains(Planet.MARS, Planet.values()));
    assertTrue(arrayContains(Planet.JUPITER, Planet.values()));
    assertTrue(arrayContains(Planet.SATURN, Planet.values()));
    assertTrue(arrayContains(Planet.URANUS, Planet.values()));
    assertTrue(arrayContains(Planet.NEPTUNE, Planet.values()));
  }

  private static boolean arrayContains(Object obj, Object[] array) {
    for (Object element : array) {
      if (element == obj) {
        return true;
      }
    }
    return false;
  }

  private static void testValueOf() {
    // TODO(b/36863439): Uncomment the following line once Enum.valueOf is implemented.
    // assertTrue(Enum.valueOf(Planet.class, "SATURN")
    assertTrue(Planet.valueOf("MERCURY") == Planet.MERCURY);
    assertTrue(Planet.valueOf("VENUS") == Planet.VENUS);
    assertTrue(Planet.valueOf("EARTH") == Planet.EARTH);
    assertTrue(Planet.valueOf("MARS") == Planet.MARS);
    assertTrue(Planet.valueOf("JUPITER") == Planet.JUPITER);
    assertTrue(Planet.valueOf("SATURN") == Planet.SATURN);
    assertTrue(Planet.valueOf("URANUS") == Planet.URANUS);
    assertTrue(Planet.valueOf("NEPTUNE") == Planet.NEPTUNE);
  }

  private static void testValueOf_exceptions() {
    try {
      Object unused = Planet.valueOf("NOTHING");
      fail("Should have thrown IllegalArgumentException.");
    } catch (IllegalArgumentException e) {
      // do nothing.
    }

    try {
      Object unused = Planet.valueOf(null);
      fail("Should have thrown NullPointerException.");
    } catch (NullPointerException e) {
      // do nothing.
    }

    try {
      Object unused = Planet.valueOf("toString");
      fail("Should have thrown IllegalArgumentException.");
    } catch (IllegalArgumentException e) {
      // do nothing.
    }

    try {
      Object unused = Planet.valueOf("__proto__");
      fail("Should have thrown IllegalArgumentException.");
    } catch (IllegalArgumentException e) {
      // do nothing.
    }

    // TODO(b/36863439): Transform these into meaningful assertions once Enum.valueOf is
    // implemented.
    try {
      Object unused = Enum.valueOf(Planet.class, null);
      // fail( "Should have thrown NullPointerException");
      // } catch (NullPointerException expected) {
    } catch (UnsupportedOperationException expected) {
      // Thrown by J2CL
    } catch (NullPointerException expected) {
      // Thrown by J2KT
    }

    // TODO(b/36863439): Transform these into meaningful assertions once Enum.valueOf is
    // implemented.
    try {
      Object unused = Enum.valueOf(Planet.class, "NOTHING");
      fail("Should have thrown IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
      // Thrown by Java
    } catch (UnsupportedOperationException expected) {
      // TODO(b/36863439): Remove this catch statement once Enum.valueOf is implemented.
      // Thrown by J2CL and J2KT
    }

    // TODO(b/30745420): Transform these into meaningful assertions once Class.getEnumConstants is
    // implemented.
    try {
      Object unused = Planet.class.getEnumConstants();
    } catch (UnsupportedOperationException expected) {
    }
  }
}
