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
package wideningfloatconversion

import com.google.j2cl.integration.testing.Asserts.assertTrue
import com.google.j2cl.integration.testing.TestUtils

fun main(vararg unused: String) {
  // Note: unlike Java Kotlin does not have implicit widening conversion for floats so none of these
  // cases are actually unique. They only serve as a comparison between Java and Kotlin.
  testAssignment()
  testCast()
}

private fun testCast() {
  val mi: Int = 2147483647 // Integer.MAX_VALUE;
  val ll: Long = 2415919103L // max_int < ll < max_int * 2, used for testing for signs.

  if (TestUtils.isJavaScript()) {
    // we don't honor float-double precision differences in JS
    assertTrue(mi.toFloat().toDouble() == 2.147483647E9)
    assertTrue(ll.toFloat().toDouble() == 2.415919103E9)
  } else {
    // JVM and Wasm
    assertTrue(mi.toFloat() == 2.14748365E9f)
    assertTrue(ll.toFloat() == 2.4159191E9f)
  }
}

private fun testAssignment() {
  val mi: Int = 2147483647 // Integer.MAX_VALUE;
  val ll: Long = 2415919103L // max_int < ll < max_int * 2, used for testing for signs.
  val ml: Long = 9223372036854775807L // Long.MAX_VALUE;

  var rf: Float

  if (TestUtils.isJavaScript()) {
    // we don't honor float-double precision differences in JS
    rf = mi.toFloat()
    assertTrue(rf.toDouble() == 2.147483647E9)
    rf = ll.toFloat()
    assertTrue(rf.toDouble() == 2.415919103E9)
  } else {
    // JVM and Wasm
    rf = mi.toFloat()
    assertTrue(rf == 2.14748365E9f)
    rf = ll.toFloat()
    assertTrue(rf == 2.4159191E9f)
    rf = ml.toFloat()
    assertTrue(rf == 9.223372E18f)
  }

  rf = ml.toFloat()
  assertTrue(rf.toDouble() == 9.223372036854776E18)
}
