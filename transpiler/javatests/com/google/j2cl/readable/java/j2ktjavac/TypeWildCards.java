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
package j2ktjavac;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
class TypeWildCards {
  static class WithDependentBounds {
    interface Event {}

    interface Collection<V extends @Nullable Object> {}

    interface Observer<E extends Event, C extends Collection<E>> {
      void on(C events);
    }

    static class Holder<E extends Event, C extends Collection<E>> {
      @Nullable Observer<E, C> observer;

      void set(Observer<E, C> observer) {}

      static <E extends Event, C extends Collection<E>> void setStatic(
          Holder<E, C> holder, Observer<E, C> observer) {}
    }

    public static void testSetField(Holder<?, ?> holder) {
      holder.observer = e -> {};
    }

    public static void testSetMethod(Holder<?, ?> holder) {
      holder.set(e -> {});
    }

    public static void testSetStaticMethod(Holder<?, ?> holder) {
      Holder.setStatic(holder, e -> {});
    }
  }
}
