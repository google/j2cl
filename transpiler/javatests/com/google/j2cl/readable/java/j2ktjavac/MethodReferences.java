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
public class MethodReferences {
  // J2KT with javac frontend renders `Consumer::accept` as:
  //
  // BiConsumer { arg0: Consumer<T>, arg1: T ->
  //   arg0.accept(arg1 as A)
  // }
  public static <T extends @Nullable Object>
      BiConsumer<Consumer<T>, T> testImplicitTypeArguments() {
    return apply(Consumer::accept);
  }

  // J2KT with javac frontend renders `Consumer::accept` as:
  //
  // BiConsumer { arg0: Consumer<T>, arg1: T ->
  //   arg0.accept(arg1 as A)
  // }
  public static <T extends @Nullable Object>
      BiConsumer<Consumer<T>, T> testExplicitTypeArguments() {
    return apply(Consumer<T>::accept);
  }

  public interface Consumer<A extends @Nullable Object> {
    void accept(A a);
  }

  public interface BiConsumer<A extends @Nullable Object, B extends @Nullable Object> {
    void accept(A a, B b);
  }

  public static <A extends @Nullable Object, B extends @Nullable Object> BiConsumer<A, B> apply(
      BiConsumer<A, B> biConsumer) {
    throw new RuntimeException();
  }
}
