package com.google.j2cl.transpiler.integration.arithmeticexceptiondisabledresultused;

public class Main {
  public static void main(String... args) {
    int a = 10;
    int b = 0;
    // This should fail! But we've turned off arithmetic exception in the BUILD file.
    int c = a / b;

    // Use the resulting value, to prove that size reductions are not accidentally because
    // the object was unused and JSCompiler decided to delete the cast on an unused thing.
    assert c < 100;
    assert c > -100;
  }
}
