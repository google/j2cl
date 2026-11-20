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

import static com.google.j2cl.integration.testing.Asserts.assertEquals;
import static com.google.j2cl.integration.testing.Asserts.assertFalse;
import static com.google.j2cl.integration.testing.Asserts.assertNotNull;
import static com.google.j2cl.integration.testing.Asserts.assertTrue;
import static com.google.j2cl.integration.testing.TestUtils.isWasm;

import java.io.Serializable;
import javaemul.internal.annotations.Wasm;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

public class Main {
  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  static class MyNativeType {
    public MyNativeType() {}
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "String")
  private static class MyOtherNativeType {}

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

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  private interface SimpleJsTypeReturnFromNative {}

  private static void testJsTypeArray_returnFromNative() {
    SimpleJsTypeReturnFromNative[] array = returnJsTypeFromNative();
    assertEquals(2, array.length);
    assertNotNull(array[0]);
  }

  @JsMethod(namespace = "arrayofnativejstype.helper")
  private static native SimpleJsTypeReturnFromNative[] returnJsTypeFromNative();

  @JsType(isNative = true, namespace = "arrayofnativejstype")
  private static class SimpleJsTypeWithProperty {
    @JsProperty
    public native int getId();
  }

  private static void testJsTypeArray_returnFromNativeWithACall() {
    SimpleJsTypeWithProperty[] array = returnJsTypeWithIdsFromNative();
    assertEquals(2, array[1].getId());
  }

  @JsMethod(namespace = "arrayofnativejstype.helper")
  private static native SimpleJsTypeWithProperty[] returnJsTypeWithIdsFromNative();

  private static void testJsType3DimArray_castFromNativeWithACall() {
    SimpleJsTypeWithProperty[][][] array =
        (SimpleJsTypeWithProperty[][][]) returnJsType3DimFromNative();
    assertEquals(1, array.length);
    assertEquals(2, array[0].length);
    assertEquals(3, array[0][0].length);
    assertEquals(2, array[0][0][1].getId());
  }

  @JsMethod(namespace = "arrayofnativejstype.helper")
  private static native Object returnJsType3DimFromNative();

  @JsMethod(namespace = "arrayofnativejstype.helper")
  private static native SimpleJsTypeWithProperty getSimpleJsType(int i);

  // TODO(b/540455028): Native JsType arrays are not assignable to Java Object[] in Wasm.
  @Wasm("nop")
  private static void testObjectArrayAssignment() {
    Object[] array = new MyNativeType[10];

    array[0] = new MyNativeType();
    array[0] = new MyOtherNativeType();
    array[0] = new Object();

    MyOtherNativeType[] other1 = (MyOtherNativeType[]) array;
    MyOtherNativeType[] other2 = (MyOtherNativeType[]) new Object[0];
  }

  private static void testObjectArray_castFromNative() {
    SimpleJsTypeWithProperty[] array = (SimpleJsTypeWithProperty[]) returnObjectArrayFromNative();
    assertNotNull(array);
    assertEquals(3, array.length);
    assertEquals("1", array[0]);
  }

  private static void testJsTypeArray_objectArrayInterchangeability() {
    // In Wasm, Java Object[] and native JsType arrays have different representations and are not
    // interchangeable.
    if (isWasm()) {
      return;
    }
    Object[] objArray = new Object[1];

    SimpleJsTypeWithProperty[][][] array = (SimpleJsTypeWithProperty[][][]) objArray;

    objArray[0] = new Object[2];
    ((Object[]) objArray[0])[0] = new Object[3];
    array[0][0][1] = getSimpleJsType(2);
    assertEquals(1, array.length);
    assertEquals(2, array[0].length);
    assertEquals(3, array[0][0].length);
    assertEquals(2, array[0][0][1].getId());
  }

  // TODO(b/540455028): Cannot do instanceof against native JsType in Wasm.
  @Wasm("nop")
  private static void testInstanceOf() {
    Object array = new MyNativeType[10];

    assertTrue(array instanceof MyNativeType[]);
    assertTrue(array instanceof MyOtherNativeType[]);
    assertTrue(array instanceof Object[]);
  }

  private static void testObjectArray_instanceOf() {
    Object array = new Object[0];
    assertTrue(array instanceof Object[]);
    // TODO(b/184675805): Enable for Wasm when array metadata is fully implemented.
    if (!isWasm()) {
      assertFalse(array instanceof Double[]);
    }
    assertFalse(array instanceof int[]);
    assertTrue(array instanceof Serializable);
  }

  // TODO(b/540455028): Cannot do instanceof against native JsType in Wasm.
  @Wasm("nop")
  private static void testObjectArray_instanceOf_nativeJsType() {
    Object array = new Object[0];
    assertFalse(array instanceof SimpleJsTypeWithProperty);
    assertTrue(array instanceof SimpleJsTypeWithProperty[]);
    assertTrue(array instanceof SimpleJsTypeWithProperty[][]);
    assertTrue(array instanceof SimpleJsTypeWithProperty[][][]);
  }

  private static void testJsTypeArray_instanceOf() {
    Object array = returnJsType3DimFromNative();
    if (!isWasm()) {
      // In Wasm, native JS arrays are not instances of Java Object[] or Serializable.
      assertTrue(array instanceof Object[]);
      assertTrue(array instanceof Serializable);
    }
    assertFalse(array instanceof Double[]);
    assertFalse(array instanceof int[]);
  }

  // TODO(b/540455028): Cannot do instanceof against native JsType in Wasm.
  @Wasm("nop")
  private static void testJsTypeArray_instanceOf_nativeJsType() {
    Object array = returnJsType3DimFromNative();
    assertFalse(array instanceof SimpleJsTypeWithProperty);
    assertTrue(array instanceof SimpleJsTypeWithProperty[]);
    assertTrue(array instanceof SimpleJsTypeWithProperty[][]);
    assertTrue(array instanceof SimpleJsTypeWithProperty[][][]);
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "?")
  private interface NativeWildcardInterface {}

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "String")
  private interface NativeStringInterface {}

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  private interface NativeObjectInterface {}

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "BufferSource")
  private interface NativeTypedefInterface {}

  private static void testNativeArrays() {
    NativeStringInterface[][] nativeArray2d = new NativeStringInterface[10][];
    nativeArray2d[1] = (NativeStringInterface[]) new NativeObjectInterface[0];

    NativeTypedefInterface[][] nativeTypeDef2d = new NativeTypedefInterface[10][];
    nativeTypeDef2d[1] = (NativeTypedefInterface[]) new NativeObjectInterface[0];

    NativeWildcardInterface[][] nativeWildcard2d = new NativeWildcardInterface[10][];
    nativeWildcard2d[1] = (NativeWildcardInterface[]) new NativeObjectInterface[0];
  }

  @Wasm("nop")
  private static void testNativeArrays_noMetadata() {
    assertHasNoMetadata(new NativeStringInterface[0]);
    assertHasNoMetadata(new NativeStringInterface[0][]);
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Array")
  private static class NativeArray {}

  @Wasm("nop")
  private static void assertHasNoMetadata(Object array) {
    assertEquals(nonNumericKeys(new NativeArray()), nonNumericKeys(array));
  }

  @JsMethod(namespace = "arrayofnativejstype.helper")
  private static native Object returnObjectArrayFromNative();

  // Non-native array return type (String[]) on native JsMethod is not supported in Wasm.
  @Wasm("nop")
  @JsMethod(namespace = "arrayofnativejstype.helper")
  private static native String[] nonNumericKeys(Object object);

  public static void main(String... args) {
    testSingleDimension();
    testMultiDimension();
    testArrayLiteral();
    testEquality();
    testJsBoundary();
    testInstanceOf();
    testJsType3DimArray_castFromNativeWithACall();
    testJsTypeArray_instanceOf();
    testJsTypeArray_instanceOf_nativeJsType();
    testJsTypeArray_objectArrayInterchangeability();
    testJsTypeArray_returnFromNative();
    testJsTypeArray_returnFromNativeWithACall();
    testObjectArrayAssignment();
    testObjectArray_castFromNative();
    testObjectArray_instanceOf();
    testObjectArray_instanceOf_nativeJsType();
    testNativeArrays();
    testNativeArrays_noMetadata();
  }
}
