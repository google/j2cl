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
  testNativeMethods()
  testNativeProperties()
  testNativeCodeRunsOnce()
  testMatchInJavaPackage()
  testMatchInSuperSourceRoot()
  testMatchByQualifiedName()
  testNativeCodeCallingMembers()
  testLongAlias()
}

private fun testNativeMethods() {
  assertTrue(NativeClass.nativeStaticMethod(2, 3) == "nativeStaticMethod5")

  val instance = NativeClass()
  assertTrue(instance.nativeInstanceMethod(1, 2) == "nativeInstanceMethod3")
}

private fun testNativeProperties() {
  assertEquals(42, NativeClass.getNativeStaticProperty())
  assertEquals("nativeInstanceProperty", NativeClass().getNativeInstanceProperty())
}

private fun testNativeCodeRunsOnce() {
  assertEquals(1, NativeClass.getNativeInitializationCount())
}

private fun testMatchInJavaPackage() {
  assertTrue(NativeClassSuper.nativeStaticMethod() == "nativeStaticMethodInSuper")
}

private fun testMatchInSuperSourceRoot() {
  assertTrue(NativeClassSuperJ2CL.nativeStaticMethod() == "nativeStaticMethodInSuperJ2CL")
}

private fun testMatchByQualifiedName() {
  assertEquals(1, QualifiedNativeMatch.getValue())
  assertEquals(1, QualifiedNativeMatch.InnerClass.getOtherValue())
}

private fun testNativeCodeCallingMembers() {
  assertEquals(11, NativeClass().callsInstanceMember())
  assertEquals(30, NativeClass.callsStaticMember(15))
  assertEquals(10, NativeClass.callsConstructor())
}

private fun testLongAlias() {
  assertEquals("nativeStaticMethodUsingLongAlias", NativeClass.nativeStaticMethodUsingLongAlias())
}

/** Integration tests for native methods. */
class Main {

  /** Tests for not requiring .native.js for native JsMethod. */
  @JsMethod external fun test()
}
