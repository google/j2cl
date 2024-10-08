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
package j2kt;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class ExplicitNotNullable {
  // Replicates wildcard problems in Guava's PairwiseEquivalence.
  static class DependentTypeParameters<E, T extends @Nullable E> {
    DependentTypeParameters<E, T> getThis() {
      return this;
    }
  }

  // TODO(b/255955130): Uncomment when fixed.
  // DependentTypeParameters<?, ?> testDependentWildcards(DependentTypeParameters<?, ?> x) {
  //   return x.getThis();
  // }
}
