package com.google.j2cl.transpiler.integration.backwardbridgemethod;

interface I {
  String get(String s);
}

class C<T> {
  public T get(T t) {
    return t;
  }
}

/**
 * A bridge method for I.get(String):String should be generated, which delegates to C.get(T):T
 */
public class Main extends C<String> implements I {
  public static void main(String... args) {
    I i = new Main();
    C c = new Main();
    Main m = new Main();
    String s = "";
    assert i.get(s) == s;
    assert c.get(s) == s;
    assert m.get(s) == s;
  }
}
