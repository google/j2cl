package com.google.j2cl.transpiler.integration.castdevirtualizedtypes;

public class Main {
  @SuppressWarnings("unused")
  public static void main(String[] args) {
    testObject();
    testNumber();
    testComparable();
  }

  private static void testObject() {
    Object object = null;

    // All these casts should succeed.
    object = (Object) "";
    object = (Object) new Double(0);
    object = (Object) new Boolean(false);
    object = (Object) new Object[] {};
  }

  private static void testNumber() {
    Number number = null;

    // This casts should succeed.
    number = (Number) new Double(0);
  }

  private static void testComparable() {
    Comparable comparable = null;

    // All these casts should succeed.
    comparable = (Comparable) "";
    comparable = (Comparable) new Double(0);
    comparable = (Comparable) new Boolean(false);
  }

  private static void testCharSequence() {
    CharSequence charSequence = null;

    // This casts should succeed.
    charSequence = (CharSequence) "";
  }
}
