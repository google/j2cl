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
package jsproperties;

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
    testConcreteJsType();
    testConcreteJsType_static();
    testJavaClassImplementingMyJsTypeInterfaceWithProperty();
    testJsPropertyGetX();
    testJsPropertyGetX_undefined();
    testJsPropertyIsX();
    testNativeJsType();
    testNativeJsTypeWithConstructor();
    testNonJsType();
    testNonJsType_static();
    testNativeStaticJsProperty();
    testNativeInstanceJsProperty();
    testDefaultMethodJsProperty();
  }

  public static final int GET_X = 100;
  public static final int SET_X = 50;

  @JsType
  interface MyJsTypeInterfaceWithProperty {
    @JsProperty
    int getX();

    @JsProperty
    void setX(int x);
  }

  static class MyJavaTypeImplementingMyJsTypeInterfaceWithProperty
      implements MyJsTypeInterfaceWithProperty {
    private int x;

    @Override
    public int getX() {
      return x + GET_X;
    }

    @Override
    public void setX(int x) {
      this.x = x + SET_X;
    }
  }

  private static void testJavaClassImplementingMyJsTypeInterfaceWithProperty() {
    MyJavaTypeImplementingMyJsTypeInterfaceWithProperty obj =
        new MyJavaTypeImplementingMyJsTypeInterfaceWithProperty();
    assertEquals(0 + GET_X, getProperty(obj, "x"));
    assertEquals(0 + GET_X, obj.getX());
    assertEquals(0, obj.x);

    setProperty(obj, "x", 10);
    assertEquals(10 + GET_X + SET_X, getProperty(obj, "x"));
    assertEquals(10 + GET_X + SET_X, obj.getX());
    assertEquals(10 + SET_X, obj.x);

    obj.setX(12);
    assertEquals(12 + GET_X + SET_X, getProperty(obj, "x"));
    assertEquals(12 + GET_X + SET_X, obj.getX());
    assertEquals(12 + SET_X, obj.x);

    MyJsTypeInterfaceWithProperty intf = new MyJavaTypeImplementingMyJsTypeInterfaceWithProperty();
    assertEquals(0 + GET_X, getProperty(intf, "x"));
    assertEquals(0 + GET_X, intf.getX());
    assertEquals(0, ((MyJavaTypeImplementingMyJsTypeInterfaceWithProperty) intf).x);

    setProperty(intf, "x", 10);
    assertEquals(10 + GET_X + SET_X, getProperty(intf, "x"));
    assertEquals(10 + GET_X + SET_X, intf.getX());
    assertEquals(10 + SET_X, ((MyJavaTypeImplementingMyJsTypeInterfaceWithProperty) intf).x);

    intf.setX(12);
    assertEquals(12 + GET_X + SET_X, getProperty(intf, "x"));
    assertEquals(12 + GET_X + SET_X, intf.getX());
    assertEquals(12 + SET_X, ((MyJavaTypeImplementingMyJsTypeInterfaceWithProperty) intf).x);
  }

  @JsType(namespace = "jsproperties", name = "MyConcreteJsType")
  public static class MyConcreteJsType {
    public int x;

    @JsProperty
    public int getY() {
      return x + GET_X;
    }

    @JsProperty
    public void setY(int x) {
      this.x = x + SET_X;
    }

    @JsProperty(name = "abc")
    public int getC() {
      return x + GET_X;
    }

    @JsProperty(name = "abc")
    public void setC(int x) {
      this.x = x + SET_X;
    }

    public static int staticX;

    @JsProperty
    public static int getStaticY() {
      return staticX + GET_X;
    }

    @JsProperty
    public static void setStaticY(int x) {
      staticX = x + SET_X;
    }

    @JsProperty(name = "abc")
    public static int getStaticC() {
      return staticX + GET_X;
    }

    @JsProperty(name = "abc")
    public static void setStaticC(int x) {
      staticX = x + SET_X;
    }
  }

  private static void testConcreteJsType() {
    MyConcreteJsType obj = new MyConcreteJsType();
    assertEquals(0 + GET_X, getProperty(obj, "y"));
    assertEquals(0 + GET_X, obj.getY());
    assertEquals(0, getProperty(obj, "x"));
    assertEquals(0, obj.x);

    setProperty(obj, "x", 8);
    assertEquals(8 + GET_X, getProperty(obj, "y"));
    assertEquals(8 + GET_X, obj.getY());
    assertEquals(8, getProperty(obj, "x"));
    assertEquals(8, obj.x);

    obj.x = 9;
    assertEquals(9 + GET_X, getProperty(obj, "y"));
    assertEquals(9 + GET_X, obj.getY());
    assertEquals(9, getProperty(obj, "x"));
    assertEquals(9, obj.x);

    setProperty(obj, "y", 10);
    assertEquals(10 + GET_X + SET_X, getProperty(obj, "y"));
    assertEquals(10 + GET_X + SET_X, obj.getY());
    assertEquals(10 + SET_X, getProperty(obj, "x"));
    assertEquals(10 + SET_X, obj.x);

    obj.setY(12);
    assertEquals(12 + GET_X + SET_X, getProperty(obj, "y"));
    assertEquals(12 + GET_X + SET_X, obj.getY());
    assertEquals(12 + SET_X, getProperty(obj, "x"));
    assertEquals(12 + SET_X, obj.x);

    setMyConcreteJsTypeAbc(obj, 20);
    assertEquals(20 + GET_X + SET_X, getMyConcreteJsTypeAbc(obj));
    assertEquals(20 + GET_X + SET_X, obj.getC());
    assertEquals(20 + SET_X, getProperty(obj, "x"));
    assertEquals(20 + SET_X, obj.x);

    obj.setC(22);
    assertEquals(22 + GET_X + SET_X, getMyConcreteJsTypeAbc(obj));
    assertEquals(22 + GET_X + SET_X, obj.getC());
    assertEquals(22 + SET_X, getProperty(obj, "x"));
    assertEquals(22 + SET_X, obj.x);
  }

  private static void testConcreteJsType_static() {
    assertEquals(0 + GET_X, getMyConcreteJsTypeStaticY());
    assertEquals(0 + GET_X, MyConcreteJsType.getStaticY());
    assertEquals(0, getMyConcreteJsTypeStaticX());
    assertEquals(0, MyConcreteJsType.staticX);

    setMyConcreteJsTypeStaticX(8);
    assertEquals(8 + GET_X, getMyConcreteJsTypeStaticY());
    assertEquals(8 + GET_X, MyConcreteJsType.getStaticY());
    assertEquals(8, getMyConcreteJsTypeStaticX());
    assertEquals(8, MyConcreteJsType.staticX);

    MyConcreteJsType.staticX = 9;
    assertEquals(9 + GET_X, getMyConcreteJsTypeStaticY());
    assertEquals(9 + GET_X, MyConcreteJsType.getStaticY());
    assertEquals(9, getMyConcreteJsTypeStaticX());
    assertEquals(9, MyConcreteJsType.staticX);

    setMyConcreteJsTypeStaticY(11);
    assertEquals(11 + GET_X + SET_X, getMyConcreteJsTypeStaticY());
    assertEquals(11 + GET_X + SET_X, MyConcreteJsType.getStaticY());
    assertEquals(11 + SET_X, getMyConcreteJsTypeStaticX());
    assertEquals(11 + SET_X, MyConcreteJsType.staticX);

    MyConcreteJsType.setStaticY(13);
    assertEquals(13 + GET_X + SET_X, getMyConcreteJsTypeStaticY());
    assertEquals(13 + GET_X + SET_X, MyConcreteJsType.getStaticY());
    assertEquals(13 + SET_X, getMyConcreteJsTypeStaticX());
    assertEquals(13 + SET_X, MyConcreteJsType.staticX);

    setMyConcreteJsTypeStaticAbc(20);
    assertEquals(20 + GET_X + SET_X, getMyConcreteJsTypeStaticAbc());
    assertEquals(20 + GET_X + SET_X, MyConcreteJsType.getStaticC());
    assertEquals(20 + SET_X, getMyConcreteJsTypeStaticX());
    assertEquals(20 + SET_X, MyConcreteJsType.staticX);

    MyConcreteJsType.setStaticC(21);
    assertEquals(21 + GET_X + SET_X, getMyConcreteJsTypeStaticAbc());
    assertEquals(21 + GET_X + SET_X, MyConcreteJsType.getStaticC());
    assertEquals(21 + SET_X, getMyConcreteJsTypeStaticX());
    assertEquals(21 + SET_X, MyConcreteJsType.staticX);
  }

  @JsType(isNative = true, namespace = "jsproperties", name = "MyNativeJsType")
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

  @JsType(isNative = true, namespace = "jsproperties", name = "MyNativeJsType")
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

  @JsType(isNative = true, namespace = "jsproperties")
  interface JsTypeIsProperty {

    @Wasm("nop") // TODO(b/559305464): This type of import is not supported in Wasm.
    @JsProperty
    boolean isX();

    @Wasm("nop") // TODO(b/559305464): This type of import is not supported in Wasm.
    @JsProperty
    void setX(boolean x);
  }

  @Wasm("nop") // TODO(b/559305464): This type of import is not supported in Wasm.
  private static void testJsPropertyIsX() {
    JsTypeIsProperty object = createJsTypeIsProperty();

    assertFalse(object.isX());
    object.setX(true);
    assertTrue(object.isX());
    object.setX(false);
    assertFalse(object.isX());
  }

  @JsType(isNative = true, namespace = "jsproperties")
  interface JsTypeGetProperty {

    @Wasm("nop") // TODO(b/559305464): This type of import is not supported in Wasm.
    @JsProperty
    int getX();

    @Wasm("nop") // TODO(b/559305464): This type of import is not supported in Wasm.
    @JsProperty
    void setX(int x);
  }

  // Native undefined/null indistinguishable after conversion in Wasm.
  @Wasm("nop")
  private static void testJsPropertyGetX_undefined() {
    JsTypeGetProperty object = createJsTypeGetProperty();
    assertTrue(isUndefined(object.getX()));
  }

  @Wasm("nop") // TODO(b/559305464): This type of import is not supported in Wasm.
  private static void testJsPropertyGetX() {
    JsTypeGetProperty object = createJsTypeGetProperty();
    object.setX(10);
    assertEquals(10, object.getX());
    object.setX(0);
    assertEquals(0, object.getX());
  }

  static class NonJsType {
    private int x;

    @JsProperty public int y;

    @JsProperty
    public void setX(int x) {
      this.x = x;
    }

    @JsProperty
    public int getX() {
      return x;
    }

    @JsProperty(name = "abc")
    public int getC() {
      return x;
    }

    @JsProperty(name = "abc")
    public void setC(int x) {
      this.x = x;
    }

    public static int staticX;

    @JsProperty
    public static int getStaticX() {
      return staticX;
    }

    @JsProperty
    public static void setStaticX(int x) {
      NonJsType.staticX = x;
    }

    @JsProperty(name = "abc")
    public static int getStaticC() {
      return staticX;
    }

    @JsProperty(name = "abc")
    public static void setStaticC(int x) {
      NonJsType.staticX = x;
    }

    // TODO(b/556880337): Instance native member on non-native type not supported.
    @Wasm("nop")
    @JsProperty(name = "hasOwnProperty")
    public native Object getA();

    @JsProperty(name = "Math.PI", namespace = JsPackage.GLOBAL)
    public static native double getB();
  }

  private static void testNonJsType() {
    NonJsType object = new NonJsType();

    object.setX(10);
    assertEquals(10, object.getX());
    assertEquals(10, getProperty(object, "x"));

    setProperty(object, "x", 4);
    assertEquals(4, object.getX());
    assertEquals(4, getProperty(object, "x"));

    object.y = 20;
    assertEquals(20, object.y);
    assertEquals(20, getProperty(object, "y"));

    setProperty(object, "y", 24);
    assertEquals(24, object.y);
    assertEquals(24, getProperty(object, "y"));

    object.setC(30);
    assertEquals(30, object.getC());
    assertEquals(30, getNonJsTypeAbc(object));

    setNonJsTypeAbc(object, 34);
    assertEquals(34, object.getC());
    assertEquals(34, getNonJsTypeAbc(object));
  }

  private static void testNonJsType_static() {
    NonJsType.setStaticX(10);
    assertEquals(10, NonJsType.getStaticX());
    assertEquals(10, getNonJsTypeStaticX());
    assertEquals(10, NonJsType.staticX);

    setNonJsTypeStaticX(4);
    assertEquals(4, NonJsType.getStaticX());
    assertEquals(4, getNonJsTypeStaticX());
    assertEquals(4, NonJsType.staticX);

    NonJsType.setStaticC(20);
    assertEquals(20, NonJsType.getStaticC());
    assertEquals(20, getNonJsTypeStaticAbc());
    assertEquals(20, NonJsType.staticX);

    setNonJsTypeStaticAbc(24);
    assertEquals(24, NonJsType.getStaticC());
    assertEquals(24, getNonJsTypeStaticAbc());
    assertEquals(24, NonJsType.staticX);
  }

  private static void testNativeStaticJsProperty() {
    int pi = (int) NonJsType.getB();
    assertTrue(pi == 3);
  }

  // TODO(b/556880337): Instance native member on non-native type not supported.
  @Wasm("nop")
  private static void testNativeInstanceJsProperty() {
    assertTrue(new NonJsType().getA() != null);
  }

  interface InterfaceWithDefaultJsProperties {
    int getterCalled();

    void setterCalled(int v);

    @JsProperty
    default int getValue() {
      return getterCalled();
    }

    @JsProperty
    default void setValue(int value) {
      setterCalled(value);
    }
  }

  static class ImplementorWithDefaultJsProperties implements InterfaceWithDefaultJsProperties {
    private int v = 0;

    @Override
    public int getterCalled() {
      return v;
    }

    @Override
    public void setterCalled(int v) {
      this.v = v;
    }
  }

  private static void testDefaultMethodJsProperty() {
    InterfaceWithDefaultJsProperties i = new ImplementorWithDefaultJsProperties();
    i.setValue(3);
    assertTrue(3 == i.getValue());
  }

  @JsMethod(namespace = "jsproperties.JsPropertyTestHelper")
  private static native MyNativeJsType createMyNativeJsType();

  @JsMethod(namespace = "jsproperties.JsPropertyTestHelper")
  private static native JsTypeGetProperty createJsTypeGetProperty();

  @JsMethod(namespace = "jsproperties.JsPropertyTestHelper")
  private static native JsTypeIsProperty createJsTypeIsProperty();

  @JsMethod(namespace = "jsproperties.JsPropertyTestHelper")
  private static native boolean isUndefined(int value);

  @JsMethod(namespace = "jsproperties.JsPropertyTestHelper")
  private static native int getMyConcreteJsTypeStaticY();

  @JsMethod(namespace = "jsproperties.JsPropertyTestHelper")
  private static native void setMyConcreteJsTypeStaticY(int value);

  @JsMethod(namespace = "jsproperties.JsPropertyTestHelper")
  private static native int getMyConcreteJsTypeStaticX();

  @JsMethod(namespace = "jsproperties.JsPropertyTestHelper")
  private static native void setMyConcreteJsTypeStaticX(int value);

  @JsMethod(namespace = "jsproperties.JsPropertyTestHelper")
  private static native int getMyConcreteJsTypeStaticAbc();

  @JsMethod(namespace = "jsproperties.JsPropertyTestHelper")
  private static native void setMyConcreteJsTypeStaticAbc(int value);

  @JsMethod(namespace = "jsproperties.JsPropertyTestHelper")
  static native int getProperty(Object object, String name);

  @JsMethod(namespace = "jsproperties.JsPropertyTestHelper")
  static native void setProperty(Object object, String name, int value);

  @JsMethod(namespace = "jsproperties.JsPropertyTestHelper")
  private static native int getNonJsTypeStaticX();

  @JsMethod(namespace = "jsproperties.JsPropertyTestHelper")
  private static native void setNonJsTypeStaticX(int value);

  @JsMethod(namespace = "jsproperties.JsPropertyTestHelper")
  private static native int getMyConcreteJsTypeAbc(Object object);

  @JsMethod(namespace = "jsproperties.JsPropertyTestHelper")
  private static native void setMyConcreteJsTypeAbc(Object object, int value);

  @JsMethod(namespace = "jsproperties.JsPropertyTestHelper")
  private static native int getNonJsTypeAbc(Object object);

  @JsMethod(namespace = "jsproperties.JsPropertyTestHelper")
  private static native void setNonJsTypeAbc(Object object, int value);

  @JsMethod(namespace = "jsproperties.JsPropertyTestHelper")
  private static native int getNonJsTypeStaticAbc();

  @JsMethod(namespace = "jsproperties.JsPropertyTestHelper")
  private static native void setNonJsTypeStaticAbc(int value);
}
