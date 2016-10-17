package com.google.j2cl.transpiler.integration.staticfieldcompoundassignment;

public class Main {
  public static long bar;

  public static void main(String... args) {
    // If there were $q qualified static field references in the desugaring of this compound
    // assignment, then the CollapseProperties optimization pass would break them and the following
    // assertion would fail.
    bar++;

    assert bar == 1;
  }
}
