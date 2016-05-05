package com.google.j2cl.transpiler.integration.staticcompiletimeconstant;

public class Main {

  private static final String CONST_WITH_ESCAPES = "A\"SD\"F";

  // Class with circular static initialization used as a way to test that compile time constants are
  // initialized even before $clinit.
  private static class Foo {
    public static String circular = init();

    public static final String STRING_COMPILE_TIME_CONSTANT = "qwer";
    public static final byte BYTE_COMPILE_TIME_CONSTANT = 100;
    public static final short SHORT_COMPILE_TIME_CONSTANT = 100;
    public static final int INT_COMPILE_TIME_CONSTANT = 100;
    public static final long LONG_COMPILE_TIME_CONSTANT = 100L;
    public static final float FLOAT_COMPILE_TIME_CONSTANT = 100;
    public static final double DOUBLE_COMPILE_TIME_CONSTANT = 100;
    public static final char CHAR_COMPILE_TIME_CONSTANT = 100;
    public static final boolean BOOLEAN_COMPILE_TIME_CONSTANT = true;

    public static Object initialized = new Object();

    private static String init() {
      assert initialized == null;
      assert STRING_COMPILE_TIME_CONSTANT == "qwer";
      assert BYTE_COMPILE_TIME_CONSTANT == 100;
      assert SHORT_COMPILE_TIME_CONSTANT == 100;
      assert INT_COMPILE_TIME_CONSTANT == 100;
      assert LONG_COMPILE_TIME_CONSTANT == 100;
      assert FLOAT_COMPILE_TIME_CONSTANT == 100;
      assert DOUBLE_COMPILE_TIME_CONSTANT == 100;
      assert CHAR_COMPILE_TIME_CONSTANT == 100;
      assert BOOLEAN_COMPILE_TIME_CONSTANT == true;

      return Bar.circular;
    }
  }

  // Class with circular static initialization used as a way to test that compile time constants are
  // initialized even before $clinit.
  private static class Bar {
    public static String circular = init();

    public static final String STRING_COMPILE_TIME_CONSTANT = "qwer";
    public static final byte BYTE_COMPILE_TIME_CONSTANT = 100;
    public static final short SHORT_COMPILE_TIME_CONSTANT = 100;
    public static final int INT_COMPILE_TIME_CONSTANT = 100;
    public static final long LONG_COMPILE_TIME_CONSTANT = 100;
    public static final float FLOAT_COMPILE_TIME_CONSTANT = 100;
    public static final double DOUBLE_COMPILE_TIME_CONSTANT = 100;
    public static final char CHAR_COMPILE_TIME_CONSTANT = 100;
    public static final boolean BOOLEAN_COMPILE_TIME_CONSTANT = true;

    public static Object initialized = new Object();

    private static String init() {
      assert initialized == null;
      assert STRING_COMPILE_TIME_CONSTANT == "qwer";
      assert BYTE_COMPILE_TIME_CONSTANT == 100;
      assert SHORT_COMPILE_TIME_CONSTANT == 100;
      assert INT_COMPILE_TIME_CONSTANT == 100;
      assert LONG_COMPILE_TIME_CONSTANT == 100;
      assert FLOAT_COMPILE_TIME_CONSTANT == 100;
      assert DOUBLE_COMPILE_TIME_CONSTANT == 100;
      assert CHAR_COMPILE_TIME_CONSTANT == 100;
      assert BOOLEAN_COMPILE_TIME_CONSTANT == true;

      return Foo.circular;
    }
  }

  @SuppressWarnings("unused")
  public static void main(String... args) {
    // Trigger the $clinit;
    String a = Bar.circular;

    // Verify that even compile time constants handle string escaping the same as regular strings.
    assert Main.CONST_WITH_ESCAPES.equals("A\"SD\"F");
  }
}
