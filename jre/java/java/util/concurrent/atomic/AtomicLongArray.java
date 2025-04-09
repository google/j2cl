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

package java.util.concurrent.atomic;

import java.util.Arrays;
import java.util.function.LongBinaryOperator;
import java.util.function.LongUnaryOperator;

public final class AtomicLongArray {
  private final long[] array;

  public AtomicLongArray(long[] array) {
    this.array = Arrays.copyOf(array, array.length);
  }

  public AtomicLongArray(int length) {
    array = new long[length];
  }

  public long get(int i) {
    return array[i];
  }

  public long getAndAccumulate(int i, long x, LongBinaryOperator accumulatorFunction) {
    long previous = array[i];
    array[i] = accumulatorFunction.applyAsLong(previous, x);
    return previous;
  }

  public long getAndAdd(int i, long delta) {
    long previous = array[i];
    array[i] = previous + delta;
    return previous;
  }

  public long getAndIncrement(int i) {
    return getAndAdd(i, 1);
  }

  public long getAndDecrement(int i) {
    return getAndAdd(i, -1);
  }

  public long getAndSet(int i, long newValue) {
    long previous = array[i];
    array[i] = newValue;
    return previous;
  }

  public long getAndUpdate(int i, LongUnaryOperator updateFunction) {
    long previous = array[i];
    array[i] = updateFunction.applyAsLong(previous);
    return previous;
  }

  public long accumulateAndGet(int i, long x, LongBinaryOperator accumulatorFunction) {
    long newValue = accumulatorFunction.applyAsLong(array[i], x);
    array[i] = newValue;
    return newValue;
  }

  public long addAndGet(int i, long delta) {
    long newValue = array[i] + delta;
    array[i] = newValue;
    return newValue;
  }

  public long incrementAndGet(int i) {
    return addAndGet(i, 1);
  }

  public long decrementAndGet(int i) {
    return addAndGet(i, -1);
  }

  public long updateAndGet(int i, LongUnaryOperator updateFunction) {
    long newValue = updateFunction.applyAsLong(array[i]);
    array[i] = newValue;
    return newValue;
  }

  public boolean compareAndSet(int i, long expect, long update) {
    if (array[i] == expect) {
      array[i] = update;
      return true;
    }
    return false;
  }

  public boolean weakCompareAndSet(int i, long expect, long update) {
    return compareAndSet(i, expect, update);
  }

  public void set(int i, long newValue) {
    array[i] = newValue;
  }

  public void lazySet(int i, long newValue) {
    set(i, newValue);
  }

  public int length() {
    return array.length;
  }

  @Override
  public String toString() {
    return Arrays.toString(array);
  }
}
