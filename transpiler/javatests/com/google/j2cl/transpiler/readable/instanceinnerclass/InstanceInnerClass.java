package com.google.j2cl.transpiler.readable.instanceinnerclass;

public class InstanceInnerClass {
  public int instanceField;

  public void funOuter() {}

  public class InnerClass {
    public int field = instanceField + InstanceInnerClass.this.instanceField;
    public InstanceInnerClass enclosingInstance = InstanceInnerClass.this;

    public void funInner() {
      funOuter();
      InstanceInnerClass.this.funOuter();
    }
  }

  public void test() {
    new InstanceInnerClass().new InnerClass();
  }
}
