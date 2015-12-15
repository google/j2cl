package com.google.j2cl.transpiler.integration.jsinteroptests;

public class MyTestCase {
  public void assertEquals(Object expected, Object actual) {
    assert (expected.equals(actual));
  }

  public void assertEquals(int expected, int actual) {
    assert (expected == actual);
  }

  public void assertTrue(boolean condition) {
    assert condition;
  }

  public void assertFalse(boolean condition) {
    assert !condition;
  }

  public void assertTrue(String message, boolean condition) {
    assert condition : message;
  }

  public void assertFalse(String message, boolean condition) {
    assert !condition : message;
  }

  public void assertNotNull(Object object) {
    assert object != null;
  }
}
