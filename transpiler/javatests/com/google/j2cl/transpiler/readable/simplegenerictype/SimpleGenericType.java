package com.google.j2cl.transpiler.readable.simplegenerictype;

public class SimpleGenericType<T, S> {
  public T first;
  public S second;

  public SimpleGenericType(T t, S s) {
    this.first = t;
    this.second = s;
  }

  public void test() {
    @SuppressWarnings("unused")
    SimpleGenericType<Object, Error> g = new SimpleGenericType<>(new Object(), new Error());
  }
}
