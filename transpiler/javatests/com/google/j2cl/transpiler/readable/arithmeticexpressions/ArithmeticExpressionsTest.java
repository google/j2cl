package com.google.j2cl.transpiler.readable.arithmeticexpressions;

public class ArithmeticExpressionsTest {
  public void test() {
    // TODO: add test cases for each prefix and postfix operation.
    assert (1 + 1 + 2 - 5 == -1);
    assert ((1 + 2) * (3 + 4) == 21);
    assert (!(1 + 2 == 4));
    assert (!(1 + 2 + 3 == 4));
  }
}
