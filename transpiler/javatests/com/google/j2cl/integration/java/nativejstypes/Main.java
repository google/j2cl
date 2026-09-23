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
package nativejstypes;

import static com.google.j2cl.integration.testing.Asserts.assertEquals;
import static com.google.j2cl.integration.testing.Asserts.assertNotNull;
import static com.google.j2cl.integration.testing.Asserts.assertThrowsClassCastException;
import static com.google.j2cl.integration.testing.Asserts.assertTrue;

import javaemul.internal.annotations.Wasm;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

public class Main {
  public static void testNativeJsTypeWithNamespace() {
    Foo foo = new Foo();
    assertTrue(foo.sum() == 42);
    foo.x = 50;
    foo.y = 5;
    assertTrue(foo.sum() == 55);
  }

  public static void testNativeJsTypeWithoutNamespace() {
    Bar bar = new Bar(6, 7);
    assertTrue(bar.product() == 42);
    bar.x = 50;
    bar.y = 5;
    assertTrue(bar.product() == 250);
    Bar.f = 10;
    assertTrue(Bar.f == 10);
  }

  public static void testGlobalNativeJsType() {
    Number number22 = new Number(2.2);
    Double number10base2 = Number.parseInt("10", 2);
    assertTrue(number22.toFixed().equals(number10base2.toString()));
  }

  public static void testNativeEquality() {
    Number n1 = new Number(1.0);
    Number n2 = new Number(1.0);
    assertTrue(n1 == n1);
    assertTrue(n1 != n2);
    assertTrue(n1 != null);

    Number n3 = getUndefined();
    assertTrue(n3 == null);
    assertTrue(n3 != n1);
  }

  @JsProperty(namespace = JsPackage.GLOBAL)
  private static native Number getUndefined();

  @JsType(isNative = true, namespace = "test.foo")
  interface MyNativeJsTypeInterface {}

  @JsType(namespace = JsPackage.GLOBAL, name = "HTMLElement", isNative = true)
  static class HTMLElementConcreteNativeJsType {}

  @JsType(namespace = JsPackage.GLOBAL, name = "HTMLElement", isNative = true)
  static class HTMLElementAnotherConcreteNativeJsType {}

  private static <NI extends MyNativeJsTypeInterface, NC extends HTMLElementConcreteNativeJsType>
      void testCasts() {
    Object myClass;
    assertNotNull(myClass = (ElementLikeNativeInterface) createFoo());
    assertNotNull(myClass = (MyNativeJsTypeInterface) createFoo());
    assertNotNull(myClass = (NI) createFoo());
    assertNotNull(myClass = (HTMLElementConcreteNativeJsType) createNativeButton());
    assertNotNull(myClass = (NC) createNativeButton());

    assertThrowsClassCastException(
        () -> {
          Object unused = (HTMLElementConcreteNativeJsType) createFoo();
        });

    // Test cross cast for native types
    Object nativeButton1 = (HTMLElementConcreteNativeJsType) createNativeButton();
    Object nativeButton2 = (HTMLElementAnotherConcreteNativeJsType) nativeButton1;

    /*
     * If the optimizations are turned on, it is possible for the compiler to dead-strip the
     * variables since they are not used. Therefore the casts could potentially be stripped.
     */
    assertNotNull(myClass);
    assertNotNull(nativeButton1);
    assertNotNull(nativeButton2);
  }

  private static Object createFoo() {
    return new Foo();
  }

  @JsMethod(namespace = "nativejstypes.JsTypeTestHelper")
  public static native Object createNativeButton();

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "*")
  interface Star {}

  private static void testStar() {
    Object object = new Object();

    assertNotNull(object);

    object = Double.valueOf(3.0);
    assertNotNull(object);
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "?")
  interface Wildcard {}

  private static void testWildcard() {
    Object object = new Object();

    assertNotNull(object);

    object = Double.valueOf(3.0);
    assertNotNull(object);
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "?")
  interface NativeFunctionalInterface<T> {
    int f(T t);
  }

  @Wasm("nop") // Implementing native types not supported in Wasm.
  private static void testNativeFunctionalInterface() {
    NativeFunctionalInterface<String> nativeFunctionalInterface = (s) -> 10;
    assertEquals(10, nativeFunctionalInterface.f(""));
  }

  public static void main(String... args) {
    testNativeJsTypeWithNamespace();
    testNativeJsTypeWithoutNamespace();
    testGlobalNativeJsType();
    testNativeEquality();
    testCasts();
    testStar();
    testWildcard();
    testNativeFunctionalInterface();
  }
}
