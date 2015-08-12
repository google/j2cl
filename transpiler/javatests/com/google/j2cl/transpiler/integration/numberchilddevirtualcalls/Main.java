package com.google.j2cl.transpiler.integration.numberchilddevirtualcalls;

class NumberChild extends Number {
  private double x;
  private double y;

  public NumberChild(double x, double y) {
    this.x = x;
    this.y = y;
  }

  @Override
  public int intValue() {
    return (int) (x + y);
  }

  @Override
  public long longValue() {
    return (long) (x + y);
  }

  @Override
  public float floatValue() {
    return (float) (x + y);
  }

  @Override
  public double doubleValue() {
    return x + y;
  }

  @Override
  public byte byteValue() {
    return (byte) x;
  }

  // does not override shortValue(), inherited from Number.shortValue().
}

public class Main {
  public static void main(String[] args) {
    Number nc = new NumberChild(2147483647.6, 2.6);
    assert (nc.byteValue() == -1);
    assert (nc.doubleValue() == 2.1474836502E9);
    //assert (nc.floatValue() == 2.14748365E9f); // does not distinguish float and double.
    assert (nc.intValue() == 2147483647);
    assert (nc.longValue() == 2147483650L);
    assert (nc.shortValue() == -1);

    assert (new NumberChild(3.6, 4.6).byteValue() == 3);
    assert (new NumberChild(3.6, 4.6).shortValue() == 8);
  }
}
