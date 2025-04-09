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

package com.google.j2cl.jre.java.util.concurrent.atomic;

import com.google.j2cl.jre.java.util.EmulTestBase;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.function.IntBinaryOperator;
import java.util.function.IntUnaryOperator;

public class AtomicIntegerArrayTest extends EmulTestBase {

  private static final IntBinaryOperator MULTIPLY = (value, multiplier) -> value * multiplier;
  private static final IntUnaryOperator DOUBLE = (value) -> value * 2;

  public void testArrayConstructor() {
    AtomicIntegerArray atomic = new AtomicIntegerArray(new int[] {42});

    assertEquals(1, atomic.length());
    assertEquals(42, atomic.get(0));
  }

  public void testLengthConstructor() {
    AtomicIntegerArray atomic = new AtomicIntegerArray(10);

    assertEquals(10, atomic.length());
    assertEquals(0, atomic.get(0));
  }

  public void testGet() {
    AtomicIntegerArray atomic = new AtomicIntegerArray(new int[] {10, 20, 30});

    assertEquals(10, atomic.get(0));
    assertEquals(20, atomic.get(1));
    assertEquals(30, atomic.get(2));
  }

  public void testGetAndAccumulate() {
    AtomicIntegerArray atomic = new AtomicIntegerArray(new int[] {42});

    assertEquals(42, atomic.getAndAccumulate(0, 2, MULTIPLY));
    assertEquals(84, atomic.get(0));
  }

  public void testGetAndAdd() {
    AtomicIntegerArray atomic = new AtomicIntegerArray(new int[] {42});

    assertEquals(42, atomic.getAndAdd(0, 10));
    assertEquals(52, atomic.get(0));
  }

  public void testGetAndIncrement() {
    AtomicIntegerArray atomic = new AtomicIntegerArray(new int[] {42});

    assertEquals(42, atomic.getAndIncrement(0));
    assertEquals(43, atomic.get(0));
  }

  public void testGetAndDecrement() {
    AtomicIntegerArray atomic = new AtomicIntegerArray(new int[] {42});

    assertEquals(42, atomic.getAndDecrement(0));
    assertEquals(41, atomic.get(0));
  }

  public void testGetAndSet() {
    AtomicIntegerArray atomic = new AtomicIntegerArray(new int[] {42});

    assertEquals(42, atomic.getAndSet(0, 50));
    assertEquals(50, atomic.get(0));
  }

  public void testGetAndUpdate() {
    AtomicIntegerArray atomic = new AtomicIntegerArray(new int[] {42});

    assertEquals(42, atomic.getAndUpdate(0, DOUBLE));
    assertEquals(84, atomic.get(0));
  }

  public void testAccumulateAndGet() {
    AtomicIntegerArray atomic = new AtomicIntegerArray(new int[] {42});

    assertEquals(84, atomic.accumulateAndGet(0, 2, MULTIPLY));
    assertEquals(84, atomic.get(0));
  }

  public void testAddAndGet() {
    AtomicIntegerArray atomic = new AtomicIntegerArray(new int[] {42});

    assertEquals(52, atomic.addAndGet(0, 10));
    assertEquals(52, atomic.get(0));
  }

  public void testIncrementAndGet() {
    AtomicIntegerArray atomic = new AtomicIntegerArray(new int[] {42});

    assertEquals(43, atomic.incrementAndGet(0));
    assertEquals(43, atomic.get(0));
  }

  public void testDecrementAndGet() {
    AtomicIntegerArray atomic = new AtomicIntegerArray(new int[] {42});

    assertEquals(41, atomic.decrementAndGet(0));
    assertEquals(41, atomic.get(0));
  }

  public void testUpdateAndGet() {
    AtomicIntegerArray atomic = new AtomicIntegerArray(new int[] {42});

    assertEquals(84, atomic.updateAndGet(0, DOUBLE));
    assertEquals(84, atomic.get(0));
  }

  public void testCompareAndSet() {
    AtomicIntegerArray atomic = new AtomicIntegerArray(new int[] {42});

    assertFalse(atomic.compareAndSet(0, 43, 50));
    assertEquals(42, atomic.get(0));

    assertTrue(atomic.compareAndSet(0, 42, 50));
    assertEquals(50, atomic.get(0));
  }

  public void testWeakCompareAndSet() {
    AtomicIntegerArray atomic = new AtomicIntegerArray(new int[] {42});

    assertFalse(atomic.weakCompareAndSet(0, 43, 50));
    assertEquals(42, atomic.get(0));

    assertTrue(atomic.weakCompareAndSet(0, 42, 50));
    assertEquals(50, atomic.get(0));
  }

  public void testSet() {
    AtomicIntegerArray atomic = new AtomicIntegerArray(new int[] {42});

    atomic.set(0, 50);

    assertEquals(50, atomic.get(0));
  }

  public void testLazySet() {
    AtomicIntegerArray atomic = new AtomicIntegerArray(new int[] {42});

    atomic.lazySet(0, 50);

    assertEquals(50, atomic.get(0));
  }

  public void testLength() {
    assertEquals(0, new AtomicIntegerArray(0).length());
    assertEquals(10, new AtomicIntegerArray(10).length());
  }

  public void testToString() {
    AtomicIntegerArray atomic = new AtomicIntegerArray(new int[] {1, 2, 3});

    assertEquals("[1, 2, 3]", atomic.toString());
  }
}
