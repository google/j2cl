package com.google.j2cl.transpiler.readable.innerclassinheritance;

public class MultipleNestings {
  public class Parent {
    public void fun() {}
  }

  public void funInM() {}

  public class InnerClass1 extends Parent {
    public void funInI1() {}

    /**
     * Inner class that extends the same class as its enclosing class extends.
     */
    public class InnerClass2 extends Parent {
      public void funInI2() {}

      public void test() {
        // call to inherited fun() with different qualifier
        this.fun(); // this.fun()
        fun(); // this.fun()
        InnerClass1.this.fun(); // this.outer.fun()

        // call to function of outer classes
        funInM(); // this.outer.outer.funInM()
        MultipleNestings.this.funInM(); // this.outer.outer.funInM()
        funInI1(); // this.outer.funInI1()
        InnerClass1.this.funInI1(); // this.outer.funInI1()

        funInI2(); // this.funInI2();
      }
    }
  }
}
