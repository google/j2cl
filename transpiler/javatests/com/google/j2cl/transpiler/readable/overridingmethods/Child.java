package com.google.j2cl.transpiler.readable.overridingmethods;

public class Child extends Parent implements SomeInterface {
  @Override
  public void fun() {}

  @Override
  public void bar() {}
}
