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

import static com.google.j2cl.integration.testing.Asserts.assertTrue;

import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsProperty;

public class Main {
  public static void main(String... args) {
    Main m = new Main();
    m.testInstanceJsProperty();
    m.testNativeInstanceJsProperty();
    m.testNativeStaticJsProperty();
    m.testStaticJsProperty();
    m.testDefaultMethodJsProperty();
  }

  public void testNativeStaticJsProperty() {
    int pi = (int) NativeFoo.getB();
    assertTrue(pi == 3);
  }

  public void testNativeInstanceJsProperty() {
    assertTrue(new NativeFoo().getA() != null);
  }

  public void testStaticJsProperty() {
    assertTrue(Foo.getF() == 10);
    assertTrue(Foo.getA() == 11);
    Foo.setA(10);
    assertTrue(Foo.getF() == 12);
    assertTrue(Foo.getA() == 13);
    assertTrue(Foo.getB() == 15);
    Foo.setB(20);
    assertTrue(Foo.getF() == 24);
    assertTrue(Foo.getB() == 27);

    // call by JS.
    assertTrue(getFooA() == 25);
    setFooA(30);
    assertTrue(getFooA() == 33);
    assertTrue(getFooB() == 35);
    setFooB(40);
    assertTrue(getFooB() == 47);
  }

  public void testInstanceJsProperty() {
    Bar bar = new Bar();
    assertTrue(bar.getF() == 10);
    assertTrue(bar.getA() == 11);
    bar.setA(10);
    assertTrue(bar.getF() == 12);
    assertTrue(bar.getA() == 13);
    assertTrue(bar.getB() == 15);
    bar.setB(20);
    assertTrue(bar.getF() == 24);
    assertTrue(bar.getB() == 27);

    // call by JS.
    assertTrue(getBarA(bar) == 25);
    setBarA(bar, 30);
    assertTrue(getBarA(bar) == 33);
    assertTrue(getBarB(bar) == 35);
    setBarB(bar, 40);
    assertTrue(getBarB(bar) == 47);
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

  class Implementor implements InterfaceWithDefaultJsProperties {
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

  private void testDefaultMethodJsProperty() {
    InterfaceWithDefaultJsProperties i = new Implementor();
    i.setValue(3);
    assertTrue(3 == i.getValue());
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
