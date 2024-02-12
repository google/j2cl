/*
 * Copyright 2024 Google Inc.
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

import com.google.j2cl.jre.java.util.EmulTestBase;
import java.util.Optional;
import java.util.stream.Stream;

/** Tests for java.util.Optional Java 9 API emulation. */
public class OptionalTest extends EmulTestBase {

  private static final Object REFERENCE = new Object();
  private static final Object OTHER_REFERENCE = new Object();

  private Optional<Object> empty;
  private Optional<Object> present;
  private Optional<Object> otherPresent;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    empty = Optional.empty();
    present = Optional.of(REFERENCE);
    otherPresent = Optional.of(OTHER_REFERENCE);
  }

  public void testOr() {
    assertTrue(present.or(() -> empty).isPresent());
    assertEquals(REFERENCE, present.or(() -> empty).get());
    assertTrue(present.or(() -> otherPresent).isPresent());
    assertEquals(REFERENCE, present.or(() -> otherPresent).get());

    assertFalse(empty.or(() -> empty).isPresent());
    assertTrue(empty.or(() -> present).isPresent());
    assertEquals(REFERENCE, empty.or(() -> present).get());

    assertThrowsNullPointerException(() -> empty.or(null));
    assertThrowsNullPointerException(() -> present.or(null));
    assertThrowsNullPointerException(() -> empty.or(() -> null));
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
