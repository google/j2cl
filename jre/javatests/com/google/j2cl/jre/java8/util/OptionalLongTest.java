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
package com.google.j2cl.jre.java8.util;

import static com.google.j2cl.jre.testing.TestUtils.isWasm;

import com.google.j2cl.jre.testing.J2ktIncompatible;
import java.util.NoSuchElementException;
import java.util.OptionalLong;
import junit.framework.TestCase;

/** Tests for OptionalLong JRE emulation. */
public class OptionalLongTest extends TestCase {

  private static final long REFERENCE = 10L;
  private static final long OTHER_REFERENCE = 20L;
  private boolean[] mutableFlag;
  private OptionalLong empty;
  private OptionalLong present;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    mutableFlag = new boolean[1];
    empty = OptionalLong.empty();
    present = OptionalLong.of(REFERENCE);
  }

  public void testIsPresent() {
    // empty case
    assertFalse(empty.isPresent());

    // non-empty case
    assertTrue(present.isPresent());
  }

  public void testGetAsLong() {
    // empty case
    try {
      empty.getAsLong();
      fail("Empty Optional should throw NoSuchElementException");
    } catch (NoSuchElementException e) {
      // expected
    }

    // non-empty case
    assertEquals(REFERENCE, present.getAsLong());
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

    try {
      empty.orElseThrow(null);
      fail("Empty Optional must throw NullPointerException if supplier is null");
    } catch (NullPointerException e) {
      // expected
    }

    // non-empty case
    try {
      present.ifPresent(null);
      fail("Non-Empty Optional must throw NullPointerException if consumer is null");
    } catch (NullPointerException e) {
      // expected
    }

    try {
      Object reference = present.orElseThrow(null);
      assertEquals(REFERENCE, reference);
    } catch (NullPointerException e) {
      fail("Optional must not throw NullPointerException if supplier is null");
    }
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
    try {
      empty.orElseGet(null);
      fail("Empty Optional must throw NullPointerException if supplier is null");
    } catch (NullPointerException e) {
      // expected
    }

    assertEquals(OTHER_REFERENCE, empty.orElseGet(() -> OTHER_REFERENCE));

    // non-empty case
    assertEquals(REFERENCE, present.orElseGet(() -> {
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
    try {
      empty.orElseThrow(() -> null);
      fail("Empty Optional must throw NullPointerException if supplier returns null");
    } catch (NullPointerException e) {
      // expected
    }

    try {
      empty.orElseThrow(IllegalStateException::new);
      fail("Empty Optional must throw supplied exception");
    } catch (IllegalStateException e) {
      // expected
    }

    // non-empty case
    assertEquals(REFERENCE, present.orElseThrow(() -> {
      fail("Optional must not execute supplier");
      return new RuntimeException("should not execute");
    }));
  }

  public void testOrElseThrowNoArgs() {
    try {
      empty.orElseThrow();
      fail("Expected NoSuchElementException from empty Optional: orElseThrow");
    } catch (NoSuchElementException ignore) {
      // expected
    }

    assertEquals(REFERENCE, present.orElseThrow());
  }

  public void testIsEmpty() {
    assertTrue(empty.isEmpty());
    assertFalse(present.isEmpty());
  }

  public void testEquals() {
    // empty case
    assertFalse(empty.equals(null));
    assertFalse(empty.equals("should not be equal"));
    assertFalse(empty.equals(present));
    assertTrue(empty.equals(empty));
    assertTrue(empty.equals(OptionalLong.empty()));

    // non empty case
    assertFalse(present.equals(null));
    assertFalse(present.equals("should not be equal"));
    assertFalse(present.equals(empty));
    assertFalse(present.equals(OptionalLong.of(OTHER_REFERENCE)));
    assertTrue(present.equals(present));
    assertTrue(present.equals(OptionalLong.of(REFERENCE)));
  }

  public void testHashcode() {
    // empty case
    assertEquals(0, empty.hashCode());

    // non empty case
    assertEquals(Long.hashCode(REFERENCE), present.hashCode());
  }

}
