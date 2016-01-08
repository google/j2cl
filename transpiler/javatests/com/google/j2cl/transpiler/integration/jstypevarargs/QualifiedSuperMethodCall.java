package com.google.j2cl.transpiler.integration.jstypevarargs;

public class QualifiedSuperMethodCall extends Main {
  public QualifiedSuperMethodCall() {
    super(1);
  }

  public class InnerClass {
    public int test1() {
      // multiple arguments.
      return QualifiedSuperMethodCall.super.f3(1, 1, 2);
    }

    public int test2() {
      // no argument for varargs.
      return QualifiedSuperMethodCall.super.f3(1);
    }

    public int test3() {
      // array literal for varargs.
      return QualifiedSuperMethodCall.super.f3(1, new int[] {1, 2});
    }

    public int test4() {
      // empty array literal for varargs.
      return QualifiedSuperMethodCall.super.f3(1, new int[] {});
    }

    public int test5() {
      // array object for varargs.
      int[] ints = new int[] {1, 2};
      return QualifiedSuperMethodCall.super.f3(1, ints);
    }
  }

  public static void test() {
    InnerClass i = new QualifiedSuperMethodCall().new InnerClass();
    assert i.test1() == 4;
    assert i.test2() == 1;
    assert i.test3() == 4;
    assert i.test4() == 1;
    assert i.test5() == 4;
  }
}
