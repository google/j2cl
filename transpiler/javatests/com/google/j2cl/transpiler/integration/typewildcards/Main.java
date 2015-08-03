package com.google.j2cl.transpiler.integration.typewildcards;

class GenericType<T> {
  public T field;

  public GenericType(T f) {
    this.field = f;
  }
}

public class Main {
  public Object unbounded(GenericType<?> g) {
    return g.field;
  }

  public Object upperBound(GenericType<? extends Main> g) {
    return g.field;
  }

  public Object lowerBound(GenericType<? super Main> g) {
    return g.field;
  }

  public static void main(String... args) {
    Main m = new Main();
    Object o = new Object();
    SubClass s = new SubClass();

    GenericType<Main> gm = new GenericType<>(m);
    GenericType<Object> go = new GenericType<>(o);
    GenericType<SubClass> gs = new GenericType<>(s);

    assert (m.unbounded(gm) == m);
    assert (m.unbounded(go) == o);

    assert (m.upperBound(gm) == m);
    assert (m.upperBound(gs) == s);

    assert (m.lowerBound(gm) == m);
    assert (m.lowerBound(go) == o);
  }
}

class SubClass extends Main {}
