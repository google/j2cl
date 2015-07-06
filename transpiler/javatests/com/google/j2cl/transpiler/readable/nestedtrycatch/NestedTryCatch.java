package com.google.j2cl.transpiler.readable.nestedtrycatch;

public class NestedTryCatch {
  public void main() {
    try {
      throw new Exception();
    } catch (Exception ae) {
      try {
        throw new Exception();
      } catch (Exception ie) {
        // expected empty body.
      }
    }
  }
}
