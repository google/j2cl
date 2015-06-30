package com.google.j2cl.transpiler.readable.instanceofinterface;

import java.io.Serializable;

public class InstanceofInterface {
  public boolean test() {
    Object o = new Object();
    return o instanceof Serializable;
  }
}
