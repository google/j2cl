package com.google.j2cl.transpiler.readable.autoboxingsideeffect;

@SuppressWarnings("BoxedPrimitiveConstructor")
public class AutoBoxingSideEffect {
  public Integer foo;

  @SuppressWarnings("unused")
  private int sideEffect;

  public AutoBoxingSideEffect getWithSideEffect() {
    sideEffect++;
    return this;
  }

  @SuppressWarnings("unused")
  public void main() {
    Integer i = 10000;
    Integer c = 1000;

    // Compound expressions.
    c += i;
    foo += i;
    getWithSideEffect().foo += i;
    foo <<= i;

    // Prefix expressions;
    Integer e = ++c;
    e = ++foo;
    e = ++getWithSideEffect().foo;

    // Postfix expressions.
    Integer f = i++;
    f = foo++;
    f = getWithSideEffect().foo++;

    Byte b = 0;
    b++;
  }
}
