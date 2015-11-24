package com.google.j2cl.transpiler.integration.interfacejsbridge;

import jsinterop.annotations.JsType;

@JsType
interface MyJsInterface {
  int foo(int a);
}

interface MyInterface {
  int foo(int a);
}

interface SubInterface extends MyJsInterface, MyInterface {
  @Override
  int foo(int a);
}

public class InterfaceImpl implements SubInterface {
  @Override
  public int foo(int a) {
    return a;
  }
}
