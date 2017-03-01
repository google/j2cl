package com.google.j2cl.transpiler.readable.bridgemethods;

import jsinterop.annotations.JsType;

public class TestCase10036 {
  @JsType
  static interface BI3 {
    default String get(String value) {
      return "BI3 get String";
    }
  }

  static interface BI2 {
    String get(String value);
  }

  static interface BI1 extends BI3 {
    @Override
    String get(String value);
  }

  abstract static class B<B1 extends Comparable> implements BI1, BI2 {
    public abstract String get(B1 value);
  }
}
