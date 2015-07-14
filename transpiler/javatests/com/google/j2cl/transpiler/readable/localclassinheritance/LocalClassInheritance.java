package com.google.j2cl.transpiler.readable.localclassinheritance;

public class LocalClassInheritance {
  public void test() {
    final int f = 1;
    class Parent {
      public void n() {
        int i = f;
      }
    }
    class Child extends Parent {
      public void n() {
        int i = f;
      }
    }
  }
}

