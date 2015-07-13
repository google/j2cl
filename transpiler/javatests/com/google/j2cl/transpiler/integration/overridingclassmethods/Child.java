package com.google.j2cl.transpiler.integration.overridingclassmethods;

public class Child extends Parent {
  @Override
  public Object overrideWithNoChange() {
    return new Child();
  }

  @Override
  public Parent overrideWithReturnChange() {
    return new Child();
  }

  public Object paramChangeCantOverride(Parent foo) {
    return new Child();
  }
}
