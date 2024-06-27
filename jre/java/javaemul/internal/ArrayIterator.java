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
package javaemul.internal;

import static javaemul.internal.InternalPreconditions.checkElement;

import java.util.Iterator;

/** An iterator for an array. */
public final class ArrayIterator<T> implements Iterator<T> {

  private final T[] array;
  private int i;

  public ArrayIterator(T[] array) {
    this.array = array;
  }

  @Override
  public boolean hasNext() {
    return i < array.length;
  }

  @Override
  public T next() {
    checkElement(hasNext());
    return array[i++];
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }
}
