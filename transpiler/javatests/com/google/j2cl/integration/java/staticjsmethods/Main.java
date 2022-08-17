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
package staticjsmethods;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;
import static jsinterop.annotations.JsPackage.GLOBAL;

import jsinterop.annotations.JsMethod;

public class Main {
  @JsMethod(name = "fun")
  public static int f1(int a) {
    return a + 11;
  }

  @JsMethod
  public static int f2(int a) {
    return a + 22;
  }

  public static void testJsMethodsCalledByJava() {
    assertTrue(f1(1) == 12);
    assertTrue(f2(1) == 23);
  }

  public static void testJsMethodsCalledByJS() {
    assertTrue(callF1(1) == 12);
    assertTrue(callF2(1) == 23);
  }

  public static void testJsMethodsCalledByOtherClass() {
    assertTrue(OtherClass.callF1(1) == 12);
    assertTrue(OtherClass.callF2(1) == 23);
  }

  public static void testNativeJsMethod() {
    assertTrue(floor(1.5) == 1);
    assertTrue(f3(-1) == 1);
    assertTrue(isFinite(1.0));
  }

  public static void testDeepNamespaceNativeJsMethod() {
    assertTrue(fooBarAbs(-1) == 1);
  }

  public static void main(String... args) {
    testJsMethodsCalledByJava();
    testJsMethodsCalledByJS();
    testJsMethodsCalledByOtherClass();
    testNativeJsMethod();
    testDeepNamespaceNativeJsMethod();
  }

  @JsMethod
  public static native int callF1(int a);

  @JsMethod
  public static native int callF2(int a);

  @JsMethod(namespace = GLOBAL, name = "Math.floor")
  public static native int floor(double d);

  @JsMethod(namespace = GLOBAL, name = "Math.abs")
  public static native int f3(int d);

  @JsMethod(namespace = GLOBAL, name = "isFinite")
  public static native boolean isFinite(double d);

  @JsMethod(namespace = "foo.Bar", name = "abs")
  public static native double fooBarAbs(double d);
}
