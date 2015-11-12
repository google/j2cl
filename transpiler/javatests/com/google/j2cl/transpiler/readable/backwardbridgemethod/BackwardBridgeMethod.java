package com.google.j2cl.transpiler.readable.backwardbridgemethod;

interface I {
  String get(String s);
}

class C<T> {
  public T get(T t) {
    return null;
  }
}

public class BackwardBridgeMethod extends C<String> implements I {
  public void test() {
    I i = new BackwardBridgeMethod();
    C c = new BackwardBridgeMethod();
    BackwardBridgeMethod a = new BackwardBridgeMethod();
    i.get("");
    c.get("");
    a.get("");
  }
}
