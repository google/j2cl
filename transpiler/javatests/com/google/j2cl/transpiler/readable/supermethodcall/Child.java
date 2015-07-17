package com.google.j2cl.transpiler.readable.supermethodcall;

public class Child extends Parent {
  @Override
  public void parentSimplest() {
    super.parentSimplest();
  }

  @Override
  public void parentWithParams(int foo) {
    super.parentWithParams(foo);
  }

  @Override
  public Child parentWithChangingReturn() {
    super.parentWithChangingReturn();
    return this;
  }

  @Override
  public void grandParentSimplest() {
    super.grandParentSimplest();
  }

  @Override
  public void grandParentWithParams(int foo) {
    super.grandParentWithParams(foo);
  }

  @Override
  public Child grandParentWithChangingReturn() {
    super.grandParentWithChangingReturn();
    return this;
  }
}
