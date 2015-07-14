package com.google.j2cl.transpiler.integration.instanceinnerclass;

/**
 * Test instance inner class.
 */
class OuterClass {
  public int fieldInOuter;
  public OuterClass(int f) {
    this.fieldInOuter = f;
  }
  class InnerClass {
    public int fieldInInner = fieldInOuter * 2;
  }
}

public class Main {
  public int fieldInMain;
  public Main(int f) {
    this.fieldInMain = f;
  }

  /**
   * Basic instance inner class.
   */
  public class A {
    public int fieldInA = fieldInMain * 2;
    public Main enclosingInstance = Main.this;
    public int fun() {
      return fieldInA * 3;
    }
  }

  /**
   * Instance inner class with constructors and this() call.
   */
  public class B {
    public int fieldInB;
    public Main fieldMain;
    public B(int x, Main m) {
      fieldInB = x;
      fieldMain = m;
    }
    public B() {
      this(fieldInMain, Main.this);
    }
    public Main getBOuter() {
      return Main.this;
    }
  }

  /**
   * Two level nested inner class
   */
  public class C {
    public class CC {
      public C fieldOfC = C.this;
      public Main fieldOfMain = Main.this;
    }
  }

  /**
   * Test inner class whose parent is also an inner class, and shares the same enclosing instance.
   */
  public class Child extends B {
    public int fieldInChild;
    public Child(int x, int y) {
      super(x, new Main(y));
      fieldInChild = fieldInB + fieldMain.fieldInMain; //x + y
    }

    public Child() {
      fieldInChild = fieldInB + fieldMain.fieldInMain; // fieldInMain * 2;
    }
  }

  /**
   * Test inner class whose parent is also an inner class, and shares the same enclosing instance,
   * but the super() call is called by another instance.
   */
  public class Child2 extends B {
    public Child2() {
      new Main(30).super();
    }
    public Main getChild2Outer() {
      return Main.this;
    }
  }

  /**
   * Test inner class without any constructors, and whose parent is also an inner class, 
   * and they share the same enclosing instance.
   */
  public class D extends B {
    public int fieldInD = fieldMain.fieldInMain;
  }

  /**
   * Test inner class whose parent is also an inner class, and does not the same enclosing instance.
   */
  public class AnotherChild extends OuterClass.InnerClass {
    public int fieldInChild;
    public AnotherChild() {
      new OuterClass(10).super();
      fieldInChild = fieldInInner * 3; //10 * 2 * 3 = 60;
    }
  }

  public static void main(String[] args) {
    Main m = new Main(2);
    assert m.new A().fun() == 12;
    assert m.new A().enclosingInstance == m;

    Main mm = new Main(20);
    B b = mm.new B();
    B bb = mm.new B(1, m);
    assert b.fieldInB == 20;
    assert b.fieldMain == mm;
    assert bb.fieldInB == 1;
    assert bb.fieldMain == m;

    C c = m.new C();
    assert c.new CC().fieldOfC == c;
    assert c.new CC().fieldOfMain == m;

    assert m.new D().fieldInD == 2;
    assert m.new Child().fieldInChild == 4;
    assert m.new Child(10, 20).fieldInChild == 30;
    assert m.new AnotherChild().fieldInChild == 60;

    Child2 c2 = m.new Child2();
    assert c2.getChild2Outer().fieldInMain == 2;
    assert c2.getBOuter().fieldInMain == 30;
  }
}
