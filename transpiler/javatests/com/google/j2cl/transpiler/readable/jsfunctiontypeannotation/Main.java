package com.google.j2cl.transpiler.readable.jsfunctiontypeannotation;

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
      return d;
    }
  }

  public static final class B<T> implements ApplyFunction<T, Integer> {
    @Override
    public T apply(T element, Integer i) {
      return null;
    }
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
    callGenericInterface(foo, "a", 1);
    callGenericInterface(bar, 1.1, 1.1);
    callParametricInterface(foo, "a");
    callParametricWithTypeVariable(foo, "a", 1);
    callParametricWithTypeVariable(bar, 1.1, 1.1);
    callImplementorGeneric(new B<Double>(), 1.1, 1);
    callImplementorParametric(new B<String>(), "");
    foo.apply("a", 1);
    bar.apply(1.1, 1.1);
    callOnFunction(new A());
  }

  public static void testCast() {
    Object o = new B<String>();
    B b1 = (B) o;
    B<String> b2 = (B<String>) o;
    ApplyFunction af1 = (ApplyFunction) o;
    ApplyFunction<String, Integer> af2 = (ApplyFunction<String, Integer>) o;
    A a = (A) o;
  }

  public static void testNewInstance() {
    B b1 = new B();
    B<String> b2 = (B) new B<String>();
    ApplyFunction af1 = new A();
  }

  @JsMethod
  public static native double callOnFunction(A f);
}
