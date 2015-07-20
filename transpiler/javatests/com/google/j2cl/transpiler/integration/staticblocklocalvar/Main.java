package com.google.j2cl.transpiler.integration.staticblocklocalvar;

/**
 * References (directly and as a captured variable in an anonymous class) a local variable defined
 * inside of a static block to show that the "declaring class" finding logic can handle the
 * situation.
 */
public class Main {
  private static int directlySetValue = 0;
  private static IGetter indirectValueGetter;

  static {
    final int someLocalVar = 999;

    // Referencing a local variable defined in a block (not method) directly.
    directlySetValue = someLocalVar;

    // Referencing a local variable defined in a block (not method) via variable capture.
    indirectValueGetter =
        new IGetter() {
          @Override
          public int get() {
            return someLocalVar;
          }
        };
  }

  public static void main(String... args) {
    assert directlySetValue == 999;
    assert indirectValueGetter.get() == 999;
  }
}
