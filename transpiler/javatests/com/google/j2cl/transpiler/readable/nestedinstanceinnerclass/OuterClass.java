package com.google.j2cl.transpiler.readable.nestedinstanceinnerclass;

public class OuterClass {
  public class InnerClass {
    public class InnerInnerClass {
      public InnerClass x = InnerClass.this;
      public OuterClass y = OuterClass.this;
    }
  }
  public void test() {
    new OuterClass().new InnerClass().new InnerInnerClass();
  }
}
