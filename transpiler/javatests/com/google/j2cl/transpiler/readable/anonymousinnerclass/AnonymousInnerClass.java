package com.google.j2cl.transpiler.readable.anonymousinnerclass;

class A {
  class B {
    
  }
}
public class AnonymousInnerClass {
  public class InnerClass {
  }
  public void test(final int arg) {
    InnerClass ic = new InnerClass() {}; // new an anonymous class with implicit qualifier.
    A a = new A();
    A.B b = a.new B(){}; // new an anonymous class with explicit qualifier.
    class C {
      public int fInC = arg;
    }
    C c = new C() {}; // new an anonymous local class.
  }
}
