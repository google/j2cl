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
package jsmethodnative;

import static com.google.j2cl.integration.testing.Asserts.assertEquals;
import static com.google.j2cl.integration.testing.Asserts.assertFalse;
import static com.google.j2cl.integration.testing.Asserts.assertNull;
import static com.google.j2cl.integration.testing.Asserts.assertSame;
import static com.google.j2cl.integration.testing.Asserts.assertTrue;
import static jsinterop.annotations.JsPackage.GLOBAL;

import java.io.Serializable;
import javaemul.internal.annotations.Wasm;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsType;

public class Main {
  public static void main(String... args) {
    testNativeJsMethodOnNonNativeType();
    testStaticNativeJsMethod();
    testForwardingMethodsOnNativeClasses();
    testCallNamespaceAsFunction();
    testBoxedTypeSupertypes();
  }

  static class NonNativeTypeWithNativeMethods {
    // TODO(b/556880337): Instance native member on non-native type not supported in Wasm.
    @Wasm("nop")
    @JsMethod(name = "toString")
    public native String callToString();

    // TODO(b/556880337): Instance native member on non-native type not supported in Wasm.
    @Wasm("nop")
    @JsMethod
    public native Object valueOf();
  }

  // TODO(b/556880337): Instance native member on non-native type not supported in Wasm.
  // This fails on compilation because "nop" doesn't return a value.
  @Wasm("nop")
  private static void testNativeJsMethodOnNonNativeType() {
    NonNativeTypeWithNativeMethods obj = new NonNativeTypeWithNativeMethods();
    assertEquals(obj.toString(), obj.callToString());
    assertSame(obj, obj.valueOf());
  }

  @JsMethod(namespace = GLOBAL)
  private static native boolean isFinite(double d);

  private static void testStaticNativeJsMethod() {
    assertFalse(isFinite(Double.POSITIVE_INFINITY));
    assertFalse(isFinite(Double.NEGATIVE_INFINITY));
    assertFalse(isFinite(Double.NaN));
    assertTrue(isFinite(0));
    assertTrue(isFinite(1));
  }

  @JsType(isNative = true, namespace = GLOBAL, name = "?")
  interface NativeInterface {
    void add(String element);
  }

  @JsType(isNative = true, namespace = "jsmethodnative", name = "NativeClass")
  static class NativeSuperClass {
    public native void add(String element);

    public native boolean remove(String element);
  }

  @JsType(isNative = true, namespace = "jsmethodnative", name = "NativeClass")
  static class NativeSubClassAccidentalOverride extends NativeSuperClass
      implements NativeInterface {}

  private static void testForwardingMethodsOnNativeClasses() {
    NativeSubClassAccidentalOverride subClass = new NativeSubClassAccidentalOverride();
    subClass.add("Hi");
    assertTrue(subClass.remove("Hi"));
    assertFalse(subClass.remove("Hi"));
  }

  @JsMethod(namespace = "jsmethodnative.FunctionNamespace", name = "")
  private static native String callFunctionNamespace(String input);

  private static void testCallNamespaceAsFunction() {
    assertEquals("foobar", callFunctionNamespace("foo"));
  }

  @JsMethod(namespace = "jsmethodnative.helper", name = "passThrough")
  private static native <T> Comparable<T> passThroughComparable(Comparable<T> c);

  @JsMethod(namespace = "jsmethodnative.helper", name = "passThrough")
  private static native CharSequence passThroughCharSequence(CharSequence cs);

  @JsMethod(namespace = "jsmethodnative.helper", name = "passThrough")
  private static native Number passThroughNumber(Number n);

  @JsMethod(namespace = "jsmethodnative.helper", name = "passThrough")
  private static native Serializable passThroughSerializable(Serializable s);

  private static void testBoxedTypeSupertypes() {
    assertNull(passThroughComparable(null));
    assertTrue(passThroughComparable("hello").compareTo("hello") == 0);
    assertTrue(passThroughComparable(42.5).compareTo(42.5) == 0);
    assertTrue(passThroughComparable(true).compareTo(true) == 0);
    assertTrue(passThroughComparable(42L).compareTo(42L) == 0);
    assertTrue(passThroughComparable(42).compareTo(42) == 0);
    Comparable<Object> customComparable = o -> 0;
    assertTrue(passThroughComparable(customComparable).compareTo(customComparable) == 0);

    assertNull(passThroughCharSequence(null));
    assertTrue(passThroughCharSequence("hello").charAt(1) == 'e');
    CharSequence customCharSequence = new StringBuilder("world");
    assertTrue(passThroughCharSequence(customCharSequence).charAt(1) == 'o');

    assertNull(passThroughNumber(null));
    assertTrue(passThroughNumber(42.5).doubleValue() == 42.5);
    assertTrue(passThroughNumber(42L).longValue() == 42L);
    assertTrue(passThroughNumber(42).intValue() == 42);

    assertNull(passThroughSerializable(null));
    assertEquals("hello", passThroughSerializable("hello"));
    assertEquals(42.5, passThroughSerializable(42.5));
    assertEquals(true, passThroughSerializable(true));
    assertEquals(42L, passThroughSerializable(42L));
    Serializable customSerializable = new Serializable() {};
    assertSame(customSerializable, passThroughSerializable(customSerializable));
  }
}
