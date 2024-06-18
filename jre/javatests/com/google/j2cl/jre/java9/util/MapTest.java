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
package com.google.j2cl.jre.java9.util;

import static org.junit.Assert.assertThrows;

import com.google.j2cl.jre.java.util.EmulTestBase;
import java.util.HashMap;
import java.util.Map;

/** Tests for java.util.Map Java 9 API emulation. */
@SuppressWarnings("JdkImmutableCollections")
public class MapTest extends EmulTestBase {

  public void testOf() {
    assertIsImmutableMapOf(Map.of());
    assertIsImmutableMapOf(Map.of("a", 1), "a");
    assertIsImmutableMapOf(Map.of("a", 1, "b", 2), "a", "b");
    assertIsImmutableMapOf(Map.of("a", 1, "b", 2, "c", 3), "a", "b", "c");
    assertIsImmutableMapOf(Map.of("a", 1, "b", 2, "c", 3, "d", 4), "a", "b", "c", "d");
    assertIsImmutableMapOf(Map.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5), "a", "b", "c", "d", "e");
    assertIsImmutableMapOf(
        Map.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6), "a", "b", "c", "d", "e", "f");
    assertIsImmutableMapOf(
        Map.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7),
        "a",
        "b",
        "c",
        "d",
        "e",
        "f",
        "g");
    assertIsImmutableMapOf(
        Map.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7, "h", 8),
        "a",
        "b",
        "c",
        "d",
        "e",
        "f",
        "g",
        "h");
    assertIsImmutableMapOf(
        Map.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7, "h", 8, "i", 9),
        "a",
        "b",
        "c",
        "d",
        "e",
        "f",
        "g",
        "h",
        "i");
    assertIsImmutableMapOf(
        Map.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7, "h", 8, "i", 9, "j", 10),
        "a",
        "b",
        "c",
        "d",
        "e",
        "f",
        "g",
        "h",
        "i",
        "j");

    // ensure NullPointerException if either key or value are null for any param
    assertThrowsNullPointerException(() -> Map.of(null, 1));
    assertThrowsNullPointerException(() -> Map.of("a", null));
    assertThrowsNullPointerException(() -> Map.of("a", 1, null, 2));
    assertThrowsNullPointerException(() -> Map.of("a", 1, "b", null));
    assertThrowsNullPointerException(() -> Map.of("a", 1, "b", 2, null, 3));
    assertThrowsNullPointerException(() -> Map.of("a", 1, "b", 2, "c", null));
    assertThrowsNullPointerException(() -> Map.of("a", 1, "b", 2, "c", 3, null, 4));
    assertThrowsNullPointerException(() -> Map.of("a", 1, "b", 2, "c", 3, "d", null));
    assertThrowsNullPointerException(() -> Map.of("a", 1, "b", 2, "c", 3, "d", 4, null, 5));
    assertThrowsNullPointerException(() -> Map.of("a", 1, "b", 2, "c", 3, "d", 4, "e", null));
    assertThrowsNullPointerException(() -> Map.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, null, 6));
    assertThrowsNullPointerException(
        () -> Map.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", null));
    assertThrowsNullPointerException(
        () -> Map.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, null, 7));
    assertThrowsNullPointerException(
        () -> Map.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", null));
    assertThrowsNullPointerException(
        () -> Map.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7, null, 8));
    assertThrowsNullPointerException(
        () -> Map.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7, "h", null));
    assertThrowsNullPointerException(
        () -> Map.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7, "h", 8, null, 9));
    assertThrowsNullPointerException(
        () -> Map.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7, "h", 8, "i", null));
    assertThrowsNullPointerException(
        () ->
            Map.of(
                "a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7, "h", 8, "i", 9, null, 10));
    assertThrowsNullPointerException(
        () ->
            Map.of(
                "a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7, "h", 8, "i", 9, "j", null));

    // ensure IllegalArgumentException if any key is repeated
    assertThrows(IllegalArgumentException.class, () -> Map.of("a", 1, "a", 2));
    assertThrows(IllegalArgumentException.class, () -> Map.of("a", 1, "b", 2, "a", 3));
    assertThrows(IllegalArgumentException.class, () -> Map.of("a", 1, "b", 2, "c", 3, "a", 4));
    assertThrows(
        IllegalArgumentException.class, () -> Map.of("a", 1, "b", 2, "c", 3, "d", 4, "a", 5));
    assertThrows(
        IllegalArgumentException.class,
        () -> Map.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "a", 6));
    assertThrows(
        IllegalArgumentException.class,
        () -> Map.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "a", 7));
    assertThrows(
        IllegalArgumentException.class,
        () -> Map.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7, "a", 8));
    assertThrows(
        IllegalArgumentException.class,
        () -> Map.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7, "h", 8, "a", 9));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            Map.of(
                "a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7, "h", 8, "i", 9, "a", 10));
  }

  public void testCopyOf() {
    assertIsImmutableMapOf(Map.copyOf(Map.of("a", 1)), "a");

    HashMap<String, Integer> hashMap = new HashMap<>();
    hashMap.put("a", 1);
    Map<String, Integer> copy = Map.copyOf(hashMap);
    assertIsImmutableMapOf(copy, "a");

    // verify that mutating the original has no effect on the copy
    hashMap.put("b", 2);
    assertFalse(copy.containsKey("b"));
    assertEquals(1, copy.size());

    hashMap.put("a", 5);
    assertEquals(1, (int) copy.get("a"));

    // ensure that null values result in a NPE
    HashMap<String, Integer> mapWithNullKey = new HashMap<>();
    mapWithNullKey.put(null, 1);
    try {
      Map.copyOf(mapWithNullKey);
      fail("expected NullPointerException from copyOf with a null key");
    } catch (NullPointerException ignored) {
      // expected
    }

    HashMap<String, Integer> mapWithNullValue = new HashMap<>();
    mapWithNullValue.put("key", null);
    try {
      Map.copyOf(mapWithNullValue);
      fail("expected NullPointerException from copyOf with a null value");
    } catch (NullPointerException ignored) {
      // expected
    }
  }

  private static void assertIsImmutableMapOf(Map<String, Integer> map, String... contents) {
    assertEquals(contents.length, map.size());
    for (int i = 0; i < contents.length; i++) {
      assertTrue(map.containsKey(contents[i]));
      assertFalse(map.containsKey(contents[i] + "nope"));
      assertEquals(i + 1, (int) map.get(contents[i]));
    }

    assertThrows(UnsupportedOperationException.class, () -> map.put("another item", 1));
    assertThrows(UnsupportedOperationException.class, () -> map.remove("not found"));
    assertThrows(UnsupportedOperationException.class, () -> map.clear());

    // Without any items, remove(T) defaults to iterating items present.
    if (contents.length > 1) {
      assertThrows(UnsupportedOperationException.class, () -> map.remove(contents[0]));
    }
  }

  public void testEntry() {
    Map.Entry<String, String> entry = Map.entry("a", "b");

    assertEquals("a", entry.getKey());
    assertEquals("b", entry.getValue());

    assertThrows(UnsupportedOperationException.class, () -> entry.setValue("z"));

    assertThrowsNullPointerException(() -> Map.entry(null, "value"));
    assertThrowsNullPointerException(() -> Map.entry("key", null));
  }

  @SuppressWarnings("DuplicateMapKeys")
  public void testOfEntries() {
    Map<String, Integer> map = Map.ofEntries(Map.entry("a", 1), Map.entry("b", 2));

    assertIsImmutableMapOf(map, "a", "b");

    // ensure NullPointerException if any entry is null, if any key is null, or value is null
    assertThrowsNullPointerException(() -> Map.ofEntries(Map.entry("a", "b"), null));
    assertThrowsNullPointerException(
        () -> Map.ofEntries(Map.entry("a", "b"), Map.entry("c", null)));
    assertThrowsNullPointerException(
        () -> Map.ofEntries(Map.entry("a", "b"), Map.entry(null, "d")));

    // ensure IllegalArgumentException if any pair has the same key (same or different value)
    assertThrows(
        IllegalArgumentException.class,
        () -> Map.ofEntries(Map.entry("a", "b"), Map.entry("a", "b")));
    assertThrows(
        IllegalArgumentException.class,
        () -> Map.ofEntries(Map.entry("a", "b"), Map.entry("a", "c")));
  }
}
