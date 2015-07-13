package com.google.j2cl.transpiler.integration.overridinginterfacemethods;

public class SomeClass implements SomeInterface {
  @Override
  public Object overrideWithNoChange() {
    return new SomeClass();
  }

  @Override
  public SomeInterface overrideWithReturnChange() {
    return new SomeClass();
  }

  /**
   * Is not an override since it adds a parameter.
   */
  public Object overrideWithReturnChange(SomeInterface foo) {
    return new SomeClass();
  }
}
