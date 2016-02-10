package com.google.j2cl.transpiler.integration.casttoclassdisabled;

/**
 * Test disabling of runtime cast checking.
 */
public class Main {
  @SuppressWarnings("unused")
  public static void main(String[] args) {
    Object object = new Main();

    // This is fine.
    Main main = (Main) object;

    // This should fail! But we've turned off cast checking in the BUILD file.
    RuntimeException exception = (RuntimeException) object;
  }
}
