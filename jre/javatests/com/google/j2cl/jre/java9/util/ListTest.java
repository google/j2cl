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
import java.util.Iterator;
import java.util.List;

/** Tests for java.util.List Java 9 API emulation. */
@SuppressWarnings("JdkImmutableCollections")
public class ListTest extends EmulTestBase {

  public void testOf() {
    assertIsImmutableListOf(List.of());
    assertIsImmutableListOf(List.of("a"), "a");
    assertIsImmutableListOf(List.of("a", "b"), "a", "b");
    assertIsImmutableListOf(List.of("a", "b", "c"), "a", "b", "c");
    assertIsImmutableListOf(List.of("a", "b", "c", "d"), "a", "b", "c", "d");
    assertIsImmutableListOf(List.of("a", "b", "c", "d", "e"), "a", "b", "c", "d", "e");
    assertIsImmutableListOf(List.of("a", "b", "c", "d", "e", "f"), "a", "b", "c", "d", "e", "f");
    assertIsImmutableListOf(
        List.of("a", "b", "c", "d", "e", "f", "g"), "a", "b", "c", "d", "e", "f", "g");
    assertIsImmutableListOf(
        List.of("a", "b", "c", "d", "e", "f", "g", "h"), "a", "b", "c", "d", "e", "f", "g", "h");
    assertIsImmutableListOf(
        List.of("a", "b", "c", "d", "e", "f", "g", "h", "i"),
        "a",
        "b",
        "c",
        "d",
        "e",
        "f",
        "g",
        "h",
        "i");
    assertIsImmutableListOf(
        List.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "j"),
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
    assertIsImmutableListOf(
        List.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k"),
        "a",
        "b",
        "c",
        "d",
        "e",
        "f",
        "g",
        "h",
        "i",
        "j",
        "k");

    // ensure list is copied
    Object[] elements = new Object[] {"a"};
    List<Object> list = List.of(elements);
    assertEquals("a", list.get(0));
    elements[0] = "b"; // Shouldn't change the list.
    assertEquals("a", list.get(0));

    // ensure that NPE is thrown if a value is null
    assertThrowsNullPointerException(() -> List.of((String) null));
    assertThrowsNullPointerException(() -> List.of("a", null));
    assertThrowsNullPointerException(() -> List.of("a", "b", null));
    assertThrowsNullPointerException(() -> List.of("a", "b", "c", null));
    assertThrowsNullPointerException(() -> List.of("a", "b", "c", "d", null));
    assertThrowsNullPointerException(() -> List.of("a", "b", "c", "d", "e", null));
    assertThrowsNullPointerException(() -> List.of("a", "b", "c", "d", "e", "f", null));
    assertThrowsNullPointerException(() -> List.of("a", "b", "c", "d", "e", "f", "g", null));
    assertThrowsNullPointerException(() -> List.of("a", "b", "c", "d", "e", "f", "g", "h", null));
    assertThrowsNullPointerException(
        () -> List.of("a", "b", "c", "d", "e", "f", "g", "h", "i", null));
    assertThrowsNullPointerException(
        () -> List.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", null));
  }

  private static void assertIsImmutableListOf(List<String> list, String... contents) {
    assertEquals(contents, list);

    // quick test that the list impl is sane
    if (contents.length == 0) {
      assertFalse(list.iterator().hasNext());
    } else {
      Iterator<String> itr = list.iterator();
      assertTrue(itr.hasNext());
      assertEquals(contents[0], itr.next());
      assertEquals(contents.length > 1, itr.hasNext());
    }

    assertThrows(UnsupportedOperationException.class, () -> list.add("another item"));
    assertThrows(UnsupportedOperationException.class, () -> list.remove(0));
    assertThrows(UnsupportedOperationException.class, () -> list.remove("not present"));
    assertThrows(UnsupportedOperationException.class, () -> list.clear());

    // if any, remove an item actually in the list
    // Without any items, remove(T) defaults to iterating items present.
    if (contents.length > 0) {
      assertThrows(UnsupportedOperationException.class, () -> list.remove(contents[0]));
    }
  }
}
