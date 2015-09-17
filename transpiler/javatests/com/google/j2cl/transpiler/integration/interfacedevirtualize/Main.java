package com.google.j2cl.transpiler.integration.interfacedevirtualize;

public class Main {
  public <T> int compare0(Comparable<T> c, T t) {
    return c.compareTo(t);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public int compare1(Comparable c, Object o) {
    return c.compareTo(o);
  }

  public int compare2(Comparable<Double> c, Double d) {
    return c.compareTo(d);
  }

  public int compare2(Comparable<Boolean> c, Boolean b) {
    return c.compareTo(b);
  }

  public int compare2(Comparable<Integer> c, Integer i) {
    return c.compareTo(i);
  }

  public int compare2(Comparable<Long> c, Long l) {
    return c.compareTo(l);
  }

  public int compare2(Comparable<ComparableImpl> c, ComparableImpl ci) {
    return c.compareTo(ci);
  }

  public int compare3(Double d1, Double d2) {
    return d1.compareTo(d2);
  }

  public int compare3(Boolean b1, Boolean b2) {
    return b1.compareTo(b2);
  }

  public int compare3(Integer i1, Integer i2) {
    return i1.compareTo(i2);
  }

  public int compare3(Long l1, Long l2) {
    return l1.compareTo(l2);
  }

  public int compare3(ComparableImpl c1, ComparableImpl c2) {
    return c1.compareTo(c2);
  }

  public void testDouble() {
    Double d1 = new Double(1.1);
    Double d2 = new Double(1.1);
    Double d3 = new Double(2.1);
    assert compare0(d1, d2) == 0;
    assert compare0(d1, d3) < 0;
    assert compare0(d3, d2) > 0;

    assert compare1(d1, d2) == 0;
    assert compare1(d1, d3) < 0;
    assert compare1(d3, d2) > 0;

    assert compare2(d1, d2) == 0;
    assert compare2(d1, d3) < 0;
    assert compare2(d3, d2) > 0;

    assert compare3(d1, d2) == 0;
    assert compare3(d1, d3) < 0;
    assert compare3(d3, d2) > 0;

    try {
      compare1(d1, new Object());
      assert false : "should have thrown an exception";
    } catch (ClassCastException e) {
      // expected.
    }
  }

  public void testBoolean() {
    Boolean b1 = new Boolean(false);
    Boolean b2 = new Boolean(false);
    Boolean b3 = new Boolean(true);
    assert compare0(b1, b2) == 0;
    assert compare0(b1, b3) < 0;
    assert compare0(b3, b2) > 0;

    assert compare1(b1, b2) == 0;
    assert compare1(b1, b3) < 0;
    assert compare1(b3, b2) > 0;

    assert compare2(b1, b2) == 0;
    assert compare2(b1, b3) < 0;
    assert compare2(b3, b2) > 0;

    assert compare3(b1, b2) == 0;
    assert compare3(b1, b3) < 0;
    assert compare3(b3, b2) > 0;

    try {
      compare1(b1, new Object());
      assert false : "should have thrown an exception";
    } catch (ClassCastException e) {
      // expected.
    }
  }

  public void testInteger() {
    Integer i1 = new Integer(1000);
    Integer i2 = new Integer(1000);
    Integer i3 = new Integer(2000);
    assert compare0(i1, i2) == 0;
    assert compare0(i1, i3) < 0;
    assert compare0(i3, i2) > 0;

    assert compare1(i1, i2) == 0;
    assert compare1(i1, i3) < 0;
    assert compare1(i3, i2) > 0;

    assert compare2(i1, i2) == 0;
    assert compare2(i1, i3) < 0;
    assert compare2(i3, i2) > 0;

    assert compare3(i1, i2) == 0;
    assert compare3(i1, i3) < 0;
    assert compare3(i3, i2) > 0;

    try {
      compare1(i1, new Object());
      assert false : "should have thrown an exception";
    } catch (ClassCastException e) {
      // expected.
    }
  }

  public void testLong() {
    Long l1 = new Long(1000L);
    Long l2 = new Long(1000L);
    Long l3 = new Long(2000L);
    assert compare0(l1, l2) == 0;
    assert compare0(l1, l3) < 0;
    assert compare0(l3, l2) > 0;

    assert compare1(l1, l2) == 0;
    assert compare1(l1, l3) < 0;
    assert compare1(l3, l2) > 0;

    assert compare2(l1, l2) == 0;
    assert compare2(l1, l3) < 0;
    assert compare2(l3, l2) > 0;

    assert compare3(l1, l2) == 0;
    assert compare3(l1, l3) < 0;
    assert compare3(l3, l2) > 0;

    try {
      compare1(l1, new Object());
      assert false : "should have thrown an exception";
    } catch (ClassCastException e) {
      // expected.
    }
  }

  public void testComparableImpl() {
    ComparableImpl c1 = new ComparableImpl(1000);
    ComparableImpl c2 = new ComparableImpl(1000);
    ComparableImpl c3 = new ComparableImpl(2000);
    assert compare0(c1, c2) == 0;
    assert compare0(c1, c3) < 0;
    assert compare0(c3, c2) > 0;

    assert compare1(c1, c2) == 0;
    assert compare1(c1, c3) < 0;
    assert compare1(c3, c2) > 0;

    assert compare2(c1, c2) == 0;
    assert compare2(c1, c3) < 0;
    assert compare2(c3, c2) > 0;

    assert compare3(c1, c2) == 0;
    assert compare3(c1, c3) < 0;
    assert compare3(c3, c2) > 0;

    try {
      compare1(c1, new Object());
      assert false : "should have thrown an exception";
    } catch (ClassCastException e) {
      // expected.
    }
  }

  public static void main(String[] args) {
    Main m = new Main();
    m.testDouble();
    m.testBoolean();
    m.testInteger();
    m.testLong();
    m.testComparableImpl();
  }
}
