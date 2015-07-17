package com.google.j2cl.transpiler.readable.localnamecollision;

public class LocalNameCollision {
  public void main() {
    new com.google.j2cl.transpiler.readable.localnamecollision.package1.A().m();
    new com.google.j2cl.transpiler.readable.localnamecollision.package2.A().m();
    new Asserts().n();
    new Class().main();
    assert new Class() != null;
  }
}
