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

import static com.google.j2cl.integration.testing.Asserts.assertTrue;

import jsinterop.annotations.JsMethod;

public class Main {
  public static void main(String... args) {
    testCallByConcreteType();
    testCallBySuperParent();
    testCallByJS();
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

  @JsMethod
  public static native int callParentFun(Parent p, int a, int b);

  @JsMethod
  public static native int callParentBar(Parent p, int a, int b);

  @JsMethod
  public static native int callParentFoo(Parent p, int a);

  @JsMethod
  public static native int callChildFun(Child c, int a, int b);

  @JsMethod
  public static native int callChildBar(Child c, int a, int b);

  @JsMethod
  public static native int callChildFoo(Child c, int a);

  @JsMethod
  public static native int callChildIntfFoo(Child c, int a);
}
