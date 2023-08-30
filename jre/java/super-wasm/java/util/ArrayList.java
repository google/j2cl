/*
 * Copyright 2007 Google Inc.
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
package java.util;

import static javaemul.internal.InternalPreconditions.checkArgument;
import static javaemul.internal.InternalPreconditions.checkPositionIndex;
import static javaemul.internal.InternalPreconditions.checkPositionIndexes;

import javaemul.internal.ArrayHelper;

/**
 * Resizeable array implementation of the List interface. See <a
 * href="https://docs.oracle.com/javase/8/docs/api/java/util/ArrayList.html">the official Java API
 * doc</a> for details.
 *
 * @param <E> the element type.
 */
public class ArrayList<E> extends ArrayListBase<E> {

  private int size;

  public ArrayList() {
    array = (E[]) new Object[10];
  }

  public ArrayList(Collection<? extends E> c) {
    array = (E[]) c.toArray();
    size = array.length;
  }

  public ArrayList(int initialCapacity) {
    // Avoid calling overridable methods from constructors
    checkArgument(initialCapacity >= 0, "Initial capacity must not be negative");
    array = (E[]) new Object[initialCapacity];
  }

  @Override
  public boolean add(E o) {
    int nextIndex = size;
    ensureCapacity(nextIndex + 1);
    array[nextIndex] = o;
    size = nextIndex + 1;
    return true;
  }

  @Override
  public void add(int index, E o) {
    checkPositionIndex(index, size);
    E[] array = insertGap(index, 1);
    array[index] = o;
  }

  @Override
  public boolean addAll(int index, Collection<? extends E> c) {
    checkPositionIndex(index, size);
    Object[] cArray = c.toArray();
    int len = cArray.length;
    if (len == 0) {
      return false;
    }
    E[] array = insertGap(index, len);
    ArrayHelper.copy(cArray, 0, array, index, len);
    return true;
  }

  private E[] insertGap(int index, int count) {
    E[] original = array;
    int originalCapacity = original.length;

    int originalSize = this.size;
    int newSize = originalSize + count;

    E[] target;
    if (newSize > originalCapacity) {
      // Ensure enough capacity.
      target = (E[]) new Object[ArrayHelper.getNewCapacity(originalCapacity, newSize)];
      // Only copy up to index since others will need to move anyway.
      ArrayHelper.copy(original, 0, target, 0, index);
      this.array = target;
    } else {
      target = original;
    }

    // Move the items. Note that we are not trying to clear existing values; callers will do that.
    ArrayHelper.copy(original, index, target, index + count, originalSize - index);

    this.size = newSize;

    return target;
  }

  public Object clone() {
    return new ArrayList<E>(this);
  }

  public void ensureCapacity(int minCapacity) {
    if (minCapacity > array.length) {
      array = ArrayHelper.grow(array, minCapacity);
    }
  }

  @Override
  void removeImpl(int index) {
    int newSize = size - 1;
    ArrayHelper.copy(array, index + 1, array, index, newSize - index);
    array[newSize] = null;
    this.size = newSize;
  }

  @Override
  int sizeImpl() {
    return size;
  }

  @Override
  public Object[] toArray() {
    return ArrayHelper.clone(array, 0, size);
  }
  
  public void trimToSize() {
    if (size == array.length) {
      return;
    }
    array = ArrayHelper.clone(array, 0, size);
  }

  @Override
  protected void removeRange(int fromIndex, int endIndex) {
    checkPositionIndexes(fromIndex, endIndex, size);
    int count = endIndex - fromIndex;
    if (count == 0) {
      return;
    }

    int oldSize = size;
    // Copy the items after removal end, overwriting removed items.
    ArrayHelper.copy(array, endIndex, array, fromIndex, oldSize - endIndex);
    // Clear the end of the array.
    Arrays.fill(array, oldSize - count, oldSize, null);

    this.size = oldSize - count;
  }

  void setSize(int newSize) {
    int oldSize = size;
    if (newSize == oldSize) {
      return;
    }
    if (newSize > oldSize) {
      ArrayHelper.grow(array, newSize);
    } else {
      Arrays.fill(array, newSize, oldSize, null);
    }
    this.size = newSize;
  }
}
