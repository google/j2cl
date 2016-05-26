package com.google.j2cl.transpiler.integration.npecatch;

/**
 * Test NPE catch.
 */
public class Main {
  public static void main(String... args) {
    try {
      throwNpe();
      assert false;
    } catch (NullPointerException expected) {
      // expected
    }
  }

  static class SomeObject {
    String doX() {
      return this + "x";
    }
  }

  public static void throwNpe() {
    SomeObject a = null;
    a.doX();
  }
}
