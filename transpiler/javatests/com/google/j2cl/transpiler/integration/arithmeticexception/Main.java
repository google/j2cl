package com.google.j2cl.transpiler.integration.arithmeticexception;

public class Main {
  public static void main(String... args) {
    testDivideByZero();
    testModByZero();
  }

  private static void testDivideByZero() {
    try {
      int a = 10;
      int b = 0;
      @SuppressWarnings("unused")
      int c = a / b;
      assert false : "failed to throw ArithmeticException";
    } catch (ArithmeticException e) {
      // do nothing.
    }

    try {
      @SuppressWarnings("unused")
      int a = 10;
      int b = 0;
      a /= b;
      assert false : "failed to throw ArithmeticException";
    } catch (ArithmeticException e) {
      // do nothing.
    }

    try {
      @SuppressWarnings("unused")
      int a = 10;
      int b = 0;
      int c = 1 + a / b;
      assert false : "failed to throw ArithmeticException";
    } catch (ArithmeticException e) {
      // do nothing.
    }

    try {
      short a = 10;
      short b = 0;
      @SuppressWarnings("unused")
      short c = (short) (a / b);
      assert false : "failed to throw ArithmeticException";
    } catch (ArithmeticException e) {
      // do nothing.
    }

    try {
      char a = 10;
      char b = 0;
      @SuppressWarnings("unused")
      char c = (char) (a / b);
      assert false : "failed to throw ArithmeticException";
    } catch (ArithmeticException e) {
      // do nothing.
    }

    try {
      byte a = 10;
      byte b = 0;
      @SuppressWarnings("unused")
      byte c = (byte) (a / b);
      assert false : "failed to throw ArithmeticException";
    } catch (ArithmeticException e) {
      // do nothing.
    }

    try {
      long a = 10;
      long b = 0;
      @SuppressWarnings("unused")
      long c = (a / b);
      assert false : "failed to throw ArithmeticException";
    } catch (ArithmeticException e) {
      // do nothing.
    }

    // Uncomment once b/29912739 is fixed.
    // try {
    //   int a = 10;
    //   int b = 0;
    //   @SuppressWarnings("unused")
    //   long c = 10L + (a / b);
    //   assert false : "failed to throw ArithmeticException";
    // } catch (ArithmeticException e) {
    //  // do nothing.
    // }
    //
    // try {
    //   short a = 10;
    //   short b = 0;
    //   @SuppressWarnings("unused")
    //   long c = 10L + (a / b);
    //   assert false : "failed to throw ArithmeticException";
    // } catch (ArithmeticException e) {
    //   // do nothing.
    // }
  }

  private static void testModByZero() {
    try {
      int a = 10;
      int b = 0;
      @SuppressWarnings("unused")
      int c = a % b;
      assert false : "failed to throw ArithmeticException";
    } catch (ArithmeticException e) {
      // do nothing.
    }

    // Uncomment once b/28875322 is fixed.
    // try {
    //   @SuppressWarnings("unused")
    //   int a = 10;
    //   int b = 0;
    //   a %= b;
    //   assert false : "failed to throw ArithmeticException";
    // } catch (ArithmeticException e) {
    //  // do nothing.
    // }

    try {
      short a = 10;
      short b = 0;
      @SuppressWarnings("unused")
      short c = (short) (a % b);
      assert false : "failed to throw ArithmeticException";
    } catch (ArithmeticException e) {
      // do nothing.
    }

    try {
      char a = 10;
      char b = 0;
      @SuppressWarnings("unused")
      char c = (char) (a % b);
      assert false : "failed to throw ArithmeticException";
    } catch (ArithmeticException e) {
      // do nothing.
    }

    try {
      byte a = 10;
      byte b = 0;
      @SuppressWarnings("unused")
      byte c = (byte) (a % b);
      assert false : "failed to throw ArithmeticException";
    } catch (ArithmeticException e) {
      // do nothing.
    }

    try {
      long a = 10;
      long b = 0;
      @SuppressWarnings("unused")
      long c = (a % b);
      assert false : "failed to throw ArithmeticException";
    } catch (ArithmeticException e) {
      // do nothing.
    }

    // Uncomment once b/29912739 is fixed.
    // try {
    //   int a = 10;
    //   int b = 0;
    //   @SuppressWarnings("unused")
    //   long c = 10L + (a % b);
    //   assert false : "failed to throw ArithmeticException";
    // } catch (ArithmeticException e) {
    //  // do nothing.
    // }
    //
    // try {
    //   short a = 10;
    //   short b = 0;
    //   @SuppressWarnings("unused")
    //   double c = 10d + (a % b);
    //   assert false : "failed to throw ArithmeticException";
    // } catch (ArithmeticException e) {
    //   // do nothing.
    // }
  }
}
