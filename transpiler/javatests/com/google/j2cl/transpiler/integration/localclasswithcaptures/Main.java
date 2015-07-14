package com.google.j2cl.transpiler.integration.localclasswithcaptures;

/**
 * Test local classes with captured variables.
 */
public class Main {
  public int testSimple(final int p) {
    final int localVar = 1;
    class InnerClass {
      public int fun() {
        class InnerInnerClass {
          public int bar() {
            return localVar + p;
          }
        }
        return new InnerInnerClass().bar(); // localVar + p
      }
    }
    return new InnerClass().fun() + p; // localVar + p + p
  }

  public int testConstructor(final int p) {
    final int localVar = 1;
    // local class with non-default constructor and this() call
    class LocalClassWithConstructor {
      public int field;

      public LocalClassWithConstructor(int a, int b) {
        field = localVar + a + b;
      }

      public LocalClassWithConstructor(int a) {
        this(a, p);
      }
    }
    int first = new LocalClassWithConstructor(1, 2).field; // 1 + 1 + 2 = 4
    int second = new LocalClassWithConstructor(3).field; // 1 + 3 + p;
    return first + second; // 8 + p;
  }

  public int testClassWithSameName() {
    final int a = 10;
    class InnerClass {
      public int test(int b) {
        return a + b;
      }
    }
    return new InnerClass().test(20); // 30
  }

  public int testClassWithParent() {
    final int a = 10;
    class ParentClass {
      int fun(int p) {
        return a + p;
      }
    }

    // local classes with/without explicit super() whose parent is also a local class
    class ChildClass extends ParentClass {}

    class AnotherChild extends ParentClass {
      public AnotherChild() {
        super();
      }
    }

    return new ChildClass().fun(20) + new AnotherChild().fun(100); // 30 + 110 = 140;
  }

  public static void main(String[] args) {
    Main m = new Main();
    assert m.testSimple(100) == 201;
    assert m.testConstructor(100) == 108;
    assert m.testClassWithSameName() == 30;
    assert m.testClassWithParent() == 140;
  }
}
