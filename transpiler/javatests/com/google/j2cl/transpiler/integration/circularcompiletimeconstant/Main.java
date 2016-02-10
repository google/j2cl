package com.google.j2cl.transpiler.integration.circularcompiletimeconstant;

public class Main {
  public static void main(String... args) {
    int total = A.C;
    assert total == 2;
  }

  public static class A {
    static final int A = B.B; // a looks a B.b
    static final int C = B.B + A;
  }

  public static class B {
    static final int B = A.A + 1; // which looks at A.a which is circular so it reads it as 0
  }
}

