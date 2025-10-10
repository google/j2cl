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

import javaemul.internal.annotations.KtIn;
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

  interface WithInTypeParameter<@KtIn T> {}

  class A implements WithInTypeParameter<A> {}

  class B implements WithInTypeParameter<B> {}

  <T> void consume(T... t) {}

  /**
   * A @KtIn type parameter cannot take wildcards with upper bounds; however the frontend my infer a
   * type for a @KtIn type parameter that has upper bounds because it is the more precise supertype.
   */
  void testIncosistentBounds() {

    // The frontend might infer WithInTypeParameter<? extends WithInTypeParameter<?>>
    consume(new A(), new B());
    this.<WithInTypeParameter<? extends WithInTypeParameter<?>>>consume(new A(), new B());
  }

  // repro for b/450914940
  static class Outer<T, V> {
    interface ParameterizedInterface<T extends @Nullable Object, V extends @Nullable Object> {}

    class Inner {
      void testNullabilityOnWildcardBounds(
          ParameterizedInterface<? super @Nullable T, ? extends @Nullable V> p) {
        testNullabilityOnWildcardBounds(p);
      }
    }
  }
}
