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
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.function.LongBinaryOperator;
import java.util.function.LongUnaryOperator;

public class AtomicLongArrayTest extends EmulTestBase {

  private static final LongBinaryOperator MULTIPLY = (value, multiplier) -> value * multiplier;
  private static final LongUnaryOperator DOUBLE = (value) -> value * 2;

  public void testArrayConstructor() {
    AtomicLongArray atomic = new AtomicLongArray(new long[] {42L});

    assertEquals(1, atomic.length());
    assertEquals(42L, atomic.get(0));
  }

  public void testLengthConstructor() {
    AtomicLongArray atomic = new AtomicLongArray(10);

    assertEquals(10, atomic.length());
    assertEquals(0, atomic.get(0));
  }

  public void testGet() {
    AtomicLongArray atomic = new AtomicLongArray(new long[] {10L, 20L, 30L});

    assertEquals(10L, atomic.get(0));
    assertEquals(20L, atomic.get(1));
    assertEquals(30L, atomic.get(2));
  }

  public void testGetAndAccumulate() {
    AtomicLongArray atomic = new AtomicLongArray(new long[] {42L});

    assertEquals(42L, atomic.getAndAccumulate(0, 2, MULTIPLY));
    assertEquals(84L, atomic.get(0));
  }

  public void testGetAndAdd() {
    AtomicLongArray atomic = new AtomicLongArray(new long[] {42L});

    assertEquals(42L, atomic.getAndAdd(0, 10L));
    assertEquals(52L, atomic.get(0));
  }

  public void testGetAndIncrement() {
    AtomicLongArray atomic = new AtomicLongArray(new long[] {42L});

    assertEquals(42L, atomic.getAndIncrement(0));
    assertEquals(43L, atomic.get(0));
  }

  public void testGetAndDecrement() {
    AtomicLongArray atomic = new AtomicLongArray(new long[] {42L});

    assertEquals(42L, atomic.getAndDecrement(0));
    assertEquals(41, atomic.get(0));
  }

  public void testGetAndSet() {
    AtomicLongArray atomic = new AtomicLongArray(new long[] {42L});

    assertEquals(42L, atomic.getAndSet(0, 50L));
    assertEquals(50L, atomic.get(0));
  }

  public void testGetAndUpdate() {
    AtomicLongArray atomic = new AtomicLongArray(new long[] {42L});

    assertEquals(42L, atomic.getAndUpdate(0, DOUBLE));
    assertEquals(84L, atomic.get(0));
  }

  public void testAccumulateAndGet() {
    AtomicLongArray atomic = new AtomicLongArray(new long[] {42L});

    assertEquals(84L, atomic.accumulateAndGet(0, 2, MULTIPLY));
    assertEquals(84L, atomic.get(0));
  }

  public void testAddAndGet() {
    AtomicLongArray atomic = new AtomicLongArray(new long[] {42L});

    assertEquals(52L, atomic.addAndGet(0, 10L));
    assertEquals(52L, atomic.get(0));
  }

  public void testIncrementAndGet() {
    AtomicLongArray atomic = new AtomicLongArray(new long[] {42L});

    assertEquals(43L, atomic.incrementAndGet(0));
    assertEquals(43L, atomic.get(0));
  }

  public void testDecrementAndGet() {
    AtomicLongArray atomic = new AtomicLongArray(new long[] {42L});

    assertEquals(41, atomic.decrementAndGet(0));
    assertEquals(41, atomic.get(0));
  }

  public void testUpdateAndGet() {
    AtomicLongArray atomic = new AtomicLongArray(new long[] {42L});

    assertEquals(84L, atomic.updateAndGet(0, DOUBLE));
    assertEquals(84L, atomic.get(0));
  }

  public void testCompareAndSet() {
    AtomicLongArray atomic = new AtomicLongArray(new long[] {42L});

    assertFalse(atomic.compareAndSet(0, 43L, 50L));
    assertEquals(42L, atomic.get(0));

    assertTrue(atomic.compareAndSet(0, 42L, 50L));
    assertEquals(50L, atomic.get(0));
  }

  public void testWeakCompareAndSet() {
    AtomicLongArray atomic = new AtomicLongArray(new long[] {42L});

    assertFalse(atomic.weakCompareAndSet(0, 43L, 50L));
    assertEquals(42L, atomic.get(0));

    assertTrue(atomic.weakCompareAndSet(0, 42L, 50L));
    assertEquals(50L, atomic.get(0));
  }

  public void testSet() {
    AtomicLongArray atomic = new AtomicLongArray(new long[] {42L});

    atomic.set(0, 50L);

    assertEquals(50L, atomic.get(0));
  }

  public void testLazySet() {
    AtomicLongArray atomic = new AtomicLongArray(new long[] {42L});

    atomic.lazySet(0, 50L);

    assertEquals(50L, atomic.get(0));
  }

  public void testLength() {
    assertEquals(0, new AtomicLongArray(0).length());
    assertEquals(10, new AtomicLongArray(10).length());
  }

  public void testToString() {
    AtomicLongArray atomic = new AtomicLongArray(new long[] {1L, 2L, 3L});

    assertEquals("[1, 2, 3]", atomic.toString());
  }
}
