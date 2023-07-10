/*
 * Copyright 2021 Google Inc.
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
import static com.google.j2cl.integration.testing.Asserts.assertNull;
import static com.google.j2cl.integration.testing.Asserts.assertTrue;
import static com.google.j2cl.integration.testing.Asserts.fail;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import javaemul.internal.ArrayHelper;
import javaemul.internal.annotations.Wasm;
import jsinterop.annotations.JsMethod;

/**
 * Incrementally tests wasm features as they are being added.
 *
 * <p>This test will be removed when all Wasm features are implemented and all integration tests are
 * enabled for Wasm.
 */
public class Main {

  public static void main(String... args) throws Exception {
    testWasmAnnotation();
    testClassLiterals();
    testArrayInstanceOf();
    testArrayGetClass();
    testWasmArrayApis();
    testArrayList();
    testHashMap();
    testLinkedHashMap();
    testEnumMap_get_put();
    testEnumMap_remove();
    testEnumMap_containsKey();
  }

  private static void testWasmAnnotation() {
    assertTrue(42 == multiply(6, 7));
  }

  @JsMethod // Exist to keep to test running under closure output
  @Wasm("i32.mul")
  private static native int multiply(int x, int y);

  private static class SomeClass {}

  private interface SomeInterface {}

  private enum SomeEnum {
    A
  }

  private static void testClassLiterals() {
    assertEquals("wasm.Main$SomeClass", SomeClass.class.getName());
    assertEquals(Object.class, SomeClass.class.getSuperclass());
    assertFalse(SomeClass.class.isEnum());
    assertFalse(SomeClass.class.isInterface());
    assertFalse(SomeClass.class.isArray());
    assertFalse(SomeClass.class.isPrimitive());
    assertEquals(
        "wasm.Main$SomeInterface", SomeInterface.class.getName());
    assertEquals(null, SomeInterface.class.getSuperclass());
    assertFalse(SomeInterface.class.isEnum());
    assertTrue(SomeInterface.class.isInterface());
    assertFalse(SomeInterface.class.isArray());
    assertFalse(SomeInterface.class.isPrimitive());
    assertEquals("wasm.Main$SomeEnum", SomeEnum.class.getName());
    assertEquals(Enum.class, SomeEnum.class.getSuperclass());
    assertTrue(SomeEnum.class.isEnum());
    assertFalse(SomeEnum.class.isInterface());
    assertFalse(SomeEnum.class.isArray());
    assertFalse(SomeEnum.class.isPrimitive());
    assertEquals("int", int.class.getName());
    assertEquals(null, int.class.getSuperclass());
    assertFalse(int.class.isEnum());
    assertFalse(int.class.isInterface());
    assertFalse(int.class.isArray());
    assertTrue(int.class.isPrimitive());
    assertEquals(int.class, int[].class.getComponentType());
    assertFalse(int[].class.isEnum());
    assertFalse(int[].class.isInterface());
    assertTrue(int[].class.isArray());
    assertFalse(int[].class.isPrimitive());
  }

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

  private static void testWasmArrayApis() {
    Object[] array = new Object[0];
    ArrayHelper.push(array, "c");
    ArrayHelper.push(array, "d");
    assertEquals(array, new Object[] {"c", "d"});

    ArrayHelper.insertTo(array, 0, "a");
    assertEquals(array, new Object[] {"a", "c", "d"});
    ArrayHelper.insertTo(array, 1, "b");
    assertEquals(array, new Object[] {"a", "b", "c", "d"});

    ArrayHelper.removeFrom(array, 3, 1);
    ArrayHelper.removeFrom(array, 0, 1);
    assertEquals(array, new Object[] {"b", "c"});

    ArrayHelper.setLength(array, 5);
    assertEquals(array, new Object[] {"b", "c", null, null, null});
    ArrayHelper.setLength(array, 1);
    assertEquals(array, new Object[] {"b"});

    Object[] clone = ArrayHelper.clone(new Object[] {"a", "b"}, 0, 4);
    assertEquals(clone, new Object[] {"a", "b", null, null});
  }

  private static void testArrayList() {
    ArrayList<String> list = new ArrayList<>();
    list.add("a");
    list.add("b");
    list.add("d");
    list.add("e");
    list.add(2, "c");
    assertEquals(list.toArray(), new Object[] {"a", "b", "c", "d", "e"});

    list.remove(4);
    list.remove(0);
    assertEquals(list.toArray(), new Object[] {"b", "c", "d"});

    list.clear();
    assertEquals(list.toArray(), new Object[] {});
    list.add("z");
    assertEquals(list.toArray(), new Object[] {"z"});
  }

  private static void testHashMap() {
    HashMap<String, String> map = new HashMap<>();
    map.put("a", "A");
    map.put("b", "B");
    map.put("c", "C");

    String value =
        map.computeIfAbsent(
            "a",
            k -> {
              fail();
              return null;
            });
    assertEquals("A", value);
    assertTrue(map.containsKey("a"));
    assertEquals("A", map.get("a"));

    map.remove("a");
    value = map.computeIfAbsent("a", String::toUpperCase);
    assertEquals("A", value);
    assertTrue(map.containsKey("a"));
    assertEquals("A", map.get("a"));

    map.remove("a");
    value = map.computeIfAbsent("a", k -> null);
    assertNull(value);
    assertFalse(map.containsKey("a"));
  }

  private static void testLinkedHashMap() {
    LinkedHashMap<String, String> map = new LinkedHashMap<>();
    map.put("a", "1");
    map.put("b", "2");
    assertEquals(2, map.keySet().size());
    assertTrue(map.keySet().contains("a"));
    assertTrue(map.keySet().contains("b"));

    map.remove("a");
    assertEquals(1, map.keySet().size());
    assertTrue(map.keySet().contains("b"));
  }

  enum MyEnum {
    A,
    B
  }

  private static void testEnumMap_get_put() {
    EnumMap<MyEnum, Integer> enumMap = new EnumMap<>(MyEnum.class);
    assertEquals(0, enumMap.size());

    enumMap.put(MyEnum.B, 5);
    assertEquals(1, enumMap.size());
    assertEquals(null, enumMap.get(MyEnum.A));
    assertEquals(Integer.valueOf(5), enumMap.get(MyEnum.B));

    enumMap.put(MyEnum.B, 4);
    assertEquals(1, enumMap.size());
    assertEquals(null, enumMap.get(MyEnum.A));
    assertEquals(Integer.valueOf(4), enumMap.get(MyEnum.B));

    enumMap.put(MyEnum.A, 3);
    assertEquals(2, enumMap.size());
    assertEquals(Integer.valueOf(3), enumMap.get(MyEnum.A));
    assertEquals(Integer.valueOf(4), enumMap.get(MyEnum.B));

    enumMap.put(MyEnum.A, 7);
    assertEquals(2, enumMap.size());
    assertEquals(Integer.valueOf(7), enumMap.get(MyEnum.A));
    assertEquals(Integer.valueOf(4), enumMap.get(MyEnum.B));
  }

  private static void testEnumMap_remove() {
    EnumMap<MyEnum, Integer> enumMap = new EnumMap<>(MyEnum.class);
    assertEquals(null, enumMap.remove(MyEnum.A));
    assertEquals(null, enumMap.remove(MyEnum.B));

    enumMap.put(MyEnum.A, 3);
    enumMap.put(MyEnum.B, 7);
    assertEquals(Integer.valueOf(3), enumMap.remove(MyEnum.A));
    assertEquals(null, enumMap.remove(MyEnum.A));
    assertEquals(Integer.valueOf(7), enumMap.remove(MyEnum.B));
    assertEquals(null, enumMap.remove(MyEnum.B));
  }

  private static void testEnumMap_containsKey() {
    EnumMap<MyEnum, Integer> enumMap = new EnumMap<>(MyEnum.class);
    assertFalse(enumMap.containsKey(MyEnum.A));
    assertFalse(enumMap.containsKey(MyEnum.B));

    enumMap.put(MyEnum.A, 3);
    assertTrue(enumMap.containsKey(MyEnum.A));
    assertFalse(enumMap.containsKey(MyEnum.B));

    enumMap.put(MyEnum.B, 3);
    assertTrue(enumMap.containsKey(MyEnum.A));
    assertTrue(enumMap.containsKey(MyEnum.B));
  }
}
