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
package staticjsmembers;

import static jsinterop.annotations.JsPackage.GLOBAL;

import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

public class StaticJsMembers {

  @JsProperty(name = "field")
  public static int field1;

  @JsProperty public static int field2;

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  public static class Native {
    @JsProperty(namespace = JsPackage.GLOBAL, name = "Math.PI")
    public static int field3;

    @JsProperty(namespace = GLOBAL, name = "top")
    public static int field4;

    @JsProperty(namespace = "foo.Bar", name = "field")
    public static int field5;

    @JsProperty(namespace = GLOBAL, name = "window.top")
    public static int field6;
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "window.Object")
  public static class Extern {}

  @JsMethod(name = "fun")
  public static void f1(int a) {}

  @JsMethod
  public static void f2(int a) {}

  @JsMethod(namespace = JsPackage.GLOBAL, name = "Math.floor")
  public static native void f3(double a);

  @JsMethod(namespace = GLOBAL, name = "isFinite")
  public static native boolean f4(double a);

  @JsMethod(namespace = "foo.Bar", name = "baz")
  public static native boolean f5();

  @JsMethod(namespace = "foo.Baz", name = "baz")
  public static native boolean f6();

  @JsMethod(namespace = JsPackage.GLOBAL, name = "Math.max")
  public static native int max(int a, int b);

  @JsMethod(namespace = JsPackage.GLOBAL, name = "Math.max")
  public static native int max(int a, int b, int c);

  @JsMethod(namespace = JsPackage.GLOBAL, name = "Math.max")
  public static native double max(double a, double b);

  public void test() {
    StaticJsMembers.f1(1);
    f1(1);
    StaticJsMembers.f2(1);
    f2(1);
    StaticJsMembers.f3(1.1);
    f3(1.1);
    StaticJsMembers.f4(1.1);
    f4(1.1);
    StaticJsMembers.f5();
    f5();
    StaticJsMembers.max(1, 2);
    max(1, 2);
    StaticJsMembers.max(1, 2, 3);
    max(1, 2, 3);
    StaticJsMembers.max(1.0, 2.0);
    max(1.0, 2.0);

    int n = field1;
    n = field2;
    n = Native.field3;
    n = Native.field4;
    n = Native.field5;
    n = Native.field6;

    new Native();
    new Extern();
  }
}
