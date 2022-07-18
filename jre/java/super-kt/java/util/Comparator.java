/*
 * Copyright 2022 Google Inc.
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

import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import javaemul.internal.annotations.KtNative;

/**
 * An interface used a basis for implementing custom ordering. <a
 * href="http://java.sun.com/j2se/1.5.0/docs/api/java/util/Comparator.html">[Sun docs]</a>
 *
 * @param <T> the type to be compared.
 */
@KtNative("kotlin.Comparator")
@FunctionalInterface
public interface Comparator<T> {

  int compare(T a, T b);

  default Comparator<T> reversed() {
    return null;
  }

  default Comparator<T> thenComparing(Comparator<? super T> other) {
    return null;
  }

  default <U> Comparator<T> thenComparing(
      Function<? super T, ? extends U> keyExtractor, Comparator<? super U> keyComparator) {
    return null;
  }

  default <U extends Comparable<? super U>> Comparator<T> thenComparing(
      Function<? super T, ? extends U> keyExtractor) {
    return null;
  }

  default Comparator<T> thenComparingInt(ToIntFunction<? super T> keyExtractor) {
    return null;
  }

  default Comparator<T> thenComparingLong(ToLongFunction<? super T> keyExtractor) {
    return null;
  }

  default Comparator<T> thenComparingDouble(ToDoubleFunction<? super T> keyExtractor) {
    return null;
  }

  static <T, U> Comparator<T> comparing(
      Function<? super T, ? extends U> keyExtractor, Comparator<? super U> keyComparator) {
    return null;
  }

  static <T, U extends Comparable<? super U>> Comparator<T> comparing(
      Function<? super T, ? extends U> keyExtractor) {
    return null;
  }

  static <T> Comparator<T> comparingDouble(ToDoubleFunction<? super T> keyExtractor) {
    return null;
  }

  static <T> Comparator<T> comparingInt(ToIntFunction<? super T> keyExtractor) {
    return null;
  }

  static <T> Comparator<T> comparingLong(ToLongFunction<? super T> keyExtractor) {
    return null;
  }

  static <T extends Comparable<? super T>> Comparator<T> naturalOrder() {
    return null;
  }

  static <T> Comparator<T> nullsFirst(Comparator<? super T> comparator) {
    return null;
  }

  static <T> Comparator<T> nullsLast(Comparator<? super T> comparator) {
    return null;
  }

  static <T extends Comparable<? super T>> Comparator<T> reverseOrder() {
    return null;
  }
}
