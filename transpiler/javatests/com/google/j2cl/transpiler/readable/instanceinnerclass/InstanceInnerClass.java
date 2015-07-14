package com.google.j2cl.transpiler.readable.instanceinnerclass;

public class InstanceInnerClass {
  public int instanceField;

  public class InnerClass {
    public int field = instanceField + InstanceInnerClass.this.instanceField;
    public InstanceInnerClass enclosingInstance = InstanceInnerClass.this;
  }

  public void test() {
    new InstanceInnerClass().new InnerClass();
  }
}
