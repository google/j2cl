/*
 * Copyright 2023 Google Inc.
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
package wasm;

import static com.google.j2cl.integration.testing.Asserts.assertEquals;
import static com.google.j2cl.integration.testing.Asserts.assertFalse;
import static com.google.j2cl.integration.testing.Asserts.assertTrue;

import javaemul.internal.annotations.Wasm;
import jsinterop.annotations.JsEnum;

/**
 * Incrementally tests wasm features as they are being added.
 *
 * <p>This test will be removed when all Wasm features are implemented and all integration tests are
 * enabled for Wasm.
 */
@SuppressWarnings("unusable-by-js")
public class Main {

  public static void main(String... args) throws Exception {
    testWasmAnnotation();
    testArrayInstanceOf();
    testArrayGetClass();
    testNativeArrays();
    testJsEnumUnboxedReturn();
  }

  private static void testWasmAnnotation() {
    assertTrue(42 == multiply(6, 7));
  }

  @Wasm("i32.mul")
  private static native int multiply(int x, int y);

  private interface SomeInterface {}

  private static void testArrayInstanceOf() {
    Object intArray = new int[0];
    assertTrue(intArray instanceof int[]);
    assertFalse(intArray instanceof long[]);
    assertFalse(intArray instanceof Object[]);
    assertFalse(intArray instanceof SomeInterface[]);

    Object multiDimIntArray = new int[0][0];
    assertFalse(multiDimIntArray instanceof int[]);
    assertFalse(multiDimIntArray instanceof long[]);
    assertTrue(multiDimIntArray instanceof Object[]);
    assertTrue(multiDimIntArray instanceof int[][]);
    // TODO(b/184675805): enable when this is fixed.
    // assertFalse(multiDimIntArray instanceof long[][]);
    // assertFalse(multiDimIntArray instanceof SomeInterface[]);

    Object objectArray = new Object[0];
    assertFalse(objectArray instanceof int[]);
    assertFalse(objectArray instanceof long[]);
    assertTrue(objectArray instanceof Object[]);
    // TODO(b/184675805): enable when this is fixed.
    // assertFalse(objectArray instanceof int[][]);
    // assertFalse(objectArray instanceof long[][]);
    // assertFalse(objectArray instanceof SomeInterface[]);

    Object multiDimObjectArray = new Object[0][0];
    assertFalse(multiDimObjectArray instanceof int[]);
    assertFalse(multiDimObjectArray instanceof long[]);
    assertTrue(multiDimObjectArray instanceof Object[]);
    assertTrue(multiDimObjectArray instanceof Object[][]);
    // TODO(b/184675805): enable when this is fixed.
    // assertFalse(multiDimObjectArray instanceof int[][]);
    // assertFalse(multiDimObjectArray instanceof long[][]);
    // assertFalse(multiDimObjectArray instanceof SomeInterface[]);

    Object referencetArray = new SomeInterface[0];
    assertFalse(referencetArray instanceof int[]);
    assertFalse(referencetArray instanceof long[]);
    assertTrue(referencetArray instanceof Object[]);
    assertTrue(referencetArray instanceof SomeInterface[]);
    // TODO(b/184675805): enable when this is fixed.
    // assertFalse(referencetArray instanceof Object[][]);
    // assertFalse(referencetArray instanceof int[][]);
    // assertFalse(referencetArray instanceof long[][]);
  }

  private static void testArrayGetClass() {
    Object intArray = new int[0];
    assertEquals(int[].class, intArray.getClass());
    assertEquals(int.class, intArray.getClass().getComponentType());

    Object objectArray = new Object[0];
    assertEquals(Object[].class, objectArray.getClass());
    assertEquals(Object.class, objectArray.getClass().getComponentType());
  }

  @Wasm("i31")
  interface I31Ref {}

  @Wasm("ref.i31")
  private static native I31Ref toI31Ref(int value);

  @Wasm("i31.get_s")
  private static native int i31GetS(I31Ref value);

  private static void testNativeArrays() {
    I31Ref[] i31Refs = new I31Ref[10];

    I31Ref two = toI31Ref(2);
    i31Refs[1] = two;
    assertTrue(i31GetS(i31Refs[1]) == 2);
    for (int i = 0; i < i31Refs.length; i++) {
      i31Refs[i] = toI31Ref(i);
    }
    for (int i = 0; i < i31Refs.length; i++) {
      assertTrue(i31GetS(i31Refs[i]) == i);
    }
  }

  @JsEnum
  private enum PlainJsEnum {
    ONE,
    TWO,
    THREE
  }

  private static PlainJsEnum returnNullValue() {
    return null;
  }

  private static void testJsEnumUnboxedReturn() {
    // TODO(b/301342159): Remove this test and add a test to java/jsenum when this throws an NPE.
    assertTrue(returnNullValue().ordinal() == Integer.MIN_VALUE);
  }
}
