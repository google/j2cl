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
package simplelongs;

public class SimpleLongs {
  public long foo = 0;

  public long getBar() {
    return 0;
  }

  @SuppressWarnings("unused")
  private int sideEffect;

  public SimpleLongs getWithSideEffect() {
    sideEffect++;
    return this;
  }

  @SuppressWarnings("unused")
  public void main() {
    // Small literals.
    long a = 0L;
    a = -100000L;
    a = 100000L;

    // Larger than int literals.
    long b = -2147483648L;
    b = 2147483648L;
    b = -9223372036854775808L;
    b = 9223372036854775807L;

    // Binary expressions.
    long c = a + b;
    c = a / b;

    // Prefix expressions;
    long e = ++a;
    e = ++foo;
    e = ++getWithSideEffect().foo;

    // Postfix expressions.
    long f = a++;
    f = foo++;
    f = getWithSideEffect().foo++;

    // Field initializers and function return statements.
    long g = foo;
    g = getBar();
  }
}
