package com.google.j2cl.transpiler.integration.casttointerface;

import java.io.Serializable;

/**
 * Test cast to interface type.
 */
public class Main {
  @SuppressWarnings("unused")
  public static void main(String[] args) {
    Object object = new Person();

    // This is fine.
    HasName hasName = (HasName) object;

    // But this isn't fine.
    try {
      Serializable exception = (Serializable) object;
      assert false : "An expected failure did not occur.";
    } catch (ClassCastException e) {
      // expected
    }
  }
}
