package com.google.j2cl.transpiler.integration.generictype;

public class Main<T, S> {
  public T first;
  public S second;

  public Main(T f, S s) {
    this.first = f;
    this.second = s;
  }

  public void set(T f, S s) {
    this.first = f;
    this.second = s;
  }

  public T getFirst() {
    return first;
  }

  public S getSecond() {
    return second;
  }

  public static void main(String... args) {
    Object first = new Object();
    Error second = new Error();
    Main<Object, Error> main = new Main<>(first, second);

    assert main.getFirst() == first;
    assert main.getSecond() == second;
    assert main.getSecond() instanceof Error;

    main.set(new Object(), new Error());
    assert main.getFirst() != first;
    assert main.getSecond() != second;
  }
}
