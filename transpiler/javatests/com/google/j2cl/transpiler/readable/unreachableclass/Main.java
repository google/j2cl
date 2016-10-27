package com.google.j2cl.transpiler.readable.unreachableclass;

public class Main {
  {
    try {
    } catch (RuntimeException e) {
      class A {
        void m() {
          class B {
            private void n() {}
          }
        }
      }
      new A().m();
      new Object() {};
    }
  }
}
