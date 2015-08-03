package com.google.j2cl.transpiler.readable.genericconstructor;

public class GenericConstructor<T> {
  // constructor with both method level and class level type parameters.
  public <S> GenericConstructor(S s, T t) {}

  // constructor with method level type parameter that has the same same with the class level one.
  public <T> GenericConstructor(T t) {}

  public void test() {
    new GenericConstructor<Error>(new Exception(), new Error());
    new GenericConstructor<Error>(new Exception());
  }
}
