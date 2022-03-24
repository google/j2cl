/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package jsinteroptests;

import static com.google.j2cl.integration.testing.Asserts.assertEquals;
import static com.google.j2cl.integration.testing.Asserts.assertFalse;
import static com.google.j2cl.integration.testing.Asserts.assertNotNull;
import static com.google.j2cl.integration.testing.Asserts.assertTrue;
import static com.google.j2cl.integration.testing.Asserts.fail;

import java.io.Serializable;
import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/** Tests JsType with array functionality. */
public class JsTypeArrayTest {
  public static void testAll() {
    testJsTypeArray();
    testJsType3DimArray_castFromNativeWithACall();
    testJsTypeArray_asAField();
    testJsTypeArray_asAParam();
    testJsTypeArray_instanceOf();
    testJsTypeArray_objectArrayInterchangeability();
    testJsTypeArray_returnFromNative();
    testJsTypeArray_returnFromNativeWithACall();
    testObjectArray_castFromNative();
    testObjectArray_instanceOf();
    testJsFunctionArray();
    testNativeArrays();
  }

  /* MAKE SURE EACH TYPE IS ONLY USED ONCE PER TEST CASE */
  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "String")
  private static class SomeJsType {}

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  private static class SomeOtherJsType {}

  private static void testJsTypeArray() {
    Object[] array = new SomeJsType[10];

    array[0] = new SomeJsType();
    array[0] = new SomeOtherJsType();
    array[0] = new Object();

    assertTrue(array instanceof SomeJsType[]);
    assertTrue(array instanceof SomeOtherJsType[]);
    assertTrue(array instanceof Object[]);

    SomeOtherJsType[] other1 = (SomeOtherJsType[]) array;
    SomeOtherJsType[] other2 = (SomeOtherJsType[]) new Object[0];
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  private interface SimpleJsTypeReturnFromNative {}

  private static void testJsTypeArray_returnFromNative() {
    SimpleJsTypeReturnFromNative[] array = returnJsTypeFromNative();
    assertEquals(2, array.length);
    assertNotNull(array[0]);
  }

  @JsMethod
  private static native SimpleJsTypeReturnFromNative[] returnJsTypeFromNative();

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  interface SimpleJsTypeReturnFromNativeWithAMethod {
    @JsProperty
    int getId();
  }

  private static void testJsTypeArray_returnFromNativeWithACall() {
    SimpleJsTypeReturnFromNativeWithAMethod[] array = returnJsTypeWithIdsFromNative();
    assertEquals(2, array[1].getId());
  }

  @JsMethod
  private static native SimpleJsTypeReturnFromNativeWithAMethod[] returnJsTypeWithIdsFromNative();

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  private interface SimpleJsTypeAsAField {}

  @JsType
  private static class SimpleJsTypeAsAFieldHolder {
    public SimpleJsTypeAsAField[] arrayField;
  }

  private static void testJsTypeArray_asAField() {
    SimpleJsTypeAsAFieldHolder holder = new SimpleJsTypeAsAFieldHolder();
    fillArrayField(holder);
    SimpleJsTypeAsAField[] array = holder.arrayField;
    assertEquals(2, array.length);
    assertNotNull(array[0]);
  }

  @JsMethod
  private static native void fillArrayField(SimpleJsTypeAsAFieldHolder holder);

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  private interface SimpleJsTypeAsAParam {}

  @JsType
  private static class SimpleJsTypeAsAParamHolder {
    private SimpleJsTypeAsAParam[] theParam;

    public void setArrayParam(SimpleJsTypeAsAParam[] param) {
      theParam = param;
    }
  }

  private static void testJsTypeArray_asAParam() {
    SimpleJsTypeAsAParamHolder holder = new SimpleJsTypeAsAParamHolder();
    fillArrayParam(holder);
    SimpleJsTypeAsAParam[] array = holder.theParam;
    assertEquals(2, array.length);
    assertNotNull(array[0]);
  }

  @JsMethod
  private static native void fillArrayParam(SimpleJsTypeAsAParamHolder holder);

  @JsType(isNative = true)
  private static class SimpleJsTypeReturnForMultiDimArray {
    @JsProperty
    public native int getId();
  }

  private static void testJsType3DimArray_castFromNativeWithACall() {
    SimpleJsTypeReturnForMultiDimArray[][][] array =
        (SimpleJsTypeReturnForMultiDimArray[][][]) returnJsType3DimFromNative();
    assertEquals(1, array.length);
    assertEquals(2, array[0].length);
    assertEquals(3, array[0][0].length);
    assertEquals(2, array[0][0][1].getId());
  }

  @JsMethod
  private static native Object returnJsType3DimFromNative();

  @JsMethod
  private static native SimpleJsTypeReturnForMultiDimArray getSimpleJsType(int i);

  private static void testObjectArray_castFromNative() {
    SimpleJsTypeReturnForMultiDimArray[] array =
        (SimpleJsTypeReturnForMultiDimArray[]) returnObjectArrayFromNative();
    assertNotNull(array);
    assertEquals(3, array.length);
    assertEquals("1", array[0]);
  }

  private static void testJsTypeArray_objectArrayInterchangeability() {
    Object[] objArray = new Object[1];

    SimpleJsTypeReturnForMultiDimArray[][][] array =
        (SimpleJsTypeReturnForMultiDimArray[][][]) objArray;

    objArray[0] = new Object[2];
    ((Object[]) objArray[0])[0] = new Object[3];
    array[0][0][1] = getSimpleJsType(2);
    assertEquals(1, array.length);
    assertEquals(2, array[0].length);
    assertEquals(3, array[0][0].length);
    assertEquals(2, array[0][0][1].getId());
  }

  private static void testObjectArray_instanceOf() {
    Object array = new Object[0];
    assertTrue(array instanceof Object[]);
    assertFalse(array instanceof Double[]);
    assertFalse(array instanceof int[]);
    assertFalse(array instanceof SimpleJsTypeReturnForMultiDimArray);
    assertTrue(array instanceof SimpleJsTypeReturnForMultiDimArray[]);
    assertTrue(array instanceof SimpleJsTypeReturnForMultiDimArray[][]);
    assertTrue(array instanceof SimpleJsTypeReturnForMultiDimArray[][][]);
    assertTrue(array instanceof Serializable);
  }

  private static void testJsTypeArray_instanceOf() {
    Object array = returnJsType3DimFromNative();
    assertTrue(array instanceof Object[]);
    assertFalse(array instanceof Double[]);
    assertFalse(array instanceof int[]);
    assertFalse(array instanceof SimpleJsTypeReturnForMultiDimArray);
    assertTrue(array instanceof SimpleJsTypeReturnForMultiDimArray[]);
    assertTrue(array instanceof SimpleJsTypeReturnForMultiDimArray[][]);
    assertTrue(array instanceof SimpleJsTypeReturnForMultiDimArray[][][]);
    assertTrue(array instanceof Serializable);
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

    assertHasNoMetadata(new NativeStringInterface[0]);
    assertHasNoMetadata(new NativeStringInterface[0][]);

    NativeTypedefInterface[][] nativeTypeDef2d = new NativeTypedefInterface[10][];
    nativeTypeDef2d[1] = (NativeTypedefInterface[]) new NativeObjectInterface[0];

    NativeWildcardInterface[][] nativeWildcard2d = new NativeWildcardInterface[10][];
    nativeWildcard2d[1] = (NativeWildcardInterface[]) new NativeObjectInterface[0];
  }

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Array")
  private static class NativeArray {}

  private static void assertHasNoMetadata(Object array) {
    assertEquals(nonNumericKeys(new NativeArray()), nonNumericKeys(array));
  }

  @JsFunction
  private interface SomeFunction {
    int m(int i);
  }

  @JsFunction
  private interface SomeOtherFunction {
    int n(Object o, int j);
  }

  private static final class SomeFunctionImplementation implements SomeFunction {
    public int m(int i) {
      return 0;
    }
  }

  private static void testJsFunctionArray() {
    Object[] someFunctionArray = new SomeFunction[10];

    // @JsFunction arrays can contain any type of function.
    someFunctionArray[0] = returnSomeFunction();

    // @JsFunction arrays are interchangeable with each other.
    assertTrue(someFunctionArray instanceof SomeFunction[]);
    assertTrue(someFunctionArray instanceof SomeOtherFunction[]);
    assertFalse(new Object[0] instanceof SomeOtherFunction[]);

    // @JsFunction arrays are subclasses of Object[] which are Serializable.
    assertTrue(someFunctionArray instanceof Serializable);

    // @JsFunction arrays are interchangeable
    SomeOtherFunction[] someOtherFunctionArray = (SomeOtherFunction[]) someFunctionArray;

    someOtherFunctionArray[0] = (a, b) -> 2;
    // @JsFunction arrays accept any @JsFunction implementation.
    someFunctionArray[2] = (SomeOtherFunction) (a, b) -> 2;
    ((Object[]) someOtherFunctionArray)[0] = new SomeFunctionImplementation();

    Object[] someFunctionImplementationArray = new SomeFunctionImplementation[1];

    // @JsFunction arrays variables can reference any function implementation implementation arrays,
    // whether its the declared @JsFunction interface or not.
    SomeFunction[] aSomeFunctionArray = (SomeFunction[]) someFunctionImplementationArray;
    SomeOtherFunction[] aSomeOtherFunctionArray =
        (SomeOtherFunction[]) someFunctionImplementationArray;

    // @JsFunction implementation arrays can only contain particular functions.
    someFunctionImplementationArray[0] = new SomeFunctionImplementation();
    try {
      // SomeFunctionImplementation[] arrays can only contain the specific JsFunction
      // implementation.
      someFunctionImplementationArray[0] = (SomeFunction) (a) -> a + 1;
      fail("Should have thrown");
    } catch (ArrayStoreException expected) {
    }

    try {
      // @JsFunction arrays can only contain functions.
      someFunctionArray[1] = new Object();
      fail("Should have thrown");
    } catch (ArrayStoreException expected) {
    }
  }

  @JsMethod
  private static native Object returnObjectArrayFromNative();

  @JsMethod
  private static native Object returnSomeFunction();

  @JsMethod
  private static native String[] nonNumericKeys(Object object);
}
