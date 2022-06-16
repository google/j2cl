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
package jsinteroptests;

import static com.google.j2cl.integration.testing.Asserts.assertEquals;
import static com.google.j2cl.integration.testing.Asserts.assertFalse;
import static com.google.j2cl.integration.testing.Asserts.assertNotNull;
import static com.google.j2cl.integration.testing.Asserts.assertTrue;

import jsinterop.annotations.JsConstructor;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsNonNull;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

/** Tests JsType object method devirtualization functionality. */
public class JsTypeObjectMethodsTest {
  public static void testAll() {
    testEquals();
    testHashCode();
    testJavaLangObjectMethodsOrNativeSubtypes();
    testObjectMethodDispatch();
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  static class NativeObjectClass {}

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  interface NativeObjectInterface {}

  @JsType(isNative = true)
  interface NativeInterface {}

  @JsMethod
  private static native NativeObjectInterface createWithEqualsAndHashCode(int a, int b);

  @JsMethod
  private static native NativeObjectInterface createWithoutEqualsAndHashCode(int a, int b);

  @JsType(isNative = true)
  static class NativeClassWithHashCode {
    NativeClassWithHashCode() {}

    NativeClassWithHashCode(int n) {}

    @Override
    public native int hashCode();

    public int myValue;
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

  static class ImplementsNativeInterface implements NativeInterface {
    private int n;

    public ImplementsNativeInterface(int n) {
      this.n = n;
    }

    @Override
    @JsMethod
    public int hashCode() {
      return n;
    }
  }

  private static void testHashCode() {
    assertEquals(3, createWithEqualsAndHashCode(1, 3).hashCode());
    NativeObjectInterface o1 = createWithoutEqualsAndHashCode(1, 3);
    NativeObjectInterface o2 = createWithoutEqualsAndHashCode(1, 3);
    assertTrue(o1.hashCode() != o2.hashCode());
    assertTrue(((Object) o1).hashCode() != ((Object) o2).hashCode());
    assertEquals(8, new SubclassNativeClassWithHashCode(8).hashCode());
    assertEquals(8, ((Object) new SubclassNativeClassWithHashCode(8)).hashCode());
    assertEquals(9, ((Object) new ImplementsNativeInterface(9)).hashCode());
    assertEquals(10, callHashCode(new SubclassNativeClassWithHashCode(10)));
  }

  private static void testEquals() {
    assertEquals(createWithEqualsAndHashCode(1, 3), createWithEqualsAndHashCode(1, 4));
    NativeObjectInterface o1 = createWithoutEqualsAndHashCode(1, 3);
    NativeObjectInterface o2 = createWithoutEqualsAndHashCode(1, 3);
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

  private static class SubtypeOfNativeClass extends NativeClassWithHashCode {
    @JsConstructor
    SubtypeOfNativeClass(int n) {
      myValue = n;
    }

    @Override
    public String toString() {
      return "(Sub)myValue: " + myValue;
    }
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "?")
  interface GenericObjectInterfaceWithObjectMethods {
    boolean equals(Object other);

    int hashCode();

    @JsNonNull
    String toString();
  }

  private static void testJavaLangObjectMethodsOrNativeSubtypes() {
    assertTrue(new NativeClassWithHashCode(3).equals(new NativeClassWithHashCode(3)));
    assertFalse(new NativeClassWithHashCode(3).equals(new NativeClassWithHashCode(4)));

    assertTrue(new NativeClassWithHashCode(6).equals(new SubtypeOfNativeClass(6)));
    assertTrue(new NativeClassWithHashCode(6).toString().contains("myValue: 6"));
    assertEquals("(Sub)myValue: 6", new SubtypeOfNativeClass(6).toString());

    // Tests that hashcode is actually working through the object trampoline and
    // assumes that consecutive hashchodes are different.
    // TODO(b/31272546): uncomment when bug is fixed
    // assertFalse(createMyNativeError(3).hashCode() == new MyNativeError().hashCode());

    GenericObjectInterfaceWithObjectMethods o =
        (GenericObjectInterfaceWithObjectMethods) new NativeObjectClass();
    assertNotNull(o.toString());
    // TODO(b/31272546): uncomment when bug is fixed
    // assertNotNull(((Double) (double) o.hashCode()));
    // Do not change to assertEquals because we are testing the dispatch to equals().
    // assertTrue(o.equals(o));
  }

  @JsType(isNative = true, name = "NativeJsTypeImplementsObjectMethods")
  static class NativeClassWithDeclarations {

    NativeClassWithDeclarations(Double number) {}

    public native boolean equals(Object other);

    public native int hashCode();

    @JsNonNull
    public native String toString();
  }

  @JsType(isNative = true, name = "NativeJsTypeImplementsObjectMethods")
  static class NativeClassWithoutDeclarations {
    NativeClassWithoutDeclarations(Double number) {}
  }

  @JsType(isNative = true, name = "NativeJsTypeImplementsObjectMethods")
  interface NativeInterfaceWithDeclarations {
    boolean equals(Object other);

    int hashCode();

    @JsNonNull
    String toString();
  }

  @JsType(isNative = true, name = "NativeJsTypeImplementsObjectMethods")
  interface NativeInterfaceWithoutDeclarations {}

  private static void testObjectMethodDispatch() {
    NativeClassWithDeclarations nativeClassWithDeclarations = new NativeClassWithDeclarations(5.0);
    NativeClassWithoutDeclarations nativeClassWithoutDeclarations =
        new NativeClassWithoutDeclarations(5.0);

    // Do not change to assertEquals because we are testing the dispatch to equals().
    assertTrue(nativeClassWithDeclarations.equals(nativeClassWithoutDeclarations));
    assertTrue(nativeClassWithoutDeclarations.equals(nativeClassWithDeclarations));

    assertEquals(nativeClassWithDeclarations.toString(), nativeClassWithoutDeclarations.toString());
    assertTrue(nativeClassWithDeclarations.toString().contains("Native Object with value"));
    assertEquals(nativeClassWithDeclarations.hashCode(), nativeClassWithoutDeclarations.hashCode());

    NativeInterfaceWithDeclarations nativeInterfaceWithDeclarations =
        (NativeInterfaceWithDeclarations) nativeClassWithDeclarations;
    NativeInterfaceWithoutDeclarations nativeInterfaceWithoutDeclarations =
        (NativeInterfaceWithoutDeclarations) nativeClassWithoutDeclarations;

    // Do not change to assertEquals because we are testing the dispatch to equals().
    assertTrue(nativeInterfaceWithDeclarations.equals(nativeInterfaceWithoutDeclarations));
    assertTrue(nativeInterfaceWithoutDeclarations.equals(nativeInterfaceWithDeclarations));

    assertEquals(
        nativeInterfaceWithDeclarations.toString(), nativeInterfaceWithoutDeclarations.toString());
    assertTrue(nativeInterfaceWithDeclarations.toString().contains("Native Object with value"));
    assertEquals(
        nativeInterfaceWithDeclarations.hashCode(), nativeInterfaceWithoutDeclarations.hashCode());
  }
}
