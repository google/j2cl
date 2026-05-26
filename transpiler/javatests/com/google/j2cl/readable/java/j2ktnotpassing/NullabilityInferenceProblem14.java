/*
 * Copyright 2026 Google Inc.
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

import org.jspecify.annotations.NullMarked;

@NullMarked
public class NullabilityInferenceProblem14 {
  public interface F<T> {}

  public interface G<U, V> {}

  public interface P {
    <X, Y> F<Y> get(G<X, Y> g);
  }

  // repro for b/516211194.
  public void test(P p, G<String, Number> g1, G<Double, Integer> g2) {
    f(p.get(g1), p.get(g2));
  }

  private static <W> void f(W... w) {}
}
