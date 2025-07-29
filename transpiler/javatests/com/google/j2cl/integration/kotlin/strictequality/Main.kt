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
package strictequality

import com.google.j2cl.integration.testing.Asserts.assertTrue
import com.google.j2cl.integration.testing.TestUtils.isJavaScript

fun main(vararg unused: String) {
  testEqualityIsStrict()
  testEqualityIsStrict_regression()
  testBoxingValueOf()
  testStringLiteralIdentityAcrossCompilationBoundaries()
}

/** Some of these tests are JavaScript specific and are disabled on the JVM and Wasm platform. */
private fun testBoxingValueOf() {
  assertTrue(1.toChar() as Char? === 1.toChar() as Char?)

  assertTrue((1.toByte() as Byte?) === (1.toByte() as Byte?))

  assertTrue(1 as Int? === 1 as Int?)

  assertTrue(-1000 as Int? !== -1000 as Int?)

  // Values -128 to 127 inclusive are guaranteed to be cached by JLS 5.1.7.
  assertTrue(-128 as Int? === -128 as Int?)

  assertTrue(127 as Int? === 127 as Int?)

  assertTrue(0 as Int? === 0 as Int?)

  // assertTrue(new String("asdf") != new String("asdf")); // can't honor, it's native JS string
  assertTrue("asdf".toString() === "asdf".toString())

  // assertTrue(new Boolean(true) != new Boolean(true)); // can't honor, it's native JS boolean
  assertTrue(true as Boolean? === true as Boolean?)

  if (isJavaScript()) {
    // assertTrue(new Double(1) != new Double(1)); // can't honor, it's native JS double
    assertTrue(1.0 as Double? === 1.0 as Double?) // different from Kotlin, it's native JS double
  }
}

private fun testEqualityIsStrict() {
  val nullObject: Any? = null
  val emptyString: Any = ""
  val boxedBooleanFalse: Any = false
  val boxedDoubleZero: Any = 0.0
  val emptyArray: Any = arrayOf<Any>()
  val undefined = arrayOfNulls<Any?>(1)[0]

  assertTrue(undefined === null)
  assertTrue(undefined === nullObject)
  assertTrue(undefined !== boxedBooleanFalse)
  assertTrue(undefined !== emptyString)
  assertTrue(undefined !== boxedDoubleZero)
  assertTrue(undefined !== emptyArray)

  assertTrue(null === nullObject)
  assertTrue(null !== boxedBooleanFalse)
  assertTrue(null !== emptyString)
  assertTrue(null !== boxedDoubleZero)
  assertTrue(null !== emptyArray)

  assertTrue(boxedBooleanFalse !== nullObject)
  assertTrue(boxedBooleanFalse !== emptyString)
  assertTrue(boxedBooleanFalse !== boxedDoubleZero)
  assertTrue(boxedBooleanFalse !== emptyArray)

  assertTrue(boxedDoubleZero !== nullObject)
  assertTrue(boxedDoubleZero !== emptyString)
  assertTrue(boxedDoubleZero !== emptyArray)

  assertTrue(emptyArray !== nullObject)
  assertTrue(emptyArray !== emptyString)

  assertTrue(emptyString !== nullObject)
}

// Make sure String does not end up compared via '==' (b/33850935).
private fun testEqualityIsStrict_regression() {
  // java.lang.Object.equals should not optimize to '=='
  assertTrue(!StringBuilder("data").equals("data"))

  // CharSequence comparision should not optimize to '=='
  assertTrue(charSeq1() !== charSeq2())
}

private fun charSeq1(): CharSequence? {
  return if (Math.random() > 0) "data" else null
}

private fun charSeq2(): CharSequence? {
  return if (Math.random() > 0) StringBuilder("data") else null
}

private fun testStringLiteralIdentityAcrossCompilationBoundaries() {
  assertTrue("ONE" == STRING_ONE)
}
