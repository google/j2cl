package com.google.j2cl.transpiler.integration.fieldmethodclasscollision;

/**
 * Test field, method and class name collision.
 */
public class Main {
  private static class Foo {}

  public static int Foo = 1;

  public static int Foo() {
    return 2;
  }

  public static void main(String... args) {
    assert new Foo() instanceof Foo;
    assert Foo == 1;
    assert Foo() == 2;
  }
}
