package com.google.j2cl.transpiler.readable.methodreferences;

public class MethodReferences {

  interface Producer<T> {
    T produce();
  }

  interface Function<T, V> {
    T apply(V v);
  }

  static Object m() {
    return new Object();
  }

  public class Inner {

    Object n() {
      return new Object();
    }

    <T extends Comparable> void main() {
      Producer<Object> objectFactory = Object::new;
      objectFactory = MethodReferences::m;
      objectFactory = super::toString;
      objectFactory = this::n;
      objectFactory = MethodReferences.this::toString;
      objectFactory = MethodReferences.super::toString;
      objectFactory = "Hi"::toString;
    }
  }
}
