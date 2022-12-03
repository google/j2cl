/*
 * Copyright 2022 Google Inc.
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
package j2ktnotpassing;

import org.jspecify.nullness.NullMarked;

@NullMarked
class UnsatisfiedTypeBounds {
  interface Foo<T extends Foo<T>> {
    T get();
  }

  static class NonMarkedFoo implements Foo<NonMarkedFoo> {
    @Override
    public NonMarkedFoo get() {
      return this;
    }
  }

  interface Marker {}

  static class MarkedFoo implements Foo<MarkedFoo>, Marker {
    @Override
    public MarkedFoo get() {
      return this;
    }
  }

  <T> T methodWithTypeConstraints(T marker, Foo<? extends T> foo) {
    return foo.get();
  }

  void test() {
    Object object = methodWithTypeConstraints(new Marker() {}, new NonMarkedFoo());
    Marker marker = methodWithTypeConstraints(new Marker() {}, new MarkedFoo());
  }
}
