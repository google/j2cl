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
package com.google.j2cl.jre.java9.util.stream;

import java.util.stream.LongStream;

import com.google.j2cl.jre.java.util.EmulTestBase;

/**
 * Tests for java.util.stream.IntStream Java 9 API emulation.
 */
public class LongStreamTest extends EmulTestBase {

  public void testTakeWhile() {
    assertEquals(
        new long[] {1, 2},
        LongStream.of(1, 2, 3, 4, 5).takeWhile(i -> i < 3).toArray()
    );
    assertEquals(0, LongStream.of(1, 2, 3, 4, 5).takeWhile(i -> i > 2).count());

    assertEquals(
        new long[] {0, 1, 2, 3, 4},
        LongStream.iterate(0, i -> i + 1).takeWhile(i -> i < 5).toArray()
    );
    assertEquals(0, LongStream.empty()
            .takeWhile(n -> n < 4L)
            .count());
    assertEquals(0, LongStream.of(5L, 6L, 7L)
            .takeWhile(n -> n < 5L)
            .count());
    assertEquals(new long[] {1L, 2L, 3L}, LongStream.of(1L, 2L, 3L)
            .takeWhile(n -> n < 4L)
            .toArray());

    long[] first = LongStream.of(1L, 2L, 3L, 4L)
            .takeWhile(n -> n < 3L)
            .toArray();
    long[] second = LongStream.of(first)
            .takeWhile(n -> n < 3L)
            .toArray();
    assertEquals(first, second);
  }

  public void testDropWhile() {
    assertEquals(
        new long[] {3, 4, 5},
        LongStream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3).toArray()
    );
    assertEquals(
        new long[] {1, 2, 3, 4, 5},
        LongStream.of(1, 2, 3, 4, 5).dropWhile(i -> i > 2).toArray()
    );

    // pass an infinite stream to dropWhile, ensure it handles it
    assertEquals(
        new long[] {5, 6, 7, 8, 9},
        LongStream.iterate(0, i -> i + 1).dropWhile(i -> i < 5).limit(5).toArray()
    );
  }
}