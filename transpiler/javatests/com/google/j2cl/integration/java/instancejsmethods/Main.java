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
package instancejsmethods;

import static com.google.j2cl.integration.testing.Asserts.assertEquals;
import static com.google.j2cl.integration.testing.Asserts.assertTrue;

import jsinterop.annotations.JsConstructor;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsType;

public class Main {
  public static void main(String... args) {
    testCallByConcreteType();
    testCallBySuperParent();
    testCallByJS();
    testOverloads();
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

  @JsMethod(namespace = "instancejsmethods.helper")
  public static native int callParentFun(Parent p, int a, int b);

  @JsMethod(namespace = "instancejsmethods.helper")
  public static native int callParentBar(Parent p, int a, int b);

  @JsMethod(namespace = "instancejsmethods.helper")
  public static native int callParentFoo(Parent p, int a);

  @JsMethod(namespace = "instancejsmethods.helper")
  public static native int callChildFun(Child c, int a, int b);

  @JsMethod(namespace = "instancejsmethods.helper")
  public static native int callChildBar(Child c, int a, int b);

  @JsMethod(namespace = "instancejsmethods.helper")
  public static native int callChildFoo(Child c, int a);

  @JsMethod(namespace = "instancejsmethods.helper")
  public static native int callChildIntfFoo(Child c, int a);

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

  @JsType(isNative = true, namespace = "instancejsmethods.Main", name = "Overloads")
  static class NativeOverloads {
    public native String mObject(Object o);

    // TODO(b/560655013): Uncomment once the bug is fixed.
    // public native String mDouble(Double d);

    @JsMethod(name = "mObject")
    public native String m(Object o);

    // TODO(b/560655013): Uncomment once the bug is fixed.
    // @JsMethod(name = "mDouble")
    // public native String m(Double o);
  }

  private static void testOverloads() {
    Overloads overloads = new Overloads();
    assertEquals("m(Object)", overloads.m(new Object()));
    assertEquals("m(Double)", overloads.m(0d));

    NativeOverloads nativeOverloads = new NativeOverloads();
    // Check overloaded JsMethods.
    assertEquals("m(Object)", nativeOverloads.mObject(null));
    // TODO(b/560655013): Uncomment once the bug is fixed.
    // assertEquals("m(Double)", nativeOverloads.mDouble(0d));

    // Check overloaded native methods.
    assertEquals("m(Object)", nativeOverloads.m(new Object()));
    // TODO(b/560655013): Uncomment once the bug is fixed.
    // assertEquals("m(Double)", nativeOverloads.m(0d));
  }
}
