package com.google.j2cl.transpiler.readable.innerclassinheritance;

public class ChildClass extends ParentOuter {
  public int fieldInOuter;

  public void funInOuter() {}

  class InnerClass extends ParentInner {
    public int fieldInInner;

    public void funInInner() {}

    public int testInnerClass() {
      // call to outer class's function inherited from ParentOuter
      funInParentOuter(); // this.outer.funInParentOuter()
      ChildClass.this.funInParentOuter(); // this.outer.funInParentOuter()

      // call to outer class's own declared function.
      funInOuter(); // this.outer.funInOuter()
      ChildClass.this.funInOuter(); // this.outer.funInOuter()

      // call to parent class's function.
      funInParentInner(); // this.funInParentIner()
      this.funInParentInner(); // this.funInParentInner()

      // call to own declared function.
      funInInner(); // this.funInInner()
      this.funInInner(); // this.funInInner()

      // accesses to outer class's own or inherited fields.
      int a = ChildClass.this.fieldInParentOuter;
      a = fieldInParentOuter;
      a = ChildClass.this.fieldInOuter;
      a = fieldInOuter;

      // accesses to its own or inherited fields.
      a = this.fieldInParentInner;
      a = fieldInParentInner;
      a = this.fieldInInner;
      a = fieldInInner;
      return a;
    }
  }

  public void testLocalClass() {
    class LocalClass extends ChildClass {
      @Override
      public void funInParentOuter() {}

      public void test() {
        funInOuter(); // this.funInOuter
        ChildClass.this.funInOuter(); // $outer.funInOuter
        funInParentOuter(); // this.funInParentOuter
        this.funInParentOuter(); // this.funInParentOuter
        ChildClass.this.funInParentOuter(); // $outer.funInParentOuter()
      }
    }
    new LocalClass().test();
  }
}
