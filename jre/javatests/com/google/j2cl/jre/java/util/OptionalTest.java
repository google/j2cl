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
package com.google.j2cl.jre.java.util;

import static com.google.j2cl.jre.testing.TestUtils.isWasm;
import static org.junit.Assert.assertThrows;

import com.google.j2cl.jre.testing.J2ktIncompatible;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Stream;

/** Tests for Optional JRE emulation. */
public class OptionalTest extends EmulTestBase {

  private static final Object REFERENCE = new Object();
  private static final Object OTHER_REFERENCE = new Object();

  private boolean[] mutableFlag;
  private Optional<Object> empty;
  private Optional<Object> present;
  private Optional<Object> otherPresent;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    mutableFlag = new boolean[1];
    empty = Optional.empty();
    present = Optional.of(REFERENCE);
    otherPresent = Optional.of(OTHER_REFERENCE);
  }

  public void testIsPresent() {
    // empty case
    assertFalse(empty.isPresent());

    empty = Optional.ofNullable(null);
    assertFalse(empty.isPresent());

    // non-empty case
    assertTrue(present.isPresent());

    present = Optional.ofNullable(REFERENCE);
    assertTrue(present.isPresent());
  }

  public void testIsEmpty() {
    // empty case
    assertTrue(empty.isEmpty());

    empty = Optional.ofNullable(null);
    assertTrue(empty.isEmpty());

    // non-empty case
    assertFalse(present.isEmpty());

    present = Optional.ofNullable(REFERENCE);
    assertFalse(present.isEmpty());
  }

  public void testGet() {
    // empty case
    assertThrows(NoSuchElementException.class, () -> empty.get());

    // non-empty case
    assertSame(REFERENCE, present.get());
  }

  @J2ktIncompatible // Parameters are non-nullable according to Jspecify
  @SuppressWarnings("DangerousLiteralNull") // Intentionally misusing Optional to test bug parity.
  public void testNull() {
    if (isWasm()) {
      // TODO(b/183769034): Re-enable when NPE on dereference is supported
      return;
    }

    // empty case
    empty.ifPresent(null); // should not fail as per JavaDoc
    empty.ifPresentOrElse(null, () -> {}); // should not fail as per JavaDoc

    assertThrows(NullPointerException.class, () -> empty.orElseThrow(null));

    // non-empty case
    assertThrows(NullPointerException.class, () -> present.ifPresent(null));

    assertThrows(NullPointerException.class, () -> present.ifPresentOrElse(null, () -> {}));

    Object reference = present.orElseThrow(null);
    assertEquals(REFERENCE, reference);
  }

  public void testIfPresent() {
    // empty case
    empty.ifPresent(wrapped -> fail("Empty Optional should not execute consumer"));

    // non-empty case
    present.ifPresent(
        (wrapped) -> {
          assertSame(REFERENCE, wrapped);
          mutableFlag[0] = true;
        });
    assertTrue("Consumer not executed", mutableFlag[0]);
  }

  public void testIfPresentOrElse() {
    // empty case
    empty.ifPresentOrElse(
        (wrapped) -> fail("Empty Optional should not call non-empty consumer"),
        () -> mutableFlag[0] = true);
    assertTrue("Consumer not executed", mutableFlag[0]);

    // non-empty case
    mutableFlag[0] = false;
    present.ifPresentOrElse(
        (wrapped) -> {
          assertSame(REFERENCE, wrapped);
          mutableFlag[0] = true;
        },
        () -> fail("Non-Empty Optional should not call empty consumer"));
    assertTrue("Consumer not executed", mutableFlag[0]);
  }

  public void testFilter() {
    if (isWasm()) {
      // TODO(b/183769034): Re-enable when NPE on dereference is supported
      return;
    }

    // empty case
    assertThrows(NullPointerException.class, () -> empty.filter(null));

    Optional<Object> filtered = empty.filter(wrapped -> true);
    assertFalse(filtered.isPresent());

    filtered = empty.filter(wrapped -> false);
    assertFalse(filtered.isPresent());

    // non-empty case
    assertThrows(NullPointerException.class, () -> present.filter(null));

    filtered = present.filter(wrapped -> true);
    assertSame(REFERENCE, filtered.get());

    filtered = present.filter(wrapped -> false);
    assertFalse(filtered.isPresent());
  }

  public void testMap() {
    if (isWasm()) {
      // TODO(b/183769034): Re-enable when NPE on dereference is supported
      return;
    }

    // empty case
    assertThrows(NullPointerException.class, () -> empty.map(null));

    empty.map(
        wrapped -> {
          fail("Empty Optional must not execute mapper");
          return "should not execute";
        });

    // non-empty case
    assertThrows(NullPointerException.class, () -> present.map(null));
    Optional<String> mapped = present.map(wrapped -> null);
    assertFalse(mapped.isPresent());

    mapped = present.map(Object::toString);
    assertEquals(REFERENCE.toString(), mapped.get());
  }

  public void testFlatMap() {
    if (isWasm()) {
      // TODO(b/183769034): Re-enable when NPE on dereference is supported
      return;
    }

    // empty case
    assertThrows(NullPointerException.class, () -> empty.flatMap(null));

    empty.flatMap(
        wrapped -> {
          fail("Empty Optional must not execute mapper");
          return Optional.of("should not execute");
        });

    // non-empty case
    assertThrows(NullPointerException.class, () -> present.flatMap(null));

    assertThrows(NullPointerException.class, () -> present.flatMap(wrapped -> null));

    Optional<String> mapped = present.flatMap(wrapped -> Optional.empty());
    assertFalse(mapped.isPresent());

    mapped = present.flatMap(wrapped -> Optional.of(wrapped.toString()));
    assertEquals(REFERENCE.toString(), mapped.get());
  }

  public void testOr() {
    if (isWasm()) {
      // TODO(b/183769034): Re-enable when NPE on dereference is supported
      return;
    }

    assertThrows(NullPointerException.class, () -> empty.or(null));

    assertThrows(NullPointerException.class, () -> present.or(null));

    assertThrows(NullPointerException.class, () -> empty.or(() -> null));

    assertEquals(
        present,
        present.or(
            () -> {
              fail("Present Optional.or must not execute supplier");
              return empty;
            }));

    assertEquals(present, empty.or(() -> present));

    assertEquals(empty, empty.or(() -> empty));

    // Both Optionals present means returning the first Optional.
    assertEquals(present, present.or(() -> otherPresent));
  }

  public void testOrElse() {
    // empty case
    assertSame(OTHER_REFERENCE, empty.orElse(OTHER_REFERENCE));

    // non-empty case
    assertSame(REFERENCE, present.orElse(OTHER_REFERENCE));
  }

  @SuppressWarnings("DangerousLiteralNull") // Intentionally misusing Optional to test bug parity.
  public void testOrElseGet() {
    if (isWasm()) {
      // TODO(b/183769034): Re-enable when NPE on dereference is supported
      return;
    }

    // empty case
    assertThrows(NullPointerException.class, () -> empty.orElseGet(null));

    assertSame(OTHER_REFERENCE, empty.orElseGet(() -> OTHER_REFERENCE));

    // non-empty case
    assertSame(
        REFERENCE,
        present.orElseGet(
            () -> {
              fail("Optional must not execute supplier");
              return OTHER_REFERENCE;
            }));
  }

  @SuppressWarnings("DangerousLiteralNull") // Intentionally misusing Optional to test bug parity.
  public void testOrElseThrow() {
    if (isWasm()) {
      // TODO(b/183769034): Re-enable when NPE on dereference is supported
      return;
    }

    // empty case
    assertThrows(NullPointerException.class, () -> empty.<RuntimeException>orElseThrow(() -> null));

    assertThrows(IllegalStateException.class, () -> empty.orElseThrow(IllegalStateException::new));

    // non-empty case
    assertSame(
        REFERENCE,
        present.orElseThrow(
            () -> {
              fail("Optional must not execute supplier");
              return new RuntimeException("should not execute");
            }));
  }

  public void testOrElseThrowNoArgs() {
    assertThrows(NoSuchElementException.class, () -> empty.orElseThrow());

    assertEquals(REFERENCE, present.orElseThrow());
  }

  public void testEquals() {
    // empty case
    assertFalse(empty.equals(null));
    assertFalse(empty.equals("should not be equal"));
    assertFalse(empty.equals(present));
    assertTrue(empty.equals(empty));
    assertTrue(empty.equals(Optional.empty()));

    // non empty case
    assertFalse(present.equals(null));
    assertFalse(present.equals("should not be equal"));
    assertFalse(present.equals(empty));
    assertFalse(present.equals(Optional.of(OTHER_REFERENCE)));
    assertTrue(present.equals(present));
    assertTrue(present.equals(Optional.of(REFERENCE)));
  }

  public void testHashcode() {
    // empty case
    assertEquals(0, empty.hashCode());

    // non empty case
    assertEquals(REFERENCE.hashCode(), present.hashCode());
  }

  public void testStream() {
    assertEquals(0, Optional.empty().stream().count());
    assertEquals(1, Optional.of("foo").stream().count());

    assertEquals(
        new String[] {"a", "b", "c"},
        Stream.of(Optional.of("a"), empty, Optional.of("b"), Optional.of("c"))
            .flatMap(Optional::stream)
            .toArray(String[]::new));
  }
}
