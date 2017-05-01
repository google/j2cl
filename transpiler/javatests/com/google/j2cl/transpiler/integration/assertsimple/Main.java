package com.google.j2cl.transpiler.integration.assertsimple;

/**
 * Test method body, assert statement, and binary expression
 * with number literals work fine.
 */
public class Main {
  public static void main(String[] args) {
    assert 1 + 2 == 3;

    try {
      assert 2 == 3;
      throw new RuntimeException("Failed to throw assert!");
    } catch (AssertionError expected) {
      // Success
      assert expected.getMessage() == null;
    }

    try {
      assert 2 == 3 : getDescription();
      throw new RuntimeException("Failed to throw assert!");
    } catch (AssertionError expected) {
      // Success
      assert expected.getMessage().equals("custom message");
    }

    try {
      assert 2 == 3 : new RuntimeException("42");
      throw new RuntimeException("Failed to throw assert!");
    } catch (AssertionError expected) {
      // Success
      assert expected.getCause().getMessage().equals("42");
    }

    try {
      assert 2 == 3 : 42;
      throw new RuntimeException("Failed to throw assert!");
    } catch (AssertionError expected) {
      // Success
      assert expected.getMessage().equals("42");
    }

    try {
      assert 2 == 3 : 42L;
      throw new RuntimeException("Failed to throw assert!");
    } catch (AssertionError expected) {
      // Success
      assert expected.getMessage().equals("42");
    }

    try {
      assert 2 == 3 : true;
      throw new RuntimeException("Failed to throw assert!");
    } catch (AssertionError expected) {
      // Success
      assert expected.getMessage().equals("true");
    }

    try {
      assert 2 == 3 : 'g';
      throw new RuntimeException("Failed to throw assert!");
    } catch (AssertionError expected) {
      // Success
      // TODO(b/37799560): fix char handling.
      // assert expected.getMessage().equals("g");
    }
  }

  private static Object getDescription() {
    return new Object() {
      @Override
      public String toString() {
        return "custom message";
      }
    };
  }
}
