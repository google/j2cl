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

    public static String STRING_CONSTANT = "qwer";
    public static byte BYTE_CONSTANT = 100;
    public static short SHORT_CONSTANT = 100;
    public static int INT_CONSTANT = 100;
    public static long LONG_CONSTANT = 100;
    public static float FLOAT_CONSTANT = 100;
    public static double DOUBLE_CONSTANT = 100;
    public static char CHAR_CONSTANT = 100;
    public static boolean BOOLEAN_CONSTANT = true;

    private static String init() {
      assert Foo.STRING_COMPILE_TIME_CONSTANT != Foo.STRING_CONSTANT;
      assert Foo.BYTE_COMPILE_TIME_CONSTANT != Foo.BYTE_CONSTANT;
      assert Foo.SHORT_COMPILE_TIME_CONSTANT != Foo.SHORT_CONSTANT;
      assert Foo.INT_COMPILE_TIME_CONSTANT != Foo.INT_CONSTANT;
      assert Foo.LONG_COMPILE_TIME_CONSTANT != Foo.LONG_CONSTANT;
      assert Foo.FLOAT_COMPILE_TIME_CONSTANT != Foo.FLOAT_CONSTANT;
      assert Foo.DOUBLE_COMPILE_TIME_CONSTANT != Foo.DOUBLE_CONSTANT;
      assert Foo.CHAR_COMPILE_TIME_CONSTANT != Foo.CHAR_CONSTANT;
      assert Foo.BOOLEAN_COMPILE_TIME_CONSTANT != Foo.BOOLEAN_CONSTANT;

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

    public static String STRING_CONSTANT = "qwer";
    public static byte BYTE_CONSTANT = 100;
    public static short SHORT_CONSTANT = 100;
    public static int INT_CONSTANT = 100;
    public static long LONG_CONSTANT = 100;
    public static float FLOAT_CONSTANT = 100;
    public static double DOUBLE_CONSTANT = 100;
    public static char CHAR_CONSTANT = 100;
    public static boolean BOOLEAN_CONSTANT = true;

    private static String init() {
      assert Bar.STRING_COMPILE_TIME_CONSTANT != Bar.STRING_CONSTANT;
      assert Bar.BYTE_COMPILE_TIME_CONSTANT != Bar.BYTE_CONSTANT;
      assert Bar.SHORT_COMPILE_TIME_CONSTANT != Bar.SHORT_CONSTANT;
      assert Bar.INT_COMPILE_TIME_CONSTANT != Bar.INT_CONSTANT;
      assert Bar.LONG_COMPILE_TIME_CONSTANT != Bar.LONG_CONSTANT;
      assert Bar.FLOAT_COMPILE_TIME_CONSTANT != Bar.FLOAT_CONSTANT;
      assert Bar.DOUBLE_COMPILE_TIME_CONSTANT != Bar.DOUBLE_CONSTANT;
      assert Bar.CHAR_COMPILE_TIME_CONSTANT != Bar.CHAR_CONSTANT;
      assert Bar.BOOLEAN_COMPILE_TIME_CONSTANT != Bar.BOOLEAN_CONSTANT;

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
