package com.google.j2cl.transpiler.integration.jsbridgemultipleaccidental;

import jsinterop.annotations.JsType;

interface InterfaceOne {
  int fun(int i);
}

interface InterfaceTwo extends InterfaceOne {
  int fun(int i);
}

interface InterfaceThree {
  int fun(int i);
}

@JsType
class C {
  public int fun(int i) {
    return i;
  }
}

public class Test extends C implements InterfaceTwo, InterfaceThree {
  // there should be only one bridge method created for List.fun() and Collection.fun().
}
