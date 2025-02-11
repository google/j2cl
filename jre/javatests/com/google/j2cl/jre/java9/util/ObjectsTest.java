/*
 * Copyright 2025 Google Inc.
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

import java.util.Objects;
import junit.framework.TestCase;

public final class ObjectsTest extends TestCase {

  public void testRequireNonNull() {
    Integer one = 1;
    Integer anotherOne = 1;
    assertEquals(one, Objects.requireNonNull(anotherOne, "message"));
    assertEquals(one, Objects.requireNonNullElse(1, 2));
    assertEquals(one, Objects.requireNonNullElseGet(1, () -> 2));
    assertEquals(one, Objects.requireNonNullElse(null, 1));
    assertEquals(one, Objects.requireNonNullElseGet(null, () -> 1));

    Exception e =
        assertThrows(
            NullPointerException.class, () -> Objects.requireNonNull(null, "expected message"));
    assertTrue(e.getMessage().contains("expected message"));

    assertThrows(NullPointerException.class, () -> Objects.requireNonNullElse(null, null));
    // NOTE: j2cl throws an uncatchable exception on a null dereference in a function call.
    // assertThrows(NullPointerException.class, () -> Objects.requireNonNullElseGet(null, null));
    assertThrows(NullPointerException.class, () -> Objects.requireNonNullElseGet(null, () -> null));
  }
}
