package com.google.j2cl.transpiler.integration.devirtualizedsupermethodcall;

public class FooCallsSuperObjectMethods {
  @Override
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return super.equals(obj);
  }

  @Override
  public String toString() {
    return super.toString();
  }
}
