package com.google.j2cl.transpiler.integration.overridingclassmethods;

/**
 * Test overriding class methods.
 */
public class Main {
  public static void main(String... args) {
    Parent parentClass = new Parent();
    assert parentClass.overrideWithNoChange() instanceof Parent;
    assert parentClass.overrideWithReturnChange() instanceof Parent;
    assert parentClass.paramChangeCantOverride(null) instanceof Parent;

    Parent childClass = new Child();
    assert childClass.overrideWithNoChange() instanceof Child;
    assert childClass.overrideWithReturnChange() instanceof Child;
    assert childClass.paramChangeCantOverride(null) instanceof Parent;
  }
}
