package com.google.j2cl.transpiler.readable.genericanddefaultmethods;

public class GenericClass<T> {
  public void foo(T value) {
    System.out.println("in GenericClass");
  }
}
