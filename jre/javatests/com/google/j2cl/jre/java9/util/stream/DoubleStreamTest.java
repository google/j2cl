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

import com.google.j2cl.jre.java.util.EmulTestBase;
import java.util.stream.DoubleStream;

/** Tests for java.util.stream.DoubleStream Java 9 API emulation. */
public class DoubleStreamTest extends EmulTestBase {

  public void testTakeWhile() {
    assertEquals(
        new double[] {1.1, 2.2},
        DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5, 1.1).takeWhile(i -> i < 3).toArray());
    assertEquals(0, DoubleStream.of(1.1, 2.2, 3.3, 4.4, 5.5).takeWhile(i -> i > 2).count());

    assertEquals(
        new double[] {0, 1.0, 2.0, 3.0, 4.0},
        DoubleStream.iterate(0, i -> i + 1).takeWhile(i -> i < 5).toArray());
    assertEquals(0, DoubleStream.empty().takeWhile(n -> n < 4).count());
    assertEquals(0, DoubleStream.of(5.0, 6.0, 7.0).takeWhile(n -> n < 5).count());
    assertEquals(
        new double[] {1.0, 2.0, 3.0},
        DoubleStream.of(1.0, 2.0, 3.0).takeWhile(n -> n < 4).toArray());

    // pass an infinite stream to takeWhile, ensure it handles it
    assertEquals(
        new double[] {0, 1.0, 2.0, 3.0, 4.0},
        DoubleStream.iterate(0, i -> i + 1).takeWhile(i -> true).limit(5).toArray());
  }

  public void testDropWhile() {
    assertEquals(
        new double[] {3, 4, 5}, DoubleStream.of(1, 2, 3, 4, 5).dropWhile(i -> i < 3).toArray());
    assertEquals(
        new double[] {1, 2, 3, 4, 5},
        DoubleStream.of(1, 2, 3, 4, 5).dropWhile(i -> i > 2).toArray());

    // pass an infinite stream to dropWhile, ensure it handles it
    assertEquals(
        new double[] {5, 6, 7, 8, 9},
        DoubleStream.iterate(0, i -> i + 1).dropWhile(i -> i < 5).limit(5).toArray());
  }
}
