package com.google.j2cl.transpiler.integration.jsinteroptests;

public class MyTestCase {
  public static void assertEquals(Object expected, Object actual) {
    assert expected.equals(actual)
        : "Not equals - expected: <" + expected + "> - actual: <" + actual + ">";
  }

  public static void assertEquals(int expected, int actual) {
    assert (expected == actual);
  }

  public static void assertTrue(boolean condition) {
    assert condition;
  }

  public static void assertFalse(boolean condition) {
    assert !condition;
  }

  public static void assertTrue(String message, boolean condition) {
    assert condition : message;
  }

  public static void assertFalse(String message, boolean condition) {
    assert !condition : message;
  }

  public static void assertNotNull(Object object) {
    assert object != null;
  }

  public static void assertNull(Object object) {
    assert object == null;
  }

  public static void assertSame(Object expected, Object actual) {
    assert expected == actual
        : "Not same - expected: <" + expected + "> - actual: <" + actual + ">";
  }

  public static void fail(String message) {
    assert false : message;
  }
}
