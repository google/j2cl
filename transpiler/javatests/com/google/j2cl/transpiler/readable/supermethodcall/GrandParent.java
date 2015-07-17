package com.google.j2cl.transpiler.readable.supermethodcall;

public class GrandParent {
  public void grandParentSimplest() {}

  @SuppressWarnings("unused")
  public void grandParentWithParams(int foo) {}

  public Object grandParentWithChangingReturn() {
    return null;
  }
}
