package com.google.j2cl.transpiler.integration.jsinnerclass;

import jsinterop.annotations.JsType;

public class Main {
  @JsType(isNative = true, namespace = "com.google.test")
  static class NativeType {
    // return new Inner(outer).getB();
    static native int getB(Outer outer);
  }

  public static void main(String... args) {
    // Call the constructor through js
    assert NativeType.getB(new Outer()) == 3;
    // Call the constructor through java
    assert new Outer().new Inner().getB() == 3;
  }
}
