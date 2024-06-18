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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/** Tests for java.util.Set Java 9 API emulation. */
@SuppressWarnings("JdkImmutableCollections")
public class SetTest extends EmulTestBase {

  public void testOf() {
    assertIsImmutableSetOf(Set.of());
    assertIsImmutableSetOf(Set.of("a"), "a");
    assertIsImmutableSetOf(Set.of("a", "b"), "a", "b");
    assertIsImmutableSetOf(Set.of("a", "b", "c"), "a", "b", "c");
    assertIsImmutableSetOf(Set.of("a", "b", "c", "d"), "a", "b", "c", "d");
    assertIsImmutableSetOf(Set.of("a", "b", "c", "d", "e"), "a", "b", "c", "d", "e");
    assertIsImmutableSetOf(Set.of("a", "b", "c", "d", "e", "f"), "a", "b", "c", "d", "e", "f");
    assertIsImmutableSetOf(
        Set.of("a", "b", "c", "d", "e", "f", "g"), "a", "b", "c", "d", "e", "f", "g");
    assertIsImmutableSetOf(
        Set.of("a", "b", "c", "d", "e", "f", "g", "h"), "a", "b", "c", "d", "e", "f", "g", "h");
    assertIsImmutableSetOf(
        Set.of("a", "b", "c", "d", "e", "f", "g", "h", "i"),
        "a",
        "b",
        "c",
        "d",
        "e",
        "f",
        "g",
        "h",
        "i");
    assertIsImmutableSetOf(
        Set.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "j"),
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
    assertIsImmutableSetOf(
        Set.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k"),
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

    // ensure NPE if any element is null
    assertThrowsNullPointerException(() -> Set.of((String) null));
    assertThrowsNullPointerException(() -> Set.of("a", null));
    assertThrowsNullPointerException(() -> Set.of("a", "b", null));
    assertThrowsNullPointerException(() -> Set.of("a", "b", "c", null));
    assertThrowsNullPointerException(() -> Set.of("a", "b", "c", "d", null));
    assertThrowsNullPointerException(() -> Set.of("a", "b", "c", "d", "e", null));
    assertThrowsNullPointerException(() -> Set.of("a", "b", "c", "d", "e", "f", null));
    assertThrowsNullPointerException(() -> Set.of("a", "b", "c", "d", "e", "f", "g", null));
    assertThrowsNullPointerException(() -> Set.of("a", "b", "c", "d", "e", "f", "g", "h", null));
    assertThrowsNullPointerException(
        () -> Set.of("a", "b", "c", "d", "e", "f", "g", "h", "i", null));
    assertThrowsNullPointerException(
        () -> Set.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", null));

    // ensure IAE if any element is duplicated
    assertThrows(IllegalArgumentException.class, () -> Set.of("a", "a"));
    assertThrows(IllegalArgumentException.class, () -> Set.of("a", "b", "a"));
    assertThrows(IllegalArgumentException.class, () -> Set.of("a", "b", "c", "a"));
    assertThrows(IllegalArgumentException.class, () -> Set.of("a", "b", "c", "d", "a"));
    assertThrows(IllegalArgumentException.class, () -> Set.of("a", "b", "c", "d", "e", "a"));
    assertThrows(IllegalArgumentException.class, () -> Set.of("a", "b", "c", "d", "e", "f", "a"));
    assertThrows(
        IllegalArgumentException.class, () -> Set.of("a", "b", "c", "d", "e", "f", "g", "a"));
    assertThrows(
        IllegalArgumentException.class, () -> Set.of("a", "b", "c", "d", "e", "f", "g", "h", "a"));
    assertThrows(
        IllegalArgumentException.class,
        () -> Set.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "a"));
    assertThrows(
        IllegalArgumentException.class,
        () -> Set.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "a"));
  }

  public void testCopyOf() {
    assertIsImmutableSetOf(Set.copyOf(Set.of("a", "b")), "a", "b");
    assertIsImmutableSetOf(Set.copyOf(Arrays.asList("a", "b")), "a", "b");

    HashSet<String> hashSet = new HashSet<>();
    hashSet.add("a");
    hashSet.add("b");
    Set<String> copy = Set.copyOf(hashSet);
    assertIsImmutableSetOf(copy, "a", "b");

    // verify that mutating the original has no effect on the copy
    hashSet.add("c");
    assertEquals(2, copy.size());
    assertFalse(copy.contains("c"));

    hashSet.remove("a");
    assertEquals(2, copy.size());
    assertTrue(copy.contains("a"));

    // ensure that null value result in a NPE
    try {
      Set.copyOf(Arrays.asList("a", null));
      fail("Expected NullPointerException from null item in collection passed to copyOf");
    } catch (NullPointerException ignored) {
      // expected
    }

    // ensure that duplicate values are ignored.
    assertIsImmutableSetOf(Set.copyOf(Arrays.asList("a", "a")), "a");
  }

  private static void assertIsImmutableSetOf(Set<String> set, String... contents) {
    assertEquals(contents.length, set.size());
    for (int i = 0; i < contents.length; i++) {
      assertTrue(set.contains(contents[i]));
      assertFalse(set.contains(contents[i] + "nope"));
    }

    // quick test that the set impl is sane, aside from the above
    if (contents.length == 0) {
      assertFalse(set.iterator().hasNext());
    } else {
      Iterator<String> itr = set.iterator();
      assertTrue(itr.hasNext());

      assertContains(contents, itr.next());

      assertEquals(contents.length > 1, itr.hasNext());
    }

    assertThrows(UnsupportedOperationException.class, () -> set.add("another item"));
    assertThrows(UnsupportedOperationException.class, () -> set.remove("not present"));
    assertThrows(UnsupportedOperationException.class, () -> set.clear());

    // if any, remove an item actually in the list
    // Without any items, remove(T) defaults to iterating items present.
    if (contents.length > 1) {
      assertThrows(UnsupportedOperationException.class, () -> set.remove(contents[0]));
    }
  }

  private static void assertContains(String[] contents, String value) {
    for (String item : contents) {
      if (item.equals(value)) {
        return;
      }
    }
    fail("Failed to find '" + value + "' in " + Arrays.toString(contents));
  }
}
