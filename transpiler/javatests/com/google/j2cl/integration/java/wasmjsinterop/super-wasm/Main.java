/*
 * Copyright 2022 Google Inc.
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
package wasmjsinterop;

import static com.google.j2cl.integration.testing.Asserts.assertEquals;
import static com.google.j2cl.integration.testing.Asserts.assertSame;
import static com.google.j2cl.integration.testing.Asserts.assertTrue;

import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/** Tests J2WASM jsinterop features. */
public final class Main {
  // TODO(b/479895505): Add JsFunction tests once the functionality is enabled.
  public static void main(String... args) throws Exception {
    testJsString();
    testJsNumber();
    testJsBoolean();
    testJsLong();
    testGeneralizedJavaWasmBoundary();
    testObjectMethods();
    testGlobalJsType();
    testNonglobalJsType();
    testJsTypeInterface();
  }

  private static void testJsString() {
    assertEquals(null, appendInJs(null, null));

    String empty = "";
    assertEquals(empty, appendInJs(empty, empty));

    String foo = "String with special ";
    String bar = "chars like $'%$\"^";
    assertEquals(foo + bar, appendInJs(foo, bar));
  }

  private static void testJsNumber() {
    assertEquals(null, sumDoublesInJs(null, null));
    assertEquals(null, sumDoublesInJs(1.5, null));
    assertEquals(null, sumDoublesInJs(null, 2.5));

    Double d1 = 3.25;
    Double d2 = 6.75;
    assertEquals(10.0, sumDoublesInJs(d1, d2));
  }

  private static void testJsBoolean() {
    assertEquals(null, andBooleansInJs(null, null));
    assertEquals(null, andBooleansInJs(true, null));
    assertEquals(null, andBooleansInJs(null, false));

    Boolean b1 = true;
    Boolean b2 = false;
    assertEquals(false, andBooleansInJs(b1, b2));
    assertEquals(true, andBooleansInJs(true, true));
  }

  private static void testJsLong() {
    assertEquals(null, sumLongsInJs(null, null));
    assertEquals(null, sumLongsInJs(100L, null));
    assertEquals(null, sumLongsInJs(null, 200L));

    Long l1 = 12121212121212L;
    Long l2 = 21212121212121L;
    assertEquals(33333333333333L, sumLongsInJs(l1, l2));
  }

  private static void testGeneralizedJavaWasmBoundary() {
    assertEquals(null, roundTripAsJavaLangObject(null));

    String s = "hello";
    Object sRes = roundTripAsJavaLangObject(s);
    assertTrue(sRes instanceof String);
    assertEquals("hello", sRes);

    Double d = 3.14;
    Object dRes = roundTripAsJavaLangObject(d);
    assertTrue(dRes instanceof Double);
    assertEquals(3.14, dRes);

    Object o = new Object();
    Object oRes = roundTripAsJavaLangObject(o);
    assertSame(o, oRes);

    o = getJsNull();
    assertEquals(null, o);
    assertTrue(o == null);

    o = getJsUndefined();
    assertEquals(null, o);
    assertTrue(o == null);

    o = getJsTrue();
    assertTrue(o instanceof Boolean);
    assertTrue((Boolean) o);

    o = getJsOnePointFive();
    assertTrue(o instanceof Double);
    assertEquals(1.5, o);

    o = getJsHello();
    assertTrue(o instanceof String);
    assertEquals("hello", o);

    o = getJsFoo();
    assertEquals(5, ((Foo) o).getValue());

    o = getJsNullAsNative();
    assertEquals(null, o);
    assertTrue(o == null);

    o = getJsUndefinedAsNative();
    assertEquals(null, o);
    assertTrue(o == null);

    o = getJsTrueAsNative();
    assertTrue(o instanceof Boolean);
    assertTrue((Boolean) o);

    o = getJsOnePointFiveAsNative();
    assertTrue(o instanceof Double);
    assertEquals(1.5, o);

    o = getJsHelloAsNative();
    assertTrue(o instanceof String);
    assertEquals("hello", o);

    o = getJsFooAsNative();
    assertEquals(5, ((Foo) o).getValue());

    o = getJsFooAsNative();
    OtherNativeJsObject otherO = (OtherNativeJsObject) o;
    Foo f = (Foo) otherO;
    assertEquals(5, f.getValue());

    o = getJsFooArray();
    Foo[] fooArr = (Foo[]) o;
    assertEquals(2, fooArr.length);
    assertEquals(5, fooArr[0].getValue());
    assertEquals(6, fooArr[1].getValue());

    o = getJsFooArrayAsNative();
    NativeJsObject[] nativeArr = (NativeJsObject[]) o;
    assertEquals(2, nativeArr.length);

    fooArr = (Foo[]) nativeArr;
    assertEquals(5, fooArr[0].getValue());
    assertEquals(6, fooArr[1].getValue());

    // TODO(b/540448377): Enable when wrapper identity is preserved.
    // assertSame(getJsFoo(), getJsFoo());
  }

  private static void testObjectMethods() {
    // Test toString.
    assertEquals("Hi, hello", "Hi, " + getJsHello());
    assertEquals("Hi, hello", "Hi, " + getJsHelloAsNative());

    assertEquals("Hi, 1.5", "Hi, " + getJsOnePointFive());
    assertEquals("Hi, 1.5", "Hi, " + getJsOnePointFiveAsNative());

    // TODOb/540445903): Delegate equals/hashCode/toString to the underlying native object if
    // present
    // assertEquals("Foo(5)", "" + getJsFoo());
    // assertEquals("Hi, " + getJsFoo(), "Hi, " + getJsFooAsNative());

    // Test equals.
    assertTrue(getJsHello().equals(getJsHelloAsNative()));
    assertTrue(getJsOnePointFive().equals(getJsOnePointFiveAsNative()));
    // TODOb/540445903): Delegate equals/hashCode/toString to the underlying native object if
    // present
    // assertTrue(getJsFoo().equals(getJsFooAsNative()));

    // Test hashCode.
    assertEquals(getJsHello().hashCode(), getJsHelloAsNative().hashCode());
    assertEquals(getJsOnePointFive().hashCode(), getJsOnePointFiveAsNative().hashCode());
    // TODOb/540445903): Delegate equals/hashCode/toString to the underlying native object if
    // present
    // assertEquals(getJsFoo().hashCode(), getJsFooAsNative().hashCode());
  }

  private static void testGlobalJsType() {
    RegExp regExp = new RegExp("test", "g");
    assertEquals(true, regExp.test("test"));
    assertEquals(4, regExp.lastIndex);
    assertEquals(4, regExp.getLastIndex());

    regExp.lastIndex = 0;
    assertEquals(0, regExp.lastIndex);
    assertEquals(0, regExp.getLastIndex());

    regExp.setLastIndex(1);
    assertEquals(1, regExp.lastIndex);
    assertEquals(1, regExp.getLastIndex());

    assertEquals(false, regExp.test("rest"));
  }

  private static void testNonglobalJsType() {
    Foo f = new Foo(1);
    assertEquals(1, f.getValue());
    assertEquals(3, f.sum(1, 2));
    assertEquals(6, Foo.mult(2, 3));
    assertEquals(19, f.myOverlay(3, 4));
  }

  private static void testJsTypeInterface() {
    FooInterface foo = createFooInterface();
    assertEquals(3, foo.sum(1, 2));
    assertEquals(4, foo.sumPlusOneOverlay(1, 2));
    assertEquals(5, FooInterface.sumPlusTwoStaticOverlay(1, 2));
  }

  @JsType(isNative = true, name = "RegExp", namespace = JsPackage.GLOBAL)
  public static class RegExp {
    @JsProperty public int lastIndex;

    public RegExp(String pattern, String flags) {}

    public native boolean test(String value);

    // JsProperty methods.
    @JsProperty
    public native int getLastIndex();

    @JsProperty
    public native void setLastIndex(int value);
  }

  @JsType(isNative = true, name = "Foo", namespace = "test")
  public static class Foo {
    public Foo(int value) {}

    public native int getValue();

    public native int sum(int a, int b);

    public static native int mult(int a, int b);

    @JsOverlay
    private int myOverlay(int a, int b) {
      return privateOverlay(a, b);
    }

    @JsOverlay
    private int privateOverlay(int a, int b) {
      return mult(a, b) + sum(a, b);
    }
  }

  @JsType(isNative = true, name = "Foo", namespace = "test")
  public interface FooInterface {
    int sum(int a, int b);

    @JsOverlay
    default int sumPlusOneOverlay(int a, int b) {
      return a + b + 1;
    }

    @JsOverlay
    static int sumPlusTwoStaticOverlay(int a, int b) {
      return a + b + 2;
    }
  }

  @JsMethod(namespace = "test.Foo", name = "create")
  static native FooInterface createFooInterface();

  @JsMethod(namespace = "test.utils")
  private static native String appendInJs(String a, String b);

  @JsMethod(namespace = "test.utils")
  private static native Double sumDoublesInJs(Double a, Double b);

  @JsMethod(namespace = "test.utils", name = "identityInJs")
  private static native Object roundTripAsJavaLangObject(Object o);

  @JsMethod(namespace = "test.utils")
  private static native Boolean andBooleansInJs(Boolean a, Boolean b);

  @JsMethod(namespace = "test.utils")
  private static native Long sumLongsInJs(Long a, Long b);

  @JsMethod(namespace = "test.utils")
  private static native Object getJsNull();

  @JsMethod(namespace = "test.utils")
  private static native Object getJsUndefined();

  @JsMethod(namespace = "test.utils")
  private static native Object getJsTrue();

  @JsMethod(namespace = "test.utils")
  private static native Object getJsOnePointFive();

  @JsMethod(namespace = "test.utils")
  private static native Object getJsHello();

  @JsMethod(namespace = "test.utils")
  private static native Object getJsFoo();

  @JsMethod(namespace = "test.utils")
  private static native Object getJsFooArray();

  @JsType(isNative = true, name = "?", namespace = JsPackage.GLOBAL)
  interface NativeJsObject {}

  @JsType(isNative = true, name = "?", namespace = JsPackage.GLOBAL)
  interface OtherNativeJsObject {}

  @JsMethod(namespace = "test.utils", name = "getJsNull")
  private static native NativeJsObject getJsNullAsNative();

  @JsMethod(namespace = "test.utils", name = "getJsUndefined")
  private static native NativeJsObject getJsUndefinedAsNative();

  @JsMethod(namespace = "test.utils", name = "getJsTrue")
  private static native NativeJsObject getJsTrueAsNative();

  @JsMethod(namespace = "test.utils", name = "getJsOnePointFive")
  private static native NativeJsObject getJsOnePointFiveAsNative();

  @JsMethod(namespace = "test.utils", name = "getJsHello")
  private static native NativeJsObject getJsHelloAsNative();

  @JsMethod(namespace = "test.utils", name = "getJsFoo")
  private static native NativeJsObject getJsFooAsNative();

  @JsMethod(namespace = "test.utils", name = "getJsFooArray")
  private static native NativeJsObject getJsFooArrayAsNative();
}
