package com.google.j2cl.transpiler.readable.localnamecollision;

public class LocalNameCollision {
  @SuppressWarnings("unused")
  public void testClassLocalVarCollision() {
    boolean LocalNameCollision = false;
    Object RuntimeException = null;
    int Asserts = 1;
    int $Asserts = 1;
    int l_Asserts = 1;
    int com_google_j2cl_transpiler_readable_localnamecollision_Class = 1;
    int com_google_j2cl_transpiler_readable_localnamecollision_package1_A = 1;
    int com_google_j2cl_transpiler_readable_localnamecollision_package2_A = 1;
    int A = 1;
    LocalNameCollision =
        RuntimeException instanceof LocalNameCollision
            || RuntimeException instanceof RuntimeException
            || RuntimeException
                instanceof com.google.j2cl.transpiler.readable.localnamecollision.package1.A
            || RuntimeException
                instanceof com.google.j2cl.transpiler.readable.localnamecollision.package2.A
            || RuntimeException instanceof Class;
    assert new Asserts().n() == 5;
  }

  public boolean testClassParameterCollision(
      boolean LocalNameCollision, Object Asserts, Object $Asserts, int l_Asserts, int A) {
    return LocalNameCollision
        && Asserts instanceof LocalNameCollision
        && $Asserts instanceof Asserts
        && (l_Asserts == A);
  }

  @SuppressWarnings("unused")
  // test class and parameters collision in constructor.
  public LocalNameCollision(
      boolean LocalNameCollision, Object Asserts, Object $Asserts, int l_Asserts, int A) {
    boolean result =
        LocalNameCollision
            && Asserts instanceof LocalNameCollision
            && $Asserts instanceof Asserts
            && (l_Asserts == A);
  }
}
