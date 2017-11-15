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
package com.google.j2cl.transpiler.readable.typewildcards;

class GenericType<T> {}

public class TypeWildCards {
  public void unbounded(GenericType<?> g) {}

  public void upperBound(GenericType<? extends TypeWildCards> g) {}

  public void lowerBound(GenericType<? super TypeWildCards> g) {}

  public void test() {
    unbounded(new GenericType<TypeWildCards>());
    upperBound(new GenericType<TypeWildCards>());
    lowerBound(new GenericType<TypeWildCards>());
  }

  private interface X {
    void m();
  }

  private interface Y {
    void n();
  }

  private static class A implements X {
    int f;

    @Override
    public void m() {}
  }

  public static <T extends A> void testBoundedTypeMemberAccess(T t) {
    int i = t.f;
    t.m();
  }

  public static <T extends A & Y> void testIntersectionBoundedTypeMemberAccess(T t) {
    int i = t.f;
    t.m();
    t.n();
  }
}
