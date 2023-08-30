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
 * <p>This implementation differs from JDK 1.5 <code>ArrayList</code> in terms of capacity
 * management. There is no speed advantage to pre-allocating array sizes in JavaScript, so this
 * implementation does not include any of the capacity and "growth increment" concepts in the
 * standard ArrayList class. Although <code>ArrayList(int)</code> accepts a value for the initial
 * capacity of the array, this constructor simply delegates to <code>ArrayList()</code>. It is only
 * present for compatibility with JDK 1.5's API.
 *
 * @param <E> the element type.
 */
public class ArrayList<E> extends ArrayListBase<E> {

  public ArrayList() {
    array = (E[]) new Object[0];
  }

  public ArrayList(Collection<? extends E> c) {
    this();
    // Avoid calling overridable methods from constructors
    addAllImpl(0, c);
  }

  public ArrayList(int initialCapacity) {
    this();
    // Avoid calling overridable methods from constructors
    checkArgument(initialCapacity >= 0, "Initial capacity must not be negative");
  }

  @Override
  public boolean add(E o) {
    ArrayHelper.push(array, o);
    return true;
  }

  @Override
  public void add(int index, E o) {
    checkPositionIndex(index, array.length);
    ArrayHelper.insertTo(array, index, o);
  }

  @Override
  public boolean addAll(int index, Collection<? extends E> c) {
    checkPositionIndex(index, array.length);
    return addAllImpl(index, c);
  }

  private boolean addAllImpl(int insertIndex, Collection<? extends E> c) {
    Object[] values = c.toArray();
    int len = values.length;
    if (len == 0) {
      return false;
    }

    int newLength = array.length + len;
    ArrayHelper.setLength(array, newLength);

    // Make room for the values that will be inserted by moving the existing elements to the
    // end so that they are not overwritten.
    int insertEndIndex = insertIndex + len;
    ArrayHelper.copy(array, insertIndex, array, insertEndIndex, newLength - insertEndIndex);

    // Copy new values into the insert location.
    ArrayHelper.copy(values, 0, array, insertIndex, len);
    return true;
  }

  public Object clone() {
    return new ArrayList<E>(this);
  }

  public void ensureCapacity(int ignored) {
    // Ignored.
  }

  @Override
  void removeImpl(int index) {
    ArrayHelper.removeFrom(array, index, 1);
  }

  @Override
  int sizeImpl() {
    return array.length;
  }

  @Override
  public Object[] toArray() {
    return ArrayHelper.clone(array);
  }

  public void trimToSize() {
    // We are always trimmed to size.
  }

  @Override
  protected void removeRange(int fromIndex, int endIndex) {
    checkPositionIndexes(fromIndex, endIndex, array.length);
    int count = endIndex - fromIndex;
    ArrayHelper.removeFrom(array, fromIndex, count);
  }

  void setSize(int newSize) {
    ArrayHelper.setLength(array, newSize);
  }
}
