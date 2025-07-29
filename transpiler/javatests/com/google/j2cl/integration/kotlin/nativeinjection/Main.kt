/*
 * Copyright 2023 Google Inc.
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
package nativeinjection

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.assertTrue
import jsinterop.annotations.JsMethod

fun main(vararg unused: String) {
  assertTrue(NativeClass.nativeStaticMethod() == "nativeStaticMethod")
  val instance = NativeClass()
  assertTrue(instance.nativeInstanceMethod() == "nativeInstanceMethod")

  assertTrue(NativeClassSuper.nativeStaticMethod() == "nativeStaticMethodInSuper")
  assertTrue(NativeClassSuperJ2CL.nativeStaticMethod() == "nativeStaticMethodInSuperJ2CL")

  assertEquals(1, QualifiedNativeMatch.getValue())
  assertEquals(1, QualifiedNativeMatch.InnerClass.getOtherValue())
}

/** Integration tests for native methods. */
class Main {

  /** Tests for not requiring .native.js for native JsMethod. */
  @JsMethod external fun test()
}
