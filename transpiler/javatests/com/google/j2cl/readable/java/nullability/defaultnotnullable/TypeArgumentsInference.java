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
package nullability.defaultnotnullable;

import org.jspecify.nullness.NullMarked;
import org.jspecify.nullness.Nullable;

@NullMarked
public class TypeArgumentsInference {
  public static final class Pair<K extends @Nullable Object, V extends @Nullable Object> {}

  public static <K extends @Nullable Object> Pair<K, ?> wildcardPair() {
    return new Pair<>();
  }

  public static <K extends @Nullable Object, V extends @Nullable Object> Pair<K, V> pairIdentity(
      Pair<K, V> pair) {
    return pair;
  }

  public static <K extends @Nullable Object> Pair<K, ?> wildcardPairAndIdentity() {
    // Since it's not possible to specify explicit type arguments for "pairIdentity" method call,
    // they should also be absent in the generated Kotlin code. J2KT does not render non-denotable
    // type arguments, but in this particular case, the second type parameter is incorrectly
    // inferred as java.lang.Object in the AST.
    return pairIdentity(wildcardPair());
  }
}
