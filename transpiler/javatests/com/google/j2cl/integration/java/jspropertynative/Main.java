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
package jspropertynative;

import static com.google.j2cl.integration.testing.Asserts.assertEquals;
import static com.google.j2cl.integration.testing.Asserts.assertFalse;
import static com.google.j2cl.integration.testing.Asserts.assertTrue;

import javaemul.internal.annotations.Wasm;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

public class Main {
  public static void main(String... args) {
    testJsPropertyGetY();
    testJsPropertyGetY_undefined();
    testJsPropertyIsX();
    testNativeJsType();
    testNativeJsTypeWithConstructor();
    testNativeStaticJsProperty();
    testNativeInstanceJsProperty();
  }

  @JsType(isNative = true, namespace = "jspropertynative", name = "MyNativeJsType")
  public static class MyNativeJsType {
    public MyNativeJsType() {}

    public MyNativeJsType(int n) {}

    public static int staticX;

    public boolean ctorExecuted;

    public int x;

    @JsProperty
    public native int getY();

    @JsProperty
    public native void setY(int x);

    public native int sum(int bias);
  }

  private static void testNativeJsType() {
    MyNativeJsType.staticX = 34;
    assertEquals(34, MyNativeJsType.staticX);

    MyNativeJsType obj = createMyNativeJsType();
    assertTrue(obj.ctorExecuted);
    assertEquals(obj.x, 0);
    obj.x = 72;
    assertEquals(72, obj.x);
    assertEquals(74, obj.sum(2));

    assertEquals(0, obj.getY());
    obj.setY(91);
    assertEquals(91, obj.getY());
  }

  @JsType(isNative = true, namespace = "jspropertynative", name = "MyNativeJsType")
  public static class MyNativeJsTypeWithConstructor {
    public MyNativeJsTypeWithConstructor(int x) {}

    public boolean ctorExecuted;
    public int x;
  }

  private static void testNativeJsTypeWithConstructor() {
    MyNativeJsTypeWithConstructor obj = new MyNativeJsTypeWithConstructor(12);
    assertTrue(obj.ctorExecuted);
    assertEquals(12, obj.x);
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "?")
  interface HasIsX {

    @JsProperty
    boolean isX();

    @JsProperty
    void setX(boolean x);
  }

  private static void testJsPropertyIsX() {
    HasIsX object = createHasIsX();
    assertFalse(object.isX());
    object.setX(true);
    assertTrue(object.isX());
    object.setX(false);
    assertFalse(object.isX());
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "?")
  interface HasGetY {

    @JsProperty
    int getY();

    @JsProperty
    void setY(int x);
  }

  // Native undefined/null indistinguishable after conversion in Wasm.
  @Wasm("nop")
  private static void testJsPropertyGetY_undefined() {
    HasGetY object = createHasGetY();
    assertTrue(isUndefined(object.getY()));
  }

  private static void testJsPropertyGetY() {
    HasGetY object = createHasGetY();
    object.setY(10);
    assertEquals(10, object.getY());
    object.setY(0);
    assertEquals(0, object.getY());
  }

  static class NonNativeTypeWithNativeProperties {
    // TODO(b/556880337): Instance native member on non-native type not supported.
    @Wasm("nop")
    @JsProperty(name = "hasOwnProperty")
    public native Object getA();

    @JsProperty(name = "Math.PI", namespace = JsPackage.GLOBAL)
    public static native double getB();
  }

  private static void testNativeStaticJsProperty() {
    int pi = (int) NonNativeTypeWithNativeProperties.getB();
    assertTrue(pi == 3);
  }

  // TODO(b/556880337): Instance native member on non-native type not supported.
  @Wasm("nop")
  private static void testNativeInstanceJsProperty() {
    assertTrue(new NonNativeTypeWithNativeProperties().getA() != null);
  }

  @JsMethod(namespace = "jspropertynative.helper")
  private static native MyNativeJsType createMyNativeJsType();

  @JsMethod(namespace = "jspropertynative.helper", name = "createJsObject")
  private static native HasGetY createHasGetY();

  @JsMethod(namespace = "jspropertynative.helper", name = "createJsObject")
  private static native HasIsX createHasIsX();

  @JsMethod(namespace = "jspropertynative.helper")
  private static native boolean isUndefined(int value);
}
