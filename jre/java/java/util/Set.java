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


import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsType;

/**
 * Represents a set of unique objects. <a
 * href="http://java.sun.com/j2se/1.5.0/docs/api/java/util/Set.html">[Sun docs]</a>
 *
 * @param <E> element type.
 */
@JsType
public interface Set<E> extends Collection<E> {

  @JsIgnore
  @Override
  Iterator<E> iterator();

  @JsIgnore
  static <E> Set<E> of() {
    return jsOf();
  }

  @JsIgnore
  static <E> Set<E> of(E e1) {
    return jsOf(e1);
  }

  @JsIgnore
  static <E> Set<E> of(E e1, E e2) {
    return jsOf(e1, e2);
  }

  @JsIgnore
  static <E> Set<E> of(E e1, E e2, E e3) {
    return jsOf(e1, e2, e3);
  }

  @JsIgnore
  static <E> Set<E> of(E e1, E e2, E e3, E e4) {
    return jsOf(e1, e2, e3, e4);
  }

  @JsIgnore
  static <E> Set<E> of(E e1, E e2, E e3, E e4, E e5) {
    return jsOf(e1, e2, e3, e4, e5);
  }

  @JsIgnore
  static <E> Set<E> of(E e1, E e2, E e3, E e4, E e5, E e6) {
    return jsOf(e1, e2, e3, e4, e5, e6);
  }

  @JsIgnore
  static <E> Set<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7) {
    return jsOf(e1, e2, e3, e4, e5, e6, e7);
  }

  @JsIgnore
  static <E> Set<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8) {
    return jsOf(e1, e2, e3, e4, e5, e6, e7, e8);
  }

  @JsIgnore
  static <E> Set<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9) {
    return jsOf(e1, e2, e3, e4, e5, e6, e7, e8, e9);
  }

  @JsIgnore
  static <E> Set<E> of(E e1, E e2, E e3, E e4, E e5, E e6, E e7, E e8, E e9, E e10) {
    return jsOf(e1, e2, e3, e4, e5, e6, e7, e8, e9, e10);
  }

  @JsIgnore
  static <E> Set<E> of(E... elements) {
    // This is not marked as JS method for symmetry with List.of and avoid extra cloning at
    // call sites when an array is passed. A different method provided as Set.of to JS below.
    return Collections.internalSetOf(elements, /* allowDuplicates= */ false);
  }

  /** Set.of API that is friendly to use from JavaScript. */
  @JsMethod(name = "of")
  static <E> Set<E> jsOf(E... elements) {
    // Note that this method is also used internal "of(E e)" etc, to take advantage of JS varargs.
    return Collections.internalSetOf(elements, /* allowDuplicates= */ false);
  }

  static <E> Set<E> copyOf(Collection<? extends E> coll) {
    return Collections.internalSetOf((E[]) coll.toArray(), /* allowDuplicates= */ true);
  }

  @JsIgnore
  @Override
  default Spliterator<E> spliterator() {
    return Spliterators.spliterator(this, Spliterator.DISTINCT);
  }
}
