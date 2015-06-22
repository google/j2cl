package com.google.j2cl.transpiler.readable.instanceofclass;

public class InstanceofClass {
  public void test() {
    Object object = new InstanceofClass();
    assert object instanceof InstanceofClass;
    assert object instanceof Object;
    assert !(object instanceof String);
  }
}
