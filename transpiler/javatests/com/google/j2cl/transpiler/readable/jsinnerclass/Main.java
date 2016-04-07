package com.google.j2cl.transpiler.readable.jsinnerclass;

import jsinterop.annotations.JsType;

public class Main {
  static class Outer {
    int a = 2;

    @JsType(namespace = "com.google.test")
    public class Inner {
      private int b;

      public Inner() {
        this.b = a + 1;
      }

      public int getB() {
        return b;
      }
    }

    public int method() {
      return new Inner().getB() + a;
    }
  }

  public static void main(String... args) {
    assert new Outer().method() == 5;
  }
}
