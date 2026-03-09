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

import jsinterop.annotations.JsProperty;

/** Tests for non native static JsProperty. */
class Foo {
  private static int f;

  @JsProperty
  public static int getA() {
    return f + 1;
  }

  @JsProperty
  public static void setA(int x) {
    f = x + 2;
  }

  @JsProperty(name = "abc")
  public static int getB() {
    return f + 3;
  }

  @JsProperty(name = "abc")
  public static void setB(int x) {
    f = x + 4;
  }
}

/** Tests for non native instance JsProperty. */
class Bar {
  private int f;

  @JsProperty private int privateField = -1;

  @JsProperty
  public int getA() {
    return f + 1;
  }

  @JsProperty
  public void setA(int x) {
    f = x + 2;
  }

  @JsProperty(name = "abc")
  public int getB() {
    return f + 3;
  }

  @JsProperty(name = "abc")
  public void setB(int x) {
    f = x + 4;
  }
}

/** Tests for read only JsProperty. */
class ReadOnlyJsProperty {
  public final int a = 1;

  public static final int b = 2;

  @JsProperty
  public int getC() {
    return 3;
  }

  @JsProperty
  public static int getD() {
    return 4;
  }
}

interface InterfaceWithDefaultJsProperties {
  @JsProperty
  default int getM() {
    return 3;
  }

  @JsProperty
  default void setM(int value) {}
}

class ImplementsInterfaceWithDefaultJsProperties implements InterfaceWithDefaultJsProperties {}

public class Main {
  public void testNativeJsProperty() {
    new FooWithNativeProperty().getA();
    FooWithNativeProperty.getB();
  }

  public void testStaticJsProperty() {
    Foo.getA();
    Foo.setA(10);
    Foo.getB();
    Foo.setB(10);
  }

  public void testInstanceJsProperty() {
    Bar bar = new Bar();
    bar.getA();
    bar.setA(10);
    bar.getB();
    bar.setB(10);
  }

  public void testReadOnlyJsProperty() {
    ReadOnlyJsProperty readOnlyJsProperty = new ReadOnlyJsProperty();
    int r;
    r = readOnlyJsProperty.a;
    r = ReadOnlyJsProperty.b;
    readOnlyJsProperty.getC();
    ReadOnlyJsProperty.getD();
  }
}
