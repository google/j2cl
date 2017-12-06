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
package com.google.j2cl.transpiler.readable.cast;

public class CastGenerics<T, E extends Number> {

  T field;
  T method() {
    return null;
  }

  public static void testErasureCast() {
    String str = new CastGenerics<String, Number>().field;
    str = new CastGenerics<String, Number>().method();
  }

  public static void testCastToParamterizedType() {
    Object o = new Integer(1);
    CastGenerics<Error, Number> cc = (CastGenerics<Error, Number>) o;
    cc = (CastGenerics) o;
  }

  public void testCastToTypeVariable() {
    Object o = new Integer(1);
    E e = (E) o; // cast to type varaible with bound
    T t = (T) o; // cast to type varaible without bound
    E[] es = (E[]) o; // cast to array of type varaible.
    T[] ts = (T[]) o;
  }

  public <S, V extends Enum<V>> void testCastToMethodTypeVariable() {
    Object o = new Integer(1);
    S s = (S) o; // cast to type variable declared by the method.
    Object c = (CastGenerics<S, Number>) o;
    c = (S[]) o;
    c = (V) o;
  }

  /**
   * This method tests that J2CL correctly sets the Generic to its bound inside a method since
   * closure compiler cannot handle it.
   */
  public <TT extends Enum> void outerGenericMethod() {
    class Nested<SS> {
      private void nestedGenericMethod(Object o) {
        TT t = (TT) o;
      }
    }
  }

  interface Empty1 {};

  interface Empty2<TT> {};

  public <EE extends Empty1 & Empty2<EE>> EE method(Object o) {
    if (o instanceof Empty1) {
      return (EE) o;
    }
    return null;
  }


  public static <T> Foo<T> doSomething() {
    return new Foo<T>() { };
  }

  public static class Foo<V> { }
}
