package com.google.j2cl.transpiler.readable.multiplecatch;

public class MultipleCatch {
  public void main() {
    try {
      throw new ClassCastException();
    } catch (NullPointerException | ClassCastException e) {
      // expected empty body.
    } catch (RuntimeException r) {
      r = null; // used to show exception variable is transpiled correctly.
    }
  }
}
