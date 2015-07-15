package com.google.j2cl.transpiler.integration.innerclassinheritance;

/**
 * TODO: uncomment when class name collisions are supported.
 */
public class Main {
  public static void main(String[] args) {
    //    com.google.j2cl.transpiler.integration.innerclassinheritance.p1.A a =
    //        new com.google.j2cl.transpiler.integration.innerclassinheritance.p1.A();
    //    com.google.j2cl.transpiler.integration.innerclassinheritance.p1.A aa =
    //        new com.google.j2cl.transpiler.integration.innerclassinheritance.p2.A();
    //    assert (a.new B() instanceof
    //        com.google.j2cl.transpiler.integration.innerclassinheritance.p1.A.B);
    //    // TODO: new inner class is resolved statically, so aa.new B() should be instanceof
    // p1.A.B,
    //    // but not p2.A.B. what we are doing now is dynamically dispatched, so this may be a bug
    // here.
    //    assert (aa.new B() instanceof
    //        com.google.j2cl.transpiler.integration.innerclassinheritance.p1.A.B);
    //    assert !(aa.new B() instanceof
    //        com.google.j2cl.transpiler.integration.innerclassinheritance.p2.A.B);
    //    assert (new com.google.j2cl.transpiler.integration.innerclassinheritance.p2.A().new B()
    // instanceof
    //        com.google.j2cl.transpiler.integration.innerclassinheritance.p2.A.B);
    //    assert (new com.google.j2cl.transpiler.integration.innerclassinheritance.p2.A().new C()
    // instanceof
    //        com.google.j2cl.transpiler.integration.innerclassinheritance.p1.A.C);
  }
}
