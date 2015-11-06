package com.google.j2cl.transpiler.integration.renamejsmethodsinnativejstype;

import jsinterop.annotations.JsMethod;

public class Main {
  public static void main(String... args) {
    testJsMethodInJava();
    testJsMethodInJs();
  }

  public static void testJsMethodInJava() {
    Foo foo = new Foo();
    assert foo.sum() == 42;
    foo.x = 50;
    foo.y = 5;
    assert foo.sum() == 55;
  }

  public static void testJsMethodInJs() {
    Foo foo = new Foo();
    assert callSum(foo) == 42;
  }

  @JsMethod
  public static native int callSum(Foo foo);
}
