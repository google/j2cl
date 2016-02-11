package com.google.j2cl.transpiler.integration.arithmeticexceptiondisabled;

public class Main {
  public static void main(String... args) {
    int a = 10;
    int b = 0;
    // This should fail! But we've turned off arithmetic exception in the BUILD file.
    int c = a / b;
  }
}
