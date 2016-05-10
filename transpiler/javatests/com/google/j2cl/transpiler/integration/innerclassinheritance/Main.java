package com.google.j2cl.transpiler.integration.innerclassinheritance;

public class Main {
  public static void main(String[] args) {
    com.google.j2cl.transpiler.integration.innerclassinheritance.p1.A a =
        new com.google.j2cl.transpiler.integration.innerclassinheritance.p1.A();
    com.google.j2cl.transpiler.integration.innerclassinheritance.p1.A aa =
        new com.google.j2cl.transpiler.integration.innerclassinheritance.p2.A();
    assert (a.new B()
        instanceof com.google.j2cl.transpiler.integration.innerclassinheritance.p1.A.B);

    assert (aa.new B()
        instanceof com.google.j2cl.transpiler.integration.innerclassinheritance.p1.A.B);

    assert !(aa.new B()
        instanceof com.google.j2cl.transpiler.integration.innerclassinheritance.p2.A.B);
    assert (new com.google.j2cl.transpiler.integration.innerclassinheritance.p2.A().new B()
        instanceof com.google.j2cl.transpiler.integration.innerclassinheritance.p2.A.B);
    assert (new com.google.j2cl.transpiler.integration.innerclassinheritance.p2.A().new C()
        instanceof com.google.j2cl.transpiler.integration.innerclassinheritance.p1.A.C);
  }
}
