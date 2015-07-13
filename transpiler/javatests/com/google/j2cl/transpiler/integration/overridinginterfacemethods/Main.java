package com.google.j2cl.transpiler.integration.overridinginterfacemethods;

/**
 * Test overriding interface methods.
 */
public class Main {
  public static void main(String... args) {
    SomeInterface instance = new SomeClass();
    assert instance.overrideWithNoChange() instanceof SomeClass;
    assert instance.overrideWithReturnChange() instanceof SomeClass;
    assert ((SomeClass) instance).overrideWithReturnChange(null) instanceof SomeClass;
  }
}
