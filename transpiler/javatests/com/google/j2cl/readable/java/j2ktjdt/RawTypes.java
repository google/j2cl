/*
 * Copyright 2025 Google Inc.
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
package j2ktjdt;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class RawTypes {

  class Parent<T> {}

  class Child<T extends Child<T>> extends Parent<T> {}

  <T extends Child<T>> Child<T> copy(Child<T> child) {
    return child;
  }

  <T extends Child<T>> Parent<T> toParent(Child<T> a) {
    return a;
  }

  Parent returnsRaw(Child<?> parent) {
    return toParent(copy((Child) parent));
  }
}
