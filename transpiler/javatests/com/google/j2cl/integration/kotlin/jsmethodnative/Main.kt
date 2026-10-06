/*
 * Copyright 2015 Google Inc.
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
package jsmethodnative

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.assertFalse
import com.google.j2cl.integration.testing.Asserts.assertSame
import com.google.j2cl.integration.testing.Asserts.assertTrue
import jsinterop.annotations.JsMethod
import jsinterop.annotations.JsPackage.GLOBAL

fun main(vararg args: String) {
  testNativeJsMethodOnNonNativeType()
  testStaticNativeJsMethod()
}

private class NonNativeTypeWithNativeMethods {
  @JsMethod(name = "toString") external fun callToString(): String

  @JsMethod external fun valueOf(): Any
}

private fun testNativeJsMethodOnNonNativeType() {
  val obj = NonNativeTypeWithNativeMethods()
  assertEquals(obj.toString(), obj.callToString())
  assertSame(obj, obj.valueOf())
}

@JsMethod(namespace = GLOBAL) private external fun isFinite(d: Double): Boolean

private fun testStaticNativeJsMethod() {
  assertFalse(isFinite(Double.POSITIVE_INFINITY))
  assertFalse(isFinite(Double.NEGATIVE_INFINITY))
  assertFalse(isFinite(Double.NaN))
  assertTrue(isFinite(0.0))
  assertTrue(isFinite(1.0))
}
