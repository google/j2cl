package com.google.j2cl.transpiler.integration.simpleliterals;

public class Main {
  @SuppressWarnings("cast")
  public static void main(String... args) {
    boolean a = false;
    assert !a;

    char b = 'a';
    assert b == 97;

    Object c = null;
    assert c == null;

    int d = 100;
    assert d == 50 + 50;

    String e = "foo";
    assert e instanceof String;

    Class<?> f = Main.class;
    assert f instanceof Class;
  }
}
