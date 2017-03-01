package com.google.j2cl.transpiler.readable.bridgemethods;

import jsinterop.annotations.JsType;

public class TestCase102 {
  @JsType
  static interface BI2 {
    String get(String value);
  }

  static interface BI1 {
    String get(String value);
  }

  abstract static class B<B1> implements BI1, BI2 {
    public abstract String get(B1 value);
  }
}
