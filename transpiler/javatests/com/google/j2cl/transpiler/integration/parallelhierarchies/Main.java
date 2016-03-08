package com.google.j2cl.transpiler.integration.parallelhierarchies;

/**
 * Tests behavior of nested inner classes with parallel class hierarchies.
 */
public class Main {

  /**
   * One set of inner classes with explicit constructors.
   */
  public static class ExplicitConstructors {
    public class Outer1 {
      public Outer1() { }
      public Outer1(int i) { }
      public class Inner1 {
        public Inner1() { }
        public Inner1(int i) { }
      }
    }

    public class Outer2 extends Outer1 {
      public Outer2() {
        super();
      }
      public Outer2(int i) {
        super(i);
      }
      public class Inner2 extends Inner1 {
        public Inner2() {
          super();
        }
        public Inner2(int i) {
          super(i);
        }
      }
    }
  }

  /**
   * Another set of inner classes with implicit constructors.
   */
  public static class ImplicitConstructors {
    public class Outer1 {
      public class Inner1 { }
    }

    public class Outer2 extends Outer1 {
      public class Inner2 extends Inner1 { }
    }
  }

  /**
   * Another set with qualified calls to outer class fields.
   */
  public static class QualifiedCalls {
    public String name = "QualifiedCalls";

    public class Outer1 {
      public String name = "Outer1";
      public class Inner1 {
        public String name = "Inner1";
        public String superResult;
        public Inner1() {
          superResult = this.name + Outer1.this.name + QualifiedCalls.this.name;
        }
      }
    }

    public class Outer2 extends Outer1 {
      public String name = "Outer2";
      public class Inner2 extends Inner1 {
        public String name = "Inner2";
        public String result;
        public Inner2() {
          result = this.name + Outer2.this.name + QualifiedCalls.this.name + superResult;
        }
      }
    }
  }

  public static void main(String... args) {
    QualifiedCalls.Outer2.Inner2 test = new QualifiedCalls().new Outer2().new Inner2();
    assert "Inner2Outer2QualifiedCallsInner1Outer1QualifiedCalls".equals(test.result);
  }
}
