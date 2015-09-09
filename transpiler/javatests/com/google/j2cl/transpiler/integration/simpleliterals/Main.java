package com.google.j2cl.transpiler.integration.simpleliterals;

public class Main {
  @SuppressWarnings("cast")
  public static void main(String... args) {
    boolean a = false;
    assert !a;

    char b = 'a';
    assert b == 97;

    // Avoiding a "condition always evaluates to true" error in JSComp type checking.
    Object maybeNull = b == 97 ? null : new Object();
    Object c = null;
    assert c == maybeNull;

    int d = 100;
    assert d == 50 + 50;

    String e = "foo";
    assert e instanceof String;

    Class<?> f = Main.class;
    assert f instanceof Class;
  }
}
