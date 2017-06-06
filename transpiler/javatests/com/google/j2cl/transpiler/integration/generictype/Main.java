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
package com.google.j2cl.transpiler.integration.generictype;

public class Main<T, S> {
  public T first;
  public S second;

  public Main(T f, S s) {
    this.first = f;
    this.second = s;
  }

  public void set(T f, S s) {
    this.first = f;
    this.second = s;
  }

  public T getFirst() {
    return first;
  }

  public S getSecond() {
    return second;
  }

  public static void main(String... args) {
    Object first = new Object();
    Error second = new Error();
    Main<Object, Error> main = new Main<>(first, second);

    assert main.getFirst() == first;
    assert main.getSecond() == second;
    assert main.getSecond() instanceof Error;

    main.set(new Object(), new Error());
    assert main.getFirst() != first;
    assert main.getSecond() != second;
  }
}
