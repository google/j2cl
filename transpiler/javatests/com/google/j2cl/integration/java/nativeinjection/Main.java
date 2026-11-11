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
package nativeinjection;

import static com.google.j2cl.integration.testing.Asserts.assertEquals;
import static com.google.j2cl.integration.testing.Asserts.assertTrue;

import jsinterop.annotations.JsMethod;

/**
 * Integration tests for native methods.
 */
public class Main {
  public static void main(String... args) {
    testNativeMethods();
    testNativeProperties();
    testNativeCodeRunsOnce();
    testMatchInJavaPackage();
    testMatchInSuperSourceRoot();
    testMatchByQualifiedName();
    testNativeCodeCallingMembers();
    testNativeMemberImplementedByExposedMember();
    testLongAlias();
  }

  private static void testNativeMethods() {
    assertTrue(NativeClass.nativeStaticMethod(2, 3).equals("nativeStaticMethod5"));

    NativeClass instance = new NativeClass();
    assertTrue(instance.nativeInstanceMethod(1, 2).equals("nativeInstanceMethod3"));
  }

  private static void testNativeProperties() {
    assertEquals(42, NativeClass.getNativeStaticProperty());
    assertEquals("nativeInstanceProperty", new NativeClass().getNativeInstanceProperty());
  }

  private static void testNativeCodeRunsOnce() {
    assertEquals(1, NativeClass.getNativeInitializationCount());
  }

  private static void testMatchInJavaPackage() {
    assertTrue(NativeClassSuper.nativeStaticMethod().equals("nativeStaticMethodInSuper"));
  }

  private static void testMatchInSuperSourceRoot() {
    assertTrue(NativeClassSuperJ2CL.nativeStaticMethod().equals("nativeStaticMethodInSuperJ2CL"));
  }

  private static void testMatchByQualifiedName() {
    assertEquals(1, QualifiedNativeMatch.getValue());
    assertEquals(1, QualifiedNativeMatch.InnerClass.getOtherValue());
  }

  private static void testNativeCodeCallingMembers() {
    assertEquals(11, new NativeClass().callsInstanceMember());
    assertEquals(30, NativeClass.callsStaticMember(15));
    assertEquals(10, NativeClass.callsConstructor());
  }

  private static void testNativeMemberImplementedByExposedMember() {
    Object value = "value";
    assertEquals("implementedByExposedMember:value", NativeClass.implementedByExposedMember(value));
  }

  private static void testLongAlias() {
    assertEquals(
        "nativeStaticMethodUsingLongAlias", NativeClass.nativeStaticMethodUsingLongAlias());
  }

  /**
   * Tests for not requiring .native.js for native JsMethod.
   */
  @JsMethod
  public native void test();
}
