/*
 * Copyright 2026 Google Inc.
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
package arrayofnativejstype;

import static com.google.j2cl.integration.testing.Asserts.assertTrue;

import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

public class Main {
  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  static class MyNativeType {
    public MyNativeType() {}
  }

  private static void testSingleDimension() {
    MyNativeType[] arr = new MyNativeType[10];
    assertTrue(arr.length == 10);

    MyNativeType element = new MyNativeType();
    arr[0] = element;
    assertTrue(arr[0] == element);
  }

  private static void testMultiDimension() {
    MyNativeType[][] arr2d = new MyNativeType[3][2];
    assertTrue(arr2d.length == 3);
    assertTrue(arr2d[0].length == 2);

    MyNativeType element = new MyNativeType();
    arr2d[1][1] = element;
    assertTrue(arr2d[1][1] == element);

    arr2d = new MyNativeType[3][];
    assertTrue(arr2d.length == 3);
    assertTrue(arr2d[0] == null);
  }

  private static void testArrayLiteral() {
    MyNativeType element1 = new MyNativeType();
    MyNativeType element2 = new MyNativeType();
    MyNativeType[] arr = new MyNativeType[] {element1, element2};

    assertTrue(arr.length == 2);
    assertTrue(arr[0] == element1 && arr[1] == element2);
  }

  private static void testEquality() {
    MyNativeType[] arr1 = new MyNativeType[10];
    MyNativeType[] arr2 = new MyNativeType[10];
    assertTrue(arr1 == arr1);
    assertTrue(arr1 != arr2);
    assertTrue(arr1 != null);

    MyNativeType[] undefinedArr = getUndefined();
    assertTrue(undefinedArr == null);
    assertTrue(undefinedArr != arr1);
  }

  @JsProperty(namespace = JsPackage.GLOBAL)
  private static native MyNativeType[] getUndefined();

  // Trick:
  // Object(arr) call is effectively a pass-through without native.js.
  // Also validates that passed instance is actually not a Wasm object.

  @JsMethod(namespace = JsPackage.GLOBAL, name = "Object")
  private static native MyNativeType[] passThrough(MyNativeType[] arr);

  @JsMethod(namespace = JsPackage.GLOBAL, name = "Object")
  private static native MyNativeType[][] passThrough2d(MyNativeType[][] arr);

  @JsMethod(namespace = JsPackage.GLOBAL, name = "Array.isArray")
  private static native boolean isArray(MyNativeType[] arr);

  @JsMethod(namespace = JsPackage.GLOBAL, name = "Array.isArray")
  private static native boolean isArray(MyNativeType[][] arr);

  private static void testJsBoundary() {
    MyNativeType element = new MyNativeType();
    MyNativeType[] arr = new MyNativeType[] {element};
    assertTrue(isArray(arr));

    MyNativeType[] result = passThrough(arr);
    assertTrue(isArray(result));
    assertTrue(result.length == 1);
    assertTrue(result[0] == element);

    MyNativeType[][] arr2d = new MyNativeType[][] {new MyNativeType[] {element}};
    assertTrue(isArray(arr2d));

    MyNativeType[][] result2d = passThrough2d(arr2d);
    assertTrue(isArray(result2d));
    assertTrue(result2d.length == 1 && result2d[0].length == 1);
    assertTrue(result2d[0][0] == element);
  }

  public static void main(String... args) {
    testSingleDimension();
    testMultiDimension();
    testArrayLiteral();
    testEquality();
    testJsBoundary();
  }
}
