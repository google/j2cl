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
package com.google.j2cl.transpiler.integration.jsinteroptests;

import static jsinterop.annotations.JsPackage.GLOBAL;

import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsProperty;

/**
 * Tests JsMethod functionality.
 */
public class JsMethodTest extends MyTestCase {
  public static void testAll() {
    JsMethodTest test = new JsMethodTest();
    test.testNativeJsMethod();
    test.testStaticNativeJsMethod();
    test.testStaticNativeJsPropertyGetter();
    test.testStaticNativeJsPropertySetter();
    test.testLambdaImplementingJsMethod();
    test.testLambdaRequiringJsMethodBridge();
  }

  static class MyObject {
    @JsProperty public int mine;
  }

  public void testNativeJsMethod() {
    MyObject obj = new MyObject();
    obj.mine = 0;
    assertTrue(PropertyUtils.hasOwnPropertyMine(obj));
    assertFalse(PropertyUtils.hasOwnPropertyToString(obj));
  }

  @JsMethod(namespace = GLOBAL)
  private static native boolean isFinite(double d);

  public void testStaticNativeJsMethod() {
    assertFalse(isFinite(Double.POSITIVE_INFINITY));
    assertFalse(isFinite(Double.NEGATIVE_INFINITY));
    assertFalse(isFinite(Double.NaN));
    assertTrue(isFinite(0));
    assertTrue(isFinite(1));
  }

  @JsProperty(namespace = GLOBAL, name = "NaN")
  private static native double getNaN();

  @JsProperty(namespace = GLOBAL, name = "Infinity")
  private static native double infinity();

  public void testStaticNativeJsPropertyGetter() {
    assertTrue(getNaN() != getNaN());
    assertTrue(Double.isInfinite(infinity()));
    assertTrue(Double.isInfinite(-infinity()));
  }

  @JsProperty(namespace = "window")
  private static native void setJsInteropSecret(String magic);

  @JsProperty(namespace = "window")
  private static native String getJsInteropSecret();

  public void testStaticNativeJsPropertySetter() {
    setJsInteropSecret("very secret!");
    assertEquals("very secret!", getJsInteropSecret());
  }

  interface FunctionalInterfaceWithJsMethod {
    @JsMethod
    String greet();
  }

  public void testLambdaImplementingJsMethod() {
    FunctionalInterfaceWithJsMethod f = () -> "Hello";
    assertEquals("Hello", f.greet());
  }

  interface NullSupplier {
    default Object get() {
      return null;
    }
  }

  interface JsSupplier extends NullSupplier {
    @JsMethod
    @Override
    Object get();
  }

  public void testLambdaRequiringJsMethodBridge() {
    JsSupplier aSupplier = () -> "Hello";
    NullSupplier aliasToSupplier = aSupplier;
    assertEquals("Hello", aSupplier.get());
    assertEquals("Hello", aliasToSupplier.get());
  }
}
