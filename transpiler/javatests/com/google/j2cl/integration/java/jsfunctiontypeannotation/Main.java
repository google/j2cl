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
package jsfunctiontypeannotation;

import static com.google.j2cl.integration.testing.Asserts.assertThrowsClassCastException;
import static com.google.j2cl.integration.testing.Asserts.assertTrue;

import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsMethod;

public class Main {
  @JsFunction
  interface ApplyFunction<T, S extends Number> {
    T apply(T t, S s);
  }

  public static final class A implements ApplyFunction<Double, Double> {
    @Override
    public Double apply(Double d, Double i) {
      return d + i;
    }
  }

  public static final class B<T> implements ApplyFunction<T, Integer> {
    @Override
    public T apply(T element, Integer i) {
      return element;
    }
  }

  public static void main(String... args) {
    testParameterTypes();
    testCast();
    testNewInstance();
  }

  public static Object callGenericInterface(ApplyFunction af, Object o, Number n) {
    return af.apply(o, n);
  }

  public static String callParametricInterface(ApplyFunction<String, Integer> af, String s) {
    return af.apply(s, 1);
  }

  public static <U, V extends Number> U callParametricWithTypeVariable(
      ApplyFunction<U, V> af, U u, V v) {
    return af.apply(u, v);
  }

  public static Object callImplementorGeneric(B b, Object o, Integer n) {
    return b.apply(o, n);
  }

  public static String callImplementorParametric(B<String> b, String s) {
    return b.apply(s, 1);
  }

  public static void testParameterTypes() {
    ApplyFunction foo = new B<String>();
    ApplyFunction bar = new A();
    assertTrue(callGenericInterface(foo, "a", 1).equals("a"));
    assertTrue(callGenericInterface(bar, 1.1, 1.1).equals(new Double(2.2)));
    assertTrue(callParametricInterface(foo, "a").equals("a"));
    assertTrue(callParametricWithTypeVariable(foo, "a", 1).equals("a"));
    assertTrue(callParametricWithTypeVariable(bar, 1.1, 1.1).equals(new Double(2.2)));
    assertTrue(callImplementorGeneric(new B<Double>(), 1.1, 1).equals(new Double(1.1)));
    assertTrue(callImplementorParametric(new B<String>(), "").equals(""));
    assertTrue(foo.apply("a", 1).equals("a"));
    assertTrue(bar.apply(1.1, 1.1).equals(new Double(2.2)));
    assertTrue(callOnFunction(new A()) == 2.2);
  }

  public static void testCast() {
    Object o = new B<String>();
    B b1 = (B) o;
    B<String> b2 = (B<String>) o;
    ApplyFunction af1 = (ApplyFunction) o;
    ApplyFunction<String, Integer> af2 = (ApplyFunction<String, Integer>) o;

    assertThrowsClassCastException(
        () -> {
          A a = (A) o;
        });
    assertTrue(b1 != null);
    assertTrue(b2 != null);
    assertTrue(af1 != null);
    assertTrue(af2 != null);
  }

  public static void testNewInstance() {
    B b1 = new B();
    B<String> b2 = (B) new B<String>();
    ApplyFunction af1 = new A();
    assertTrue(b1 != null);
    assertTrue(b2 != null);
    assertTrue(af1 != null);
  }

  @JsMethod
  public static native double callOnFunction(ApplyFunction<Double, Double> f);
}
