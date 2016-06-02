package com.google.j2cl.transpiler.readable.methodreferences;

public class MethodReferences {
  interface Producer<T> {
    T produce();
  }

  static Object m() {
    return new Object();
  }

  void main() {
    Producer<Object> objectFactory = Object::new;
    objectFactory = MethodReferences::m;
  }
}
