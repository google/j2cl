package com.google.j2cl.transpiler.readable.castonarrayinit;

public class CastOnArrayInit {
  public static class Foo<T> {}

  public static <E> void fun(Foo<E>... args) {}

  public static <E> void test() {
    Foo<E> f1 = new Foo<>();
    Foo<E> f2 = new Foo<>();
    fun(f1, f2);
  }
}
