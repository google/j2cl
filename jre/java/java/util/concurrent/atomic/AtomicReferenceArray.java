// CHECKSTYLE_OFF: Copyrighted to Guava Authors.
/*
 * Copyright (C) 2015 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// CHECKSTYLE_ON

package java.util.concurrent.atomic;

import java.util.Arrays;

/**
 * GWT emulated version of {@link AtomicReferenceArray}.
 *
 * @param <V> the element type.
 */
public class AtomicReferenceArray<V> {

  private final V[] values;

  public AtomicReferenceArray(V[] array) {
    values = Arrays.copyOf(array, array.length);
  }

  public AtomicReferenceArray(int length) {
    values = (V[]) new Object[length];
  }

  public boolean compareAndSet(int i, V expect, V update) {
    if (values[i] == expect) {
      values[i] = update;
      return true;
    }
    return false;
  }

  public V get(int i) {
    return values[i];
  }

  public V getAndSet(int i, V x) {
    V previous = values[i];
    values[i] = x;
    return previous;
  }

  public void lazySet(int i, V x) {
    values[i] = x;
  }

  public int length() {
    return values.length;
  }

  public void set(int i, V x) {
    values[i] = x;
  }

  public boolean weakCompareAndSet(int i, V expect, V update) {
    return compareAndSet(i, expect, update);
  }

  @Override
  public String toString() {
    return Arrays.toString(values);
  }
}
