package com.google.j2cl.transpiler.integration.castdevirtualizedtypes;

@SuppressWarnings("BoxedPrimitiveConstructor")
public class Main {
  public static void main(String[] args) {
    testObject();
    testNumber();
    testComparable();
    testCharSequence();
    testVoid();
  }

  private static void testObject() {
    Object unusedObject = null;

    // All these casts should succeed.
    unusedObject = (Object) "";
    unusedObject = (Object) new Double(0);
    unusedObject = (Object) new Boolean(false);
    unusedObject = (Object) new Object[] {};
  }

  private static void testNumber() {
    Number unusedNumber = null;

    // This casts should succeed.
    unusedNumber = (Number) new Double(0);
  }

  private static void testComparable() {
    Comparable<?> unusedComparable = null;

    // All these casts should succeed.
    unusedComparable = (Comparable) "";
    unusedComparable = (Comparable) new Double(0);
    unusedComparable = (Comparable) new Boolean(false);
  }

  private static void testCharSequence() {
    CharSequence unusedCharSequence = null;

    // This casts should succeed.
    unusedCharSequence = (CharSequence) "";
  }

  private static void testVoid() {
    Void unusedVoid = null;

    // This casts should succeed.
    unusedVoid = (Void) null;
  }
}
