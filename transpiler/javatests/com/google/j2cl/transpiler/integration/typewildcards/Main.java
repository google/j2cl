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
package com.google.j2cl.transpiler.integration.typewildcards;

@SuppressWarnings("rawtypes")
public class Main {
  public Object unbounded(GenericType<?> g) {
    return g.field;
  }

  public Object upperBound(GenericType<? extends Main> g) {
    return g.field;
  }

  public Object lowerBound(GenericType<? super Main> g) {
    return g.field;
  }

  public static void main(String... args) {
    Main m = new Main();
    Object o = new Object();
    SubClass s = new SubClass();

    GenericType<Main> gm = new GenericType<>(m);
    GenericType<Object> go = new GenericType<>(o);
    GenericType<SubClass> gs = new GenericType<>(s);

    assert (m.unbounded(gm) == m);
    assert (m.unbounded(go) == o);

    assert (m.upperBound(gm) == m);
    assert (m.upperBound(gs) == s);

    assert (m.lowerBound(gm) == m);
    assert (m.lowerBound(go) == o);

    assert new RawSubclass().f(null).equals("RawSubclass");
  }

  interface I<T extends Main> {
    default String f(T t) {
      return "default";
    }
  }

  static class RawSubclass implements I {
    @Override
    public String f(Main t) {
      return "RawSubclass";
    }
  }

  static class GenericType<T> {
    public T field;

    public GenericType(T f) {
      this.field = f;
    }
  }

  static class SubClass extends Main {}
}
