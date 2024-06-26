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

import static javaemul.internal.InternalPreconditions.checkNotNull;

import java.util.function.UnaryOperator;
import javaemul.internal.ArrayHelper;
import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsNonNull;
import jsinterop.annotations.JsType;

/**
 * Represents a sequence of objects.
 * See <a href="https://docs.oracle.com/javase/8/docs/api/java/util/List.html">
 * the official Java API doc</a> for details.
 *
 * @param <E> element type
 */
@JsType
public interface List<E> extends Collection<E> {

  @JsIgnore
  static <E> List<E> of() {
    return jsOf();
  }

  @JsIgnore
  static <E> List<E> of(E e1) {
    return jsOf(e1);
  }

  @JsIgnore
  static <E> List<E> of(E e1, E e2) {
    return jsOf(e1, e2);
  }

  @JsIgnore
  static <E> List<E> of(E e1, E e2, E e3) {
    return jsOf(e1, e2, e3);
  }

  @JsIgnore
  static <E> List<E> of(E e1, E e2, E e3, E e4) {
    return jsOf(e1, e2, e3, e4);
  }

  @JsIgnore
  static <E> List<E> of(E e1, E e2, E e3, E e4, E e5) {
    return jsOf(e1, e2, e3, e4, e5);
  }

  @JsIgnore
  static <E> List<E> of(E e1, E e2, E e3, E e4, E e5, E e6) {
    return jsOf(e1, e2, e3, e4, e5, e6);
  }

  @JsIgnore
  static <E> List<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7) {
    return jsOf(e1, e2, e3, e4, e5, e6, e7);
  }

  @JsIgnore
  static <E> List<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8) {
    return jsOf(e1, e2, e3, e4, e5, e6, e7, e8);
  }

  @JsIgnore
  static <E> List<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9) {
    return jsOf(e1, e2, e3, e4, e5, e6, e7, e8, e9);
  }

  @JsIgnore
  static <E> List<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9, E e10) {
    return jsOf(e1, e2, e3, e4, e5, e6, e7, e8, e9, e10);
  }

  @JsIgnore // List.of API is exposed to JS via other means.
  static <E> List<E> of(E... elements) {
    // Note that this is not JsMethod.
    // If it was, then Java caller under JavaScript transpilation would do a JavaScript spread. We
    // cannot just trust that cloning since under Wasm, it won't have such cloning. As a result
    // under JS, we would end up having an extra clone happening.
    // To workaround that, we avoid marking this as a JS method and expose another method to JS to
    // act as the List.of implementation.

    // Since one might pass an array here, we need to do a defensive copy here.
    return Collections.internalListOf((E[]) ArrayHelper.unsafeClone(elements, 0, elements.length));
  }

  /** List.of API that is friendly to use from JavaScript. */
  @JsMethod(name = "of")
  private static <E> List<E> jsOf(E... elements) {
    // Forward directly as we don't need defensive copy for JavaScript calls.
    // Note that this method is also used internal "of(E e)" etc, to take advantage of JS varargs.
    return Collections.internalListOf(elements);
  }

  static <E> List<E> copyOf(Collection<? extends E> coll) {
    return Collections.internalListOf((E[]) coll.toArray());
  }

  @JsMethod(name = "addAtIndex")
  void add(int index, E element);

  @JsMethod(name = "addAllAtIndex")
  boolean addAll(int index, Collection<? extends E> c);

  @JsMethod(name = "getAtIndex")
  E get(int index);

  @JsIgnore
  @Override
  Iterator<E> iterator();

  int indexOf(Object o);

  int lastIndexOf(Object o);

  @JsIgnore
  ListIterator<E> listIterator();

  @JsIgnore
  ListIterator<E> listIterator(int from);

  @JsMethod(name = "removeAtIndex")
  E remove(int index);

  @JsIgnore
  default void replaceAll(UnaryOperator<E> operator) {
    checkNotNull(operator);
    for (int i = 0, size = size(); i < size; i++) {
      set(i, operator.apply(get(i)));
    }
  }

  @JsMethod(name = "setAtIndex")
  E set(int index, E element);

  @JsIgnore
  @SuppressWarnings("unchecked")
  default void sort(Comparator<? super E> c) {
    Object[] a = toArray();
    Arrays.sort(a, (Comparator<Object>) c);
    for (int i = 0; i < a.length; i++) {
      set(i, (E) a[i]);
    }
  }

  @JsIgnore
  @Override
  default Spliterator<E> spliterator() {
    return Spliterators.spliterator(this, Spliterator.ORDERED);
  }

  @JsNonNull List<E> subList(int fromIndex, int toIndex);
}
