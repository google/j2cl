package com.google.j2cl.transpiler.readable.localclassinstaticcontext;

public class LocalClassInStaticContext {
  public static Object f = new Object() {}; // Initializer in a static field declaration.

  public static void test() {
    // In a static function.
    class A {}
    new A();
    Object a = new Object() {};
  }

  static {
    // In a static block.
    class B {}
    new B();
    Object b = new Object() {};
  }

  public static class C {
    public int f = 1;

    public void test() {
      // It should have an enclosing instance, although its outer class is static.
      class D {
        public int fun() {
          return f;
        }
      }
      new D().fun();
    }
  }
}
