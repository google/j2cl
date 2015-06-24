package com.google.j2cl.transpiler.integration.unaryexpressions;

/**
 * Test unary operations.
 */
public class Main {
  public static void main(String[] args) {
    int a = 10;
    assert a == 10;

    int b = a++;
    assert a == 11;
    assert b == 10;

    int c = a--;
    assert a == 10;
    assert c == 11;

    int d = ++a;
    assert a == 11;
    assert d == 11;

    int e = --a;
    assert a == 10;
    assert e == 10;

    int f = -a;
    assert f == -10;

    int g = +a;
    assert g == 10;

    int h = ~a;
    assert h == ~10;

    boolean i = (a == 100);
    assert !i;
  }
}
