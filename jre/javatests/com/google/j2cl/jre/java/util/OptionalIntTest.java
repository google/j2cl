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
import java.util.OptionalInt;
import java.util.stream.Stream;

/** Tests for OptionalInt JRE emulation. */
public class OptionalIntTest extends EmulTestBase {

  private static final int REFERENCE = 10;
  private static final int OTHER_REFERENCE = 20;
  private boolean[] mutableFlag;
  private OptionalInt empty;
  private OptionalInt present;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    mutableFlag = new boolean[1];
    empty = OptionalInt.empty();
    present = OptionalInt.of(REFERENCE);
  }

  public void testIsPresent() {
    // empty case
    assertFalse(empty.isPresent());

    // non-empty case
    assertTrue(present.isPresent());
  }

  public void testIsEmpty() {
    assertTrue(empty.isEmpty());
    assertFalse(present.isEmpty());
  }

  public void testGetAsInt() {
    // empty case
    assertThrows(NoSuchElementException.class, () -> empty.getAsInt());

    // non-empty case
    assertEquals(REFERENCE, present.getAsInt());
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

    assertThrows(NullPointerException.class, () -> empty.orElseThrow(null));

    // non-empty case
    assertThrows(NullPointerException.class, () -> present.ifPresent(null));

    Object reference = present.orElseThrow(null);
    assertEquals(REFERENCE, reference);
  }

  public void testIfPresent() {
    // empty case
    empty.ifPresent(wrapped -> fail("Empty Optional should not execute consumer"));

    // non-empty case
    present.ifPresent(
        (wrapped) -> {
          assertEquals(REFERENCE, wrapped);
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
          assertEquals(REFERENCE, wrapped);
          mutableFlag[0] = true;
        },
        () -> fail("Non-Empty Optional should not call empty consumer"));
    assertTrue("Consumer not executed", mutableFlag[0]);
  }

  public void testOrElse() {
    // empty case
    assertEquals(OTHER_REFERENCE, empty.orElse(OTHER_REFERENCE));

    // non-empty case
    assertEquals(REFERENCE, present.orElse(OTHER_REFERENCE));
  }

  public void testOrElseGet() {
    if (isWasm()) {
      // TODO(b/183769034): Re-enable when NPE on dereference is supported
      return;
    }

    // empty case
    assertThrows(NullPointerException.class, () -> empty.orElseGet(null));

    assertEquals(OTHER_REFERENCE, empty.orElseGet(() -> OTHER_REFERENCE));

    // non-empty case
    assertEquals(
        REFERENCE,
        present.orElseGet(
            () -> {
              fail("Optional must not execute supplier");
              return OTHER_REFERENCE;
            }));
  }

  public void testOrElseThrow() {
    if (isWasm()) {
      // TODO(b/183769034): Re-enable when NPE on dereference is supported
      return;
    }

    // empty case
    assertThrows(NullPointerException.class, () -> empty.orElseThrow(() -> null));

    assertThrows(IllegalStateException.class, () -> empty.orElseThrow(IllegalStateException::new));

    // non-empty case
    assertEquals(
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
    assertTrue(empty.equals(OptionalInt.empty()));

    // non empty case
    assertFalse(present.equals(null));
    assertFalse(present.equals("should not be equal"));
    assertFalse(present.equals(empty));
    assertFalse(present.equals(OptionalInt.of(OTHER_REFERENCE)));
    assertTrue(present.equals(present));
    assertTrue(present.equals(OptionalInt.of(REFERENCE)));
  }

  public void testHashcode() {
    // empty case
    assertEquals(0, empty.hashCode());

    // non empty case
    assertEquals(Integer.hashCode(REFERENCE), present.hashCode());
  }

  @J2ktIncompatible // Not emulated
  public void testStream() {
    assertEquals(0, OptionalInt.empty().stream().count());
    assertEquals(1, OptionalInt.of(10).stream().count());

    assertEquals(
        new int[] {10, 100, 1000},
        Stream.of(
                OptionalInt.of(10), OptionalInt.empty(), OptionalInt.of(100), OptionalInt.of(1000))
            .flatMapToInt(OptionalInt::stream)
            .toArray());
  }
}
