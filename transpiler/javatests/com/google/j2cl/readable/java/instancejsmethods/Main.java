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

import jsinterop.annotations.JsMethod;

interface MyInterface {
  int intfFoo();
}

class SuperParent {
  public int fun(int a, int b) {
    return a + b + 111;
  }

  public int bar(int a, int b) {
    return a * b + 222;
  }
}

class Parent extends SuperParent {
  /**
   * JsMethod that exposes non-JsMethod in parent, with renaming.
   *
   * <p>A bridge method m_fun__int__int should be generated.
   */
  @Override
  @JsMethod(name = "sum")
  public int fun(int a, int b) {
    return a + b;
  }

  /**
   * JsMethod that exposes non-JsMethod in parent, without renaming.
   *
   * <p>A bridge method m_bar__int__int should be generated.
   */
  @Override
  @JsMethod
  public int bar(int a, int b) {
    return a * b;
  }

  /**
   * JsMethod that does not expose non-JsMethod in parent.
   *
   * <p>No bridge method should be generated.
   */
  @JsMethod(name = "myFoo")
  public int foo(int a) {
    return a;
  }
}

class Child extends Parent implements MyInterface {
  /**
   * Non-JsMethod that overrides a JsMethod with renaming.
   *
   * <p>It should be generated as sum() (non-mangled name), no bridge method should be generated
   * because the bridge method is already generated in its parent.
   */
  @Override
  public int fun(int a, int b) {
    return a + b + 1;
  }

  /**
   * Non-JsMethod that overrides a JsMethod without renaming.
   *
   * <p>It should be generated as bar() (non-mangled name), no bridge method should be generated
   * because the bridge method is already generated in its parent.
   */
  @Override
  public int bar(int a, int b) {
    return a * b + 1;
  }

  /**
   * JsMethod that overrides a JsMethod.
   *
   * <p>No bridge method should be generated.
   */
  @Override
  @JsMethod(name = "myFoo")
  public int foo(int a) {
    return a;
  }

  /**
   * JsMethod that overrides a non-JsMethod from its interface.
   *
   * <p>A bridge method should be generated.
   */
  @Override
  @JsMethod
  public int intfFoo() {
    return 1;
  }
}

public class Main {
  public void testCallBySuperParent() {
    SuperParent sp = new SuperParent();
    SuperParent p = new Parent();
    SuperParent c = new Child();
    Parent pp = new Parent();
    Parent cc = new Child();
    MyInterface intf = new Child();

    sp.fun(12, 35);
    sp.bar(6, 7);
    p.fun(12, 35);
    p.bar(6, 7);
    c.fun(12, 35);
    c.bar(6, 7);
    pp.foo(1);
    cc.foo(1);
    intf.intfFoo();
  }

  public static void testCallByConcreteType() {
    SuperParent sp = new SuperParent();
    Parent p = new Parent();
    Child c = new Child();

    sp.fun(12, 35);
    sp.bar(6, 7);
    p.fun(12, 35);
    p.bar(6, 7);
    p.foo(1);
    c.fun(12, 35);
    c.bar(6, 7);
    c.foo(1);
    c.intfFoo();
  }
}
