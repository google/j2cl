/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.j2cl.transpiler.readable.lambdawithgenericsanddefaults;

@FunctionalInterface
interface BiFunction<T, U, R> {

  R apply(T t, U u);

  default <V> BiFunction<T, U, V> andThen(Function<? super R, ? extends V> after) {
    return (t, u) -> after.apply(this.apply(t, u));
  }
}

@FunctionalInterface
interface Function<T, R> {

  static <T> Function<T, T> identity() {
    return t -> t;
  }

  R apply(T t);
}
