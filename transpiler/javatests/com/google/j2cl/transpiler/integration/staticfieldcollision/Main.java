package com.google.j2cl.transpiler.integration.staticfieldcollision;

import jsinterop.annotations.JsType;

/**
 * Tests that a name collision does not occur under the following circumstances:
 * - There is a static field with the same name as a generated method/field.
 * - That static field is not a compile time constant, requiring the generation of getters and a
 *   hidden field that contains the actual value.
 * - The static field is a @JsProperty.
 */
public class Main {
  @JsType
  public static class HasNameCollision {
    // Private storage should not collide with $clinit().
    public static Boolean clinit = new Boolean(false);
  }

  public static void main(String... args) {
    assert !HasNameCollision.clinit;
  }
}
