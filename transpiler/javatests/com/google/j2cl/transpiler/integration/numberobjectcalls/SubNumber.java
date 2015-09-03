package com.google.j2cl.transpiler.integration.numberobjectcalls;

public class SubNumber extends Number {
  @Override
  public int intValue() {
    return 0;
  }

  @Override
  public long longValue() {
    return 0;
  }

  @Override
  public float floatValue() {
    return 0;
  }

  @Override
  public double doubleValue() {
    return 0;
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof SubNumber;
  }

  @Override
  public int hashCode() {
    return 100;
  }

  public void test() {
    SubNumber sn = new SubNumber();
    assert (this.equals(sn));
    assert (equals(sn));
    assert (!equals(new Object()));

    assert (this.hashCode() == 100);
    assert (hashCode() == 100);

    assert (toString().equals(this.toString()));

    assert (getClass() instanceof Class);
    assert (getClass().equals(this.getClass()));
  }
}
