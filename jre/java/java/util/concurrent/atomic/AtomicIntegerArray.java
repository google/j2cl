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
import java.util.function.IntBinaryOperator;
import java.util.function.IntUnaryOperator;

public final class AtomicIntegerArray {
  private final int[] array;

  public AtomicIntegerArray(int[] array) {
    this.array = Arrays.copyOf(array, array.length);
  }

  public AtomicIntegerArray(int length) {
    array = new int[length];
  }

  public int get(int i) {
    return array[i];
  }

  public int getAndAccumulate(int i, int x, IntBinaryOperator accumulatorFunction) {
    int previous = array[i];
    array[i] = accumulatorFunction.applyAsInt(previous, x);
    return previous;
  }

  public int getAndAdd(int i, int delta) {
    int previous = array[i];
    array[i] = previous + delta;
    return previous;
  }

  public int getAndIncrement(int i) {
    return getAndAdd(i, 1);
  }

  public int getAndDecrement(int i) {
    return getAndAdd(i, -1);
  }

  public int getAndSet(int i, int newValue) {
    int previous = array[i];
    array[i] = newValue;
    return previous;
  }

  public int getAndUpdate(int i, IntUnaryOperator updateFunction) {
    int previous = array[i];
    array[i] = updateFunction.applyAsInt(previous);
    return previous;
  }

  public int accumulateAndGet(int i, int x, IntBinaryOperator accumulatorFunction) {
    int newValue = accumulatorFunction.applyAsInt(array[i], x);
    array[i] = newValue;
    return newValue;
  }

  public int addAndGet(int i, int delta) {
    int newValue = array[i] + delta;
    array[i] = newValue;
    return newValue;
  }

  public int incrementAndGet(int i) {
    return addAndGet(i, 1);
  }

  public int decrementAndGet(int i) {
    return addAndGet(i, -1);
  }

  public int updateAndGet(int i, IntUnaryOperator updateFunction) {
    int newValue = updateFunction.applyAsInt(array[i]);
    array[i] = newValue;
    return newValue;
  }

  public boolean compareAndSet(int i, int expect, int update) {
    if (array[i] == expect) {
      array[i] = update;
      return true;
    }
    return false;
  }

  public boolean weakCompareAndSet(int i, int expect, int update) {
    return compareAndSet(i, expect, update);
  }

  public void set(int i, int newValue) {
    array[i] = newValue;
  }

  public void lazySet(int i, int newValue) {
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
