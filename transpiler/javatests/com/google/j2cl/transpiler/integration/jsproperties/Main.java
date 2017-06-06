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
package com.google.j2cl.transpiler.integration.jsproperties;

import jsinterop.annotations.JsMethod;

public class Main {
  public static void main(String... args) {
    Main m = new Main();
    m.testInstanceJsProperty();
    m.testNativeInstanceJsProperty();
    m.testNativeStaticJsProperty();
    m.testStaticJsProperty();
  }

  public void testNativeStaticJsProperty() {
    int pi = (int) NativeFoo.getB();
    assert pi == 3;
  }

  public void testNativeInstanceJsProperty() {
    assert new NativeFoo().getA() != null;
  }

  public void testStaticJsProperty() {
    assert Foo.getF() == 10;
    assert Foo.getA() == 11;
    Foo.setA(10);
    assert Foo.getF() == 12;
    assert Foo.getA() == 13;
    assert Foo.getB() == 15;
    Foo.setB(20);
    assert Foo.getF() == 24;
    assert Foo.getB() == 27;

    // call by JS.
    assert getFooA() == 25;
    setFooA(30);
    assert getFooA() == 33;
    assert getFooB() == 35;
    setFooB(40);
    assert getFooB() == 47;
  }

  public void testInstanceJsProperty() {
    Bar bar = new Bar();
    assert bar.getF() == 10;
    assert bar.getA() == 11;
    bar.setA(10);
    assert bar.getF() == 12;
    assert bar.getA() == 13;
    assert bar.getB() == 15;
    bar.setB(20);
    assert bar.getF() == 24;
    assert bar.getB() == 27;

    // call by JS.
    assert getBarA(bar) == 25;
    setBarA(bar, 30);
    assert getBarA(bar) == 33;
    assert getBarB(bar) == 35;
    setBarB(bar, 40);
    assert getBarB(bar) == 47;
  }

  @JsMethod
  public static native int getFooA();

  @JsMethod
  public static native int getFooB();

  @JsMethod
  public static native int getBarA(Bar bar);

  @JsMethod
  public static native int getBarB(Bar bar);

  @JsMethod
  public static native void setFooA(int x);

  @JsMethod
  public static native void setFooB(int x);

  @JsMethod
  public static native void setBarA(Bar bar, int x);

  @JsMethod
  public static native void setBarB(Bar bar, int x);
}
