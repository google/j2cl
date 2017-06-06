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
package com.google.j2cl.transpiler.integration.jsinteroptests;

import jsinterop.annotations.JsConstructor;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

/** Tests JsType object method devirtualization functionality. */
public class JsTypeObjectMethodsTest extends MyTestCase {
  public static void testAl() {
    JsTypeObjectMethodsTest test = new JsTypeObjectMethodsTest();
    test.testEquals();
    test.testHashCode();
    test.testJavaLangObjectMethodsOrNativeSubtypes();
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  interface NativeObject {}

  @JsMethod
  private static native NativeObject createWithEqualsAndHashCode(int a, int b);

  @JsMethod
  private static native NativeObject createWithoutEqualsAndHashCode(int a, int b);

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  static class NativeClassWithHashCode {
    @Override
    public native int hashCode();
  }

  static class SubclassNativeClassWithHashCode extends NativeClassWithHashCode {
    private int n;

    @JsConstructor
    public SubclassNativeClassWithHashCode(int n) {
      this.n = n;
    }

    @Override
    @JsMethod
    public int hashCode() {
      return n;
    }
  }

  static class ImplementsNativeObject implements NativeObject {
    private int n;

    public ImplementsNativeObject(int n) {
      this.n = n;
    }

    @Override
    @JsMethod
    public int hashCode() {
      return n;
    }
  }

  public void testHashCode() {
    assertEquals(3, createWithEqualsAndHashCode(1, 3).hashCode());
    NativeObject o1 = createWithoutEqualsAndHashCode(1, 3);
    NativeObject o2 = createWithoutEqualsAndHashCode(1, 3);
    assertTrue(o1.hashCode() != o2.hashCode());
    assertTrue(((Object) o1).hashCode() != ((Object) o2).hashCode());
    assertEquals(8, new SubclassNativeClassWithHashCode(8).hashCode());
    assertEquals(8, ((Object) new SubclassNativeClassWithHashCode(8)).hashCode());
    assertEquals(9, ((Object) new ImplementsNativeObject(9)).hashCode());
    assertEquals(10, callHashCode(new SubclassNativeClassWithHashCode(10)));
  }

  public void testEquals() {
    assertEquals(createWithEqualsAndHashCode(1, 3), createWithEqualsAndHashCode(1, 4));
    NativeObject o1 = createWithoutEqualsAndHashCode(1, 3);
    NativeObject o2 = createWithoutEqualsAndHashCode(1, 3);
    assertTrue(createWithEqualsAndHashCode(1, 3).equals(createWithoutEqualsAndHashCode(1, 4)));
    assertTrue(
        ((Object) createWithEqualsAndHashCode(1, 3)).equals(createWithoutEqualsAndHashCode(1, 4)));
    assertFalse(createWithoutEqualsAndHashCode(1, 4).equals(createWithEqualsAndHashCode(1, 3)));
    assertFalse(
        ((Object) createWithoutEqualsAndHashCode(1, 4)).equals(createWithEqualsAndHashCode(1, 3)));
    assertFalse(o1.equals(o2));
    assertFalse(((Object) o1).equals(o2));
  }

  @JsMethod
  private static native int callHashCode(Object obj);

  // Use an existing class for native subclassing tests to work around the need of injecting a
  // JS class before the subclass definitions
  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Error")
  private static class MyNativeError {
    MyNativeError() {}

    MyNativeError(String error) {}

    @Override
    public native int hashCode();

    public int myValue;
  }

  private static class MyNativeErrorSubtype extends MyNativeError {
    @JsConstructor
    MyNativeErrorSubtype(int n) {
      myValue = n;
    }

    @Override
    public String toString() {
      return "(Sub)myValue: " + myValue;
    }
  }

  private static MyNativeError createMyNativeError(int n) {
    MyNativeError error = new MyNativeError("(Error)myValue: " + n);
    error.myValue = n;
    return error;
  }

  public void testJavaLangObjectMethodsOrNativeSubtypes() {
    patchErrorWithJavaLangObjectMethods();
    assertEquals(createMyNativeError(3), createMyNativeError(3));
    assertFalse(createMyNativeError(3).equals(createMyNativeError(4)));

    assertEquals(createMyNativeError(6), new MyNativeErrorSubtype(6));
    assertTrue(createMyNativeError(6).toString().contains("(Error)myValue: 6"));
    assertEquals("(Sub)myValue: 6", new MyNativeErrorSubtype(6).toString());

    // Tests that hashcode is actually working through the object trampoline and
    // assumes that consecutive hashchodes are different.
    // TODO(b/31272546): uncomment
    // assertFalse(createMyNativeError(3).hashCode() == new MyNativeError().hashCode());
  }

  @JsMethod
  private static native void patchErrorWithJavaLangObjectMethods();
}
