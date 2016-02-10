package com.google.j2cl.transpiler.integration.casttoclassdisabledresultused;

/**
 * Test disabling of runtime cast checking but also using the value afterward.
 */
public class Main {
  @SuppressWarnings("unused")
  public static void main(String[] args) {
    Object object = new Main();

    // This should fail! But we've turned off cast checking in the BUILD file.
    RuntimeException exception = (RuntimeException) object;

    // Use the resulting casted object, to prove that size reductions are not accidentally because
    // the object was unused and JSCompiler decided to delete the cast on an unused thing.
    if (exception instanceof Object) {
      // Do something that has a side effect
      try {
        throw new RuntimeException(exception.toString());
      } catch (RuntimeException e) {
        // Good catch!11!!
      }
    }
  }
}
