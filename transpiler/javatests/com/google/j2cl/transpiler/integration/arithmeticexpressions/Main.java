package com.google.j2cl.transpiler.integration.arithmeticexpressions;

public class Main {
  public static void main(String[] args) {
    //TODO: add test cases for each postfix and prefix operation.
    assert (1 + 1 + 2 - 5 == -1);
    assert ((1 + 2) * (3 + 4) == 21);
    assert (!(1 + 2 == 4));
    assert (!(1 + 2 + 3 == 4));
  }
}
