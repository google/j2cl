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

import static javaemul.internal.InternalPreconditions.checkElement;
import static javaemul.internal.InternalPreconditions.checkElementIndex;
import static javaemul.internal.InternalPreconditions.checkNotNull;
import static javaemul.internal.InternalPreconditions.checkState;

import java.io.Serializable;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import javaemul.internal.ArrayHelper;

/**
 * Base implementation for ArrayList to share with different implementations for Wasm and JS.
 *
 * @param <E> the element type.
 */
abstract class ArrayListBase<E> extends AbstractList<E>
    implements List<E>, Cloneable, RandomAccess, Serializable {

  E[] array;

  @Override
  public boolean addAll(Collection<? extends E> c) {
    return addAll(sizeImpl(), c);
  }

  @Override
  public boolean contains(Object o) {
    return (indexOf(o) != -1);
  }

  @Override
  public E get(int index) {
    checkElementIndex(index, sizeImpl());
    return array[index];
  }

  @Override
  public int indexOf(Object o) {
    return indexOf(o, 0);
  }

  @Override
  public Iterator<E> iterator() {
    return new Iterator<E>() {
      int i = 0, last = -1;

      @Override
      public boolean hasNext() {
        return i < sizeImpl();
      }

      @Override
      public E next() {
        checkElement(hasNext());

        last = i++;
        return array[last];
      }

      @Override
      public void remove() {
        checkState(last != -1);

        removeImpl(i = last);
        last = -1;
      }
    };
  }

  @Override
  public void forEach(Consumer<? super E> consumer) {
    checkNotNull(consumer);
    int size = sizeImpl();
    for (int i = 0; i < size; i++) {
      consumer.accept(array[i]);
    }
  }

  @Override
  public int lastIndexOf(Object o) {
    return lastIndexOf(o, sizeImpl() - 1);
  }

  @Override
  public E remove(int index) {
    E previous = get(index);
    removeImpl(index);
    return previous;
  }

  @Override
  public boolean remove(Object o) {
    int i = indexOf(o);
    if (i == -1) {
      return false;
    }
    removeImpl(i);
    return true;
  }

  abstract void removeImpl(int index);

  @Override
  public boolean removeIf(Predicate<? super E> filter) {
    checkNotNull(filter);

    int newIndex = 0;
    int size = sizeImpl();
    for (int index = 0; index < size; ++index) {
      E e = array[index];

      if (!filter.test(e)) {
        if (newIndex != index) {
          array[newIndex] = e;
        }
        newIndex++;
      }
    }
    if (newIndex != size) {
      setSize(newIndex);
      return true;
    }
    return false;
  }

  @Override
  public void replaceAll(UnaryOperator<E> operator) {
    checkNotNull(operator);
    int size = sizeImpl();
    for (int i = 0; i < size; i++) {
      array[i] = operator.apply(array[i]);
    }
  }

  @Override
  public E set(int index, E o) {
    E previous = get(index);
    array[index] = o;
    return previous;
  }

  @Override
  public int size() {
    return sizeImpl();
  }

  // Avoid polymorphic size method for the internal usages.
  abstract int sizeImpl();

  /*
   * Faster than the iterator-based implementation in AbstractCollection.
   */
  @SuppressWarnings("unchecked")
  @Override
  public <T> T[] toArray(T[] out) {
    int size = sizeImpl();
    if (out.length < size) {
      out = ArrayHelper.createFrom(out, size);
    }
    for (int i = 0; i < size; ++i) {
      out[i] = (T) array[i];
    }
    if (out.length > size) {
      out[size] = null;
    }
    return out;
  }

  @Override
  public void sort(Comparator<? super E> c) {
    Arrays.sort(array, 0, sizeImpl(), c);
  }

  /**
   * Used by Vector.
   */
  int indexOf(Object o, int index) {
    for (int size = sizeImpl(); index < size; ++index) {
      if (Objects.equals(o, array[index])) {
        return index;
      }
    }
    return -1;
  }

  /**
   * Used by Vector.
   */
  int lastIndexOf(Object o, int index) {
    for (; index >= 0; --index) {
      if (Objects.equals(o, array[index])) {
        return index;
      }
    }
    return -1;
  }

  abstract void setSize(int newSize);
}
