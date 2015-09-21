package com.google.j2cl.transpiler.integration.implementsinterface;

interface MyInterface {
  
}

class A implements MyInterface {}

class B implements MyInterface {}

class C implements MyInterface {}

class D implements MyInterface {}

class E implements MyInterface {}

class F implements MyInterface {}

public class Main {
  public static void main(String... args) {
    Object o = new Object();
    assert !(o instanceof Exception);
  }
}
