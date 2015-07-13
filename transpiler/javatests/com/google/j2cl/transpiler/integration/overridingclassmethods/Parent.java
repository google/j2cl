package com.google.j2cl.transpiler.integration.overridingclassmethods;

public class Parent {
  public Object overrideWithNoChange() {
    return new Parent();
  }

  public Object overrideWithReturnChange() {
    return new Parent();
  }

  public Object paramChangeCantOverride(Object foo) {
    return new Parent();
  }
}
