package com.google.j2cl.transpiler.integration.jstypevarargs;

import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsMethod;

public class Main {
  @JsFunction
  static interface Function {
    int f1(int i, Main... args);
  }

  static final class AFunction implements Function {

    @Override
    public int f1(int i, Main... args) {
      if (i >= args.length) {
        return -1;
      }
      return args[i] == null ? -1 : args[i].field;
    }
  }

  static class SubMain extends Main {
    public SubMain(int x, int y) {
      super(x + y);
    }

    // multiple arguments.
    public int test1() {
      return super.f3(10, 1, 2);
    }

    // no argument for varargs.
    public int test2() {
      return super.f3(10);
    }

    // array literal for varargs.
    public int test3() {
      return super.f3(10, new int[] {1, 2});
    }

    // empty array literal for varargs.
    public int test4() {
      return super.f3(10, new int[] {});
    }

    // array object for varargs.
    public int test5() {
      int[] ints = new int[] {1, 2};
      return super.f3(10, ints);
    }
  }

  int field;

  public Main(int f) {
    this.field = f;
  }

  public Main sideEffect(int a) {
    this.field += a;
    return this;
  }

  // varargs of unboxed type.
  @JsMethod
  public static Double sumAndMultiply(Double multiplier, Double... numbers) {
    double result = 0.0d;
    for (double d : numbers) {
      result += d;
    }
    result *= multiplier;
    return result;
  }

  // static JsMethod, with varargs that is not the first parameter.
  @JsMethod
  public static int f1(int m, int... numbers) {
    int result = 0;
    for (int n : numbers) {
      result += n;
    }
    return result *= m;
  }

  // static JsMethod, with varargs that is the first parameter.
  @JsMethod
  public static int f2(int... numbers) {
    int result = 0;
    for (int n : numbers) {
      result += n;
    }
    return result *= 100;
  }

  // instance JsMethod, with varargs that is not the first parameter.
  @JsMethod
  public int f3(int m, int... numbers) {
    int result = 0;
    for (int n : numbers) {
      result += n;
    }
    result += field;
    return result *= m;
  }

  // instance JsMethod, with varargs that is the first parameter.
  @JsMethod
  public int f4(int... numbers) {
    int result = 0;
    for (int n : numbers) {
      result += n;
    }
    result += field;
    return result *= 100;
  }

  @JsMethod
  public static <T> T generics(T... elements) {
    return elements[0];
  }

  public static void testUnboxedType() {
    // multiple arguments.
    assert sumAndMultiply(10.0, 1.0, 2.0) == 30.0;
    assert Main.sumAndMultiply(10.0, 1.0, 2.0) == 30;
    // no argument for varargs.
    assert sumAndMultiply(10.0) == 0;
    assert Main.sumAndMultiply(10.0) == 0;
    // array literal for varargs.
    assert sumAndMultiply(10.0, new Double[] {1.0, 2.0}) == 30;
    assert Main.sumAndMultiply(10.0, new Double[] {1.0, 2.0}) == 30;
    // empty array literal for varargs.
    assert sumAndMultiply(10.0, new Double[] {}) == 0;
    assert Main.sumAndMultiply(10.0, new Double[] {}) == 0;
    // array object for varargs.
    Double[] ds = new Double[] {1.0, 2.2};
    assert sumAndMultiply(10.0, ds) == 30.0;
    assert Main.sumAndMultiply(10.0, ds) == 30.0;
    // call by JS.
    assert callSumAndMultiply() == 30.0;
  }

  public static void testStaticMethodNotFirst() {
    // multiple arguments.
    assert f1(10, 1, 2) == 30;
    assert Main.f1(10, 1, 2) == 30;
    // no argument for varargs.
    assert f1(10) == 0;
    assert Main.f1(10) == 0;
    // array literal for varargs.
    assert f1(10, new int[] {1, 2}) == 30;
    assert Main.f1(10, new int[] {1, 2}) == 30;
    // empty array literal for varargs.
    assert f1(10, new int[] {}) == 0;
    assert Main.f1(10, new int[] {}) == 0;
    // array object for varargs.
    int[] ints = new int[] {1, 2};
    assert f1(10, ints) == 30;
    assert Main.f1(10, ints) == 30;
    // call by JS.
    assert callF1() == 30;
  }

  public static void testStaticMethodFirst() {
    // multiple arguments.
    assert f2(1, 2) == 300;
    assert Main.f2(1, 2) == 300;
    // no argument for varargs.
    assert f2() == 0;
    assert Main.f2() == 0;
    // array literal for varargs.
    assert f2(new int[] {1, 2}) == 300;
    assert Main.f2(new int[] {1, 2}) == 300;
    // empty array literal for varargs.
    assert f2(new int[] {}) == 0;
    assert Main.f2(new int[] {}) == 0;
    // array object for varargs.
    int[] ints = new int[] {1, 2};
    assert f2(ints) == 300;
    assert Main.f2(ints) == 300;
    // call by JS.
    assert callF2() == 300;
    assert 1 == Main.generics(1, 2);
    assert "abc".equals(Main.generics("abc", "def"));
  }

  public static void testInstanceMethodNotFirst() {
    Main m = new Main(1);
    // multiple arguments.
    assert m.f3(10, 1, 2) == 40;
    // no argument for varargs.
    assert m.f3(10) == 10;
    // array literal for varargs.
    assert m.f3(10, new int[] {1, 2}) == 40;
    // empty array literal for varargs.
    assert m.f3(10, new int[] {}) == 10;
    // array object for varargs.
    int[] ints = new int[] {1, 2};
    assert m.f3(10, ints) == 40;
    // call by JS.
    assert callF3(m) == 40;
  }

  public static void testInstanceMethodFirst() {
    Main m = new Main(1);
    // multiple arguments.
    assert m.f4(1, 2) == 400;
    // no argument for varargs.
    assert m.f4() == 100;
    // array literal for varargs.
    assert m.f4(new int[] {1, 2}) == 400;
    // empty array literal for varargs.
    assert m.f4(new int[] {}) == 100;
    // array object for varargs.
    int[] ints = new int[] {1, 2};
    assert m.f4(ints) == 400;
    // call by JS.
    assert callF4(m) == 400;
  }

  public static void testJsFunction() {
    AFunction a = new AFunction();
    Main m1 = new Main(12);
    Main m2 = new Main(34);
    // multiple arguments.
    assert a.f1(1, m1, m2) == 34;
    // no argument for varargs.
    assert a.f1(1) == -1;
    // array literal for varargs.
    assert a.f1(0, new Main[] {m1, m2}) == 12;
    // empty array literal for varargs.
    assert a.f1(0, new Main[] {}) == -1;
    // array object for varargs.
    Main[] os = new Main[] {m1, m2};
    assert a.f1(1, os) == 34;
    // call by JS
    assert callJsFunction(a) == -1;
  }

  public static void testSideEffect() {
    Main m = new Main(10);
    assert m.field == 10;
    int[] ints = new int[] {1, 2};
    m.sideEffect(5).f3(10, ints);
    assert m.field == 15;
  }

  public static void testSuperMethodCall() {
    SubMain sm = new SubMain(1, 0);
    assert sm.test1() == 40;
    assert sm.test2() == 10;
    assert sm.test3() == 40;
    assert sm.test4() == 10;
    assert sm.test5() == 40;
  }
  
  @JsMethod
  private static int count(String... strings) {
    return strings.length;
  }

  public static void testCallVarargsWithNull() {
    assert count("Hello") == 1;
    try {
      String[] strings = null;
      assert count(strings) == 0;
      assert false;
    } catch (Exception expected) {
      return;
    }
    assert false;
  }

  @JsMethod
  private static native Double callSumAndMultiply();

  @JsMethod
  private static native int callF1();

  @JsMethod
  private static native int callF2();

  @JsMethod
  private static native int callF3(Main m);

  @JsMethod
  private static native int callF4(Main m);

  @JsMethod
  private static native int callJsFunction(AFunction a);

  public static void main(String... args) {
    testInstanceMethodFirst();
    testInstanceMethodNotFirst();
    testStaticMethodFirst();
    testStaticMethodNotFirst();
    testJsFunction();
    testSideEffect();
    testSuperMethodCall();
    testCallVarargsWithNull();
    QualifiedSuperMethodCall.test();
  }
}
