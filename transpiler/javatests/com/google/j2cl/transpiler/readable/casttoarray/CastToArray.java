package com.google.j2cl.transpiler.readable.casttoarray;

public class CastToArray {
  @SuppressWarnings({"cast", "unused"})
  public void main() {
    Object foo = (Object[]) null;
    Object bar = (Object[][]) null;
  }
}
