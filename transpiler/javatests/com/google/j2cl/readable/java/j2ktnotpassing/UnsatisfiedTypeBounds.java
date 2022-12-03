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

  interface Command {}

  static class FooCommand implements Foo<FooCommand>, Command {
    @Override
    public FooCommand get() {
      return this;
    }
  }

  static final class Helper<T> {}

  static <T> T methodWithTypeConstraints(Helper<T> helper, Foo<? extends T> foo) {
    return foo.get();
  }

  static void test() {
    Command command = methodWithTypeConstraints(new Helper<Command>(), new FooCommand());
  }
}
