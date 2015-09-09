package com.google.j2cl.transpiler.readable.simplelongs;

public class SimpleLongs {
  public long foo = 0;

  public long getBar() {
    return 0;
  }

  @SuppressWarnings("unused")
  private int sideEffect;

  public SimpleLongs getWithSideEffect() {
    sideEffect++;
    return this;
  }

  @SuppressWarnings("unused")
  public void main() {
    // Small literals.
    long a = 0L;
    a = -100000L;
    a = 100000L;

    // Larger than int literals.
    long b = -2147483648L;
    b = 2147483648L;
    b = -9223372036854775808L;
    b = 9223372036854775807L;

    // Binary expressions.
    long c = a + b;
    c = a / b;

    // Prefix expressions;
    long e = ++a;
    e = ++foo;
    e = ++getWithSideEffect().foo;

    // Postfix expressions.
    long f = a++;
    f = foo++;
    f = getWithSideEffect().foo++;

    // Field initializers and function return statements.
    long g = foo;
    g = getBar();
  }
}
