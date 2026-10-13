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
package jsmethod;

import static com.google.j2cl.integration.testing.Asserts.assertEquals;
import static com.google.j2cl.integration.testing.Asserts.assertNull;
import static com.google.j2cl.integration.testing.Asserts.assertTrue;

import java.io.Serializable;
import jsinterop.annotations.JsConstructor;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

public class Main {
  public static void main(String... args) {
    testCallByConcreteType();
    testCallBySuperParent();
    testCallByJS();
    testJsMethodWithDifferentVisiblities();
    testLambdaImplementingJsMethod();
    testBoxedTypeSupertypes();
    testOverloads();
  }

  public static void testCallByConcreteType() {
    SuperParent sp = new SuperParent();
    Parent p = new Parent();
    Child c = new Child();

    assertTrue((sp.fun(12, 35) == 158));
    assertTrue((sp.bar(6, 7) == 264));
    assertTrue((p.fun(12, 35) == 47));
    assertTrue((p.bar(6, 7) == 42));
    assertTrue((c.fun(12, 35) == 48));
    assertTrue((c.bar(6, 7) == 43));
    assertTrue((p.foo(10) == 10));
    assertTrue((c.foo(10) == 11));
    assertTrue((c.intfFoo(5) == 5));
  }

  public static void testCallBySuperParent() {
    SuperParent sp = new SuperParent();
    SuperParent p = new Parent();
    SuperParent c = new Child();
    Parent pp = new Parent();
    Parent cc = new Child();
    MyInterface intf = new Child();

    assertTrue((sp.fun(12, 35) == 158));
    assertTrue((sp.bar(6, 7) == 264));
    assertTrue((p.fun(12, 35) == 47));
    assertTrue((p.bar(6, 7) == 42));
    assertTrue((c.fun(12, 35) == 48));
    assertTrue((c.bar(6, 7) == 43));
    assertTrue((pp.foo(10) == 10));
    assertTrue((cc.foo(10) == 11));
    assertTrue((intf.intfFoo(5) == 5));
  }

  public static void testCallByJS() {
    Parent p = new Parent();
    Child c = new Child();
    assertTrue((callParentFun(p, 12, 35) == 47));
    assertTrue((callParentBar(p, 6, 7) == 42));
    assertTrue((callParentFoo(p, 10) == 10));
    assertTrue((callChildFun(c, 12, 35) == 48));
    assertTrue((callChildBar(c, 6, 7) == 43));
    assertTrue((callChildFoo(c, 10) == 11));
    assertTrue((callChildIntfFoo(c, 5) == 5));
  }

  @JsMethod(namespace = "jsmethod.helper")
  public static native int callParentFun(Parent p, int a, int b);

  @JsMethod(namespace = "jsmethod.helper")
  public static native int callParentBar(Parent p, int a, int b);

  @JsMethod(namespace = "jsmethod.helper")
  public static native int callParentFoo(Parent p, int a);

  @JsMethod(namespace = "jsmethod.helper")
  public static native int callChildFun(Child c, int a, int b);

  @JsMethod(namespace = "jsmethod.helper")
  public static native int callChildBar(Child c, int a, int b);

  @JsMethod(namespace = "jsmethod.helper")
  public static native int callChildFoo(Child c, int a);

  @JsMethod(namespace = "jsmethod.helper")
  public static native int callChildIntfFoo(Child c, int a);

  static class NonPublicJsMethodClass {
    @JsMethod
    private String foo() {
      return "foo";
    }

    @JsMethod
    String bar() {
      return "bar";
    }
  }

  private static void testJsMethodWithDifferentVisiblities() {
    NonPublicJsMethodClass instance = new NonPublicJsMethodClass();
    assertEquals("foo", instance.foo());
    assertEquals("bar", instance.bar());
    assertEquals("foo", callFoo(instance, null));
    assertEquals("bar", callBar(instance, null));
  }

  @JsMethod(namespace = "jsmethod.helper")
  public static native Object callFoo(Object obj, Object param);

  @JsMethod(namespace = "jsmethod.helper")
  public static native Object callBar(Object obj, Object param);

  interface FunctionalInterfaceWithJsMethod {
    @JsMethod
    String greet();
  }

  private static void testLambdaImplementingJsMethod() {
    FunctionalInterfaceWithJsMethod f = () -> "Hello";
    assertEquals("Hello", f.greet());
  }

  // Use instance @JsMethods instead of static @JsMethods because static native @JsMethod imports
  // are emitted as direct method references (emitAsMethodReference) when building the Wasm imports
  // object, before the Wasm module is instantiated and populates its exported methods.
  static class BoxedSuperTypes {
    @JsMethod
    Comparable<?> passThroughComparable(Comparable<?> c) {
      return c;
    }

    @JsMethod
    CharSequence passThroughCharSequence(CharSequence cs) {
      return cs;
    }

    @JsMethod
    Number passThroughNumber(Number n) {
      return n;
    }

    @JsMethod
    Serializable passThroughSerializable(Serializable s) {
      return s;
    }
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "?")
  private interface NativeBoxedSuperTypes {
    <T> Comparable<T> passThroughComparable(T c);

    CharSequence passThroughCharSequence(Object cs);

    Number passThroughNumber(Object n);

    Serializable passThroughSerializable(Object s);
  }

  private static void testBoxedTypeSupertypes() {
    // Cast to a native interface to force the calls to cross the JS boundary.
    NativeBoxedSuperTypes tester = (NativeBoxedSuperTypes) (Object) new BoxedSuperTypes();
    assertTrue(tester.passThroughComparable("hello").compareTo("hello") == 0);
    assertTrue(tester.passThroughComparable(42.5).compareTo(42.5) == 0);
    assertTrue(tester.passThroughComparable(true).compareTo(true) == 0);
    assertTrue(tester.passThroughComparable(42L).compareTo(42L) == 0);
    assertTrue(tester.passThroughComparable(42).compareTo(42) == 0);
    assertNull(tester.passThroughComparable(null));

    assertTrue(tester.passThroughCharSequence("hello").charAt(1) == 'e');
    assertTrue(tester.passThroughNumber(42.5).doubleValue() == 42.5);
    assertEquals(true, tester.passThroughSerializable(true));
  }

  // Repro for b/560655013.
  static class Overloads {
    @JsConstructor
    public Overloads() {}

    @JsMethod(name = "mObject")
    public String m(Object o) {
      return "m(Object)";
    }

    @JsMethod(name = "mDouble")
    public String m(Double d) {
      return "m(Double)";
    }
  }

  @JsType(isNative = true, namespace = "jsmethod.Main", name = "Overloads")
  static class NativeOverloads {
    public native String mObject(Object o);

    public native String mDouble(Double d);

    @JsMethod(name = "mObject")
    public native String m(Object o);

    @JsMethod(name = "mDouble")
    public native String m(Double o);
  }

  private static void testOverloads() {
    Overloads overloads = new Overloads();
    assertEquals("m(Object)", overloads.m(new Object()));
    assertEquals("m(Double)", overloads.m(0d));

    NativeOverloads nativeOverloads = new NativeOverloads();
    // Check overloaded JsMethods.
    assertEquals("m(Object)", nativeOverloads.mObject(null));
    assertEquals("m(Double)", nativeOverloads.mDouble(0d));

    // Check overloaded native methods.
    assertEquals("m(Object)", nativeOverloads.m(new Object()));
    assertEquals("m(Double)", nativeOverloads.m(0d));
  }
}
