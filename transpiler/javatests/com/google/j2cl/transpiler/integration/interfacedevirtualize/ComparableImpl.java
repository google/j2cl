package com.google.j2cl.transpiler.integration.interfacedevirtualize;

public class ComparableImpl implements Comparable<ComparableImpl> {
  private int field;

  public ComparableImpl(int f) {
    this.field = f;
  }

  @Override
  public int compareTo(ComparableImpl o) {
    return field - o.field;
  }
}
