package com.google.j2cl.transpiler.readable.supermethodcall;

public class Parent extends GrandParent {
  public void parentSimplest() {}

  @SuppressWarnings("unused")
  public void parentWithParams(int foo) {}

  public Object parentWithChangingReturn() {
    return null;
  }
}
