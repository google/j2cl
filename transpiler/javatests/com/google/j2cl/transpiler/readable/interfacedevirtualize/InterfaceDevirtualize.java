package com.google.j2cl.transpiler.readable.interfacedevirtualize;

public class InterfaceDevirtualize {
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

  public int compare3(Double d1, Double d2) {
    return d1.compareTo(d2);
  }

  public int compare2(Comparable<Boolean> c, Boolean d) {
    return c.compareTo(d);
  }

  public int compare3(Boolean d1, Boolean d2) {
    return d1.compareTo(d2);
  }

  public int compare2(Comparable<Integer> c, Integer d) {
    return c.compareTo(d);
  }

  public int compare3(Integer d1, Integer d2) {
    return d1.compareTo(d2);
  }
}
