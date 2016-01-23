package com.google.j2cl.transpiler.readable.varargs;

public class Varargs {
  public Varargs(int... args) {}

  public Varargs() {
    this(1);
  }

  public void test(int a, Object... args) {}

  public static <T> void fun(T... elements) {}

  public static <E> void bar(E a, E b) {
    fun(a, b);
  }

  public void main() {
    Varargs v = new Varargs();
    v.test(1);
    v.test(1, new Object());
    v.test(1, new Object[] {new Object()});
    v.test(1, new Object[] {});
  }
}

class Child extends Varargs {
  public Child() {
    super(1);
  }
}
