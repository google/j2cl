/*
 * Copyright 2022 Google Inc.
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
package enumspecialfunctions

import com.google.j2cl.integration.testing.Asserts.assertTrue
import com.google.j2cl.integration.testing.Asserts.fail
import java.util.Arrays

/** This class tests the special functions of enum: .values() and .valueOf() */
fun main(vararg unused: String) {
  testValues()
  testValueOf()
  testValueOf_exceptions()
}

fun testValues() {
  assertTrue(Planet.values().size == 8)
  assertTrue(arrayContains(Planet.MERCURY, Planet.values()))
  assertTrue(arrayContains(Planet.VENUS, Planet.values()))
  assertTrue(arrayContains(Planet.EARTH, Planet.values()))
  assertTrue(arrayContains(Planet.MARS, Planet.values()))
  assertTrue(arrayContains(Planet.JUPITER, Planet.values()))
  assertTrue(arrayContains(Planet.SATURN, Planet.values()))
  assertTrue(arrayContains(Planet.URANUS, Planet.values()))
  assertTrue(arrayContains(Planet.NEPTUNE, Planet.values()))
}

fun arrayContains(obj: Planet, array: Array<Planet>): Boolean {
  return Arrays.binarySearch(array, obj) > -1
}

fun testValueOf() {
  assertTrue(Planet.valueOf("MERCURY") == Planet.MERCURY)
  assertTrue(Planet.valueOf("VENUS") == Planet.VENUS)
  assertTrue(Planet.valueOf("EARTH") == Planet.EARTH)
  assertTrue(Planet.valueOf("MARS") == Planet.MARS)
  assertTrue(Planet.valueOf("JUPITER") == Planet.JUPITER)
  assertTrue(Planet.valueOf("SATURN") == Planet.SATURN)
  assertTrue(Planet.valueOf("URANUS") == Planet.URANUS)
  assertTrue(Planet.valueOf("NEPTUNE") == Planet.NEPTUNE)
}

fun testValueOf_exceptions() {
  try {
    Planet.valueOf("NOTHING")
    fail("Should have thrown IllegalArgumentException.")
  } catch (e: IllegalArgumentException) {
    // do nothing.
  }

  // valueOf does not accept null in kotlin
  // try {
  //   Planet.valueOf(null)
  //   fail("Should have thrown NullPointerException.")
  // } catch (e: NullPointerException) {
  //   // do nothing.
  // }

  try {
    Planet.valueOf("toString")
    fail("Should have thrown IllegalArgumentException.")
  } catch (e: IllegalArgumentException) {
    // do nothing.
  }

  try {
    Planet.valueOf("__proto__")
    fail("Should have thrown IllegalArgumentException.")
  } catch (e: IllegalArgumentException) {
    // do nothing.
  }

  try {
    java.lang.Enum.valueOf(Planet::class.java, null)
    fail("Should have thrown NullPointerException")
  } catch (expected: UnsupportedOperationException) {
    // J2CL throws UnsupportedOperationException
  } catch (expected: NullPointerException) {
    // JVM throws NullPointerException
  }

  try {
    java.lang.Enum.valueOf(Planet::class.java, "NOTHING")
    fail("Should have thrown UnsupportedOperationException")
  } catch (expected: UnsupportedOperationException) {
    // J2CL throws UnsupportedOperationException
  } catch (expected: IllegalArgumentException) {
    // JVM throws IllegalArgumentException
  }

  try {
    Planet::class.java.enumConstants
  } catch (expected: UnsupportedOperationException) {}
}
