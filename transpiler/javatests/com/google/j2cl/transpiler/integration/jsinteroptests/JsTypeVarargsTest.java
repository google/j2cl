package com.google.j2cl.transpiler.integration.jsinteroptests;

import static jsinterop.annotations.JsPackage.GLOBAL;

import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsType;

public class JsTypeVarargsTest extends MyTestCase {
  @JsMethod
  public static native int varargsMethod1(Object... varargs);

  @JsMethod
  public static int varargsMethod2(Object... varargs) {
    return varargs.length;
  }

  @JsMethod
  public static Object varargsMethod3(int slot, Object... varargs) {
    return varargs[slot];
  }

  @JsMethod
  public static Object[] varargsMethod4(int slot, Object... varargs) {
    varargs[slot] = null;
    return varargs;
  }

  @JsMethod
  private static native Object callVarargsMethod3FromJSNI();

  @JsType(isNative = true, namespace = GLOBAL, name = "Object")
  static class NativeJsType {}

  @JsType(
    isNative = true,
    namespace = "test.foo",
    name = "JsTypeVarargsTest_MyNativeJsTypeVarargsConstructor"
  )
  static class NativeJsTypeWithVarargsConstructor {
    public Object a;
    public int b;

    NativeJsTypeWithVarargsConstructor(int i, Object... args) {}
  }

  static class SubNativeWithVarargsConstructor extends NativeJsTypeWithVarargsConstructor {
    SubNativeWithVarargsConstructor(String s, Object... args) {
      this(1, args[0], args[1], null);
    }

    SubNativeWithVarargsConstructor(int i, Object... args) {
      super(i, args);
    }

    @JsMethod
    Object varargsMethod(int i, Object... args) {
      return args[i];
    }
  }

  static class SubSubNativeWithVarargsConstructor extends SubNativeWithVarargsConstructor {
    SubSubNativeWithVarargsConstructor() {
      super(0, new Object());
    }

    @Override
    Object varargsMethod(int i, Object... args) {
      return super.varargsMethod(i, args);
    }

    Object nonJsVarargsMethod() {
      return super.varargsMethod(1, null, this);
    }
  }

  public void testVarargsCall_regularMethods() {
    // pass multiple args
    assertEquals(3, varargsMethod1("A", "B", "C"));
    assertEquals(4, varargsMethod2("A", "B", "C", "D"));
    // pass array literal
    assertEquals(2, varargsMethod1(new NativeJsType[] {null, null}));
    assertEquals(5, varargsMethod2(new NativeJsType[] {null, null, null, null, null}));
    Object[] o1 = new Object[] {"A", "B", "C"};
    Object[] o2 = new Object[] {"A", "B", "C", "D"};
    // pass array instance
    assertEquals(3, varargsMethod1("A", "B", "C"));
    assertEquals(4, varargsMethod2("A", "B", "C", "D"));
    // pass no args
    assertEquals(0, varargsMethod1());
    assertEquals(0, varargsMethod2());
    // pass empty array literal
    assertEquals(0, varargsMethod1(new Object[] {}));
    assertEquals(0, varargsMethod2(new Object[] {}));
    // pass empty array instance
    Object[] o3 = new Object[] {"A", "B", "C"};
    Object[] o4 = new Object[] {"A", "B", "C", "D"};
    assertEquals(3, varargsMethod1(o3));
    assertEquals(4, varargsMethod2(o4));

    assertEquals("C", varargsMethod3(2, "A", "B", "C", "D"));
    assertEquals("3", callVarargsMethod3FromJSNI());
    assertNull(varargsMethod4(1, "A", "B", "C")[1]);
    assertEquals("A", varargsMethod4(1, "A", "B", "C")[0]);
    assertEquals(3, varargsMethod4(1, "A", "B", "C").length);
  }

  public void testVarargsCall_constructors() {
    NativeJsType someNativeObject = new NativeJsType();
    NativeJsTypeWithVarargsConstructor object =
        new NativeJsTypeWithVarargsConstructor(1, someNativeObject, null);

    assertSame(someNativeObject, object.a);
    assertEquals(3, object.b);

    Object[] params = new Object[] {someNativeObject, null};
    object = new NativeJsTypeWithVarargsConstructor(1, params);

    assertSame(someNativeObject, object.a);
    assertEquals(3, object.b);

    object = new SubNativeWithVarargsConstructor("", someNativeObject, null);

    assertSame(someNativeObject, object.a);
    assertEquals(4, object.b);

    object = new SubNativeWithVarargsConstructor(1, someNativeObject, null);

    assertSame(someNativeObject, object.a);
    assertEquals(3, object.b);
  }

  @JsMethod
  public static Double sumAndMultiply(Double multiplier, Double... numbers) {
    double result = 0.0d;
    for (double d : numbers) {
      result += d;
    }
    result *= multiplier;
    return result;
  }

  @JsMethod
  public static int sumAndMultiplyInt(int multiplier, int... numbers) {
    int result = 0;
    for (int d : numbers) {
      result += d;
    }
    result *= multiplier;
    return result;
  }

  @JsFunction
  interface Function {
    Object f(int i, Object... args);
  }

  static class AFunction implements Function {

    @Override
    public Object f(int i, Object... args) {
      return args[i];
    }

    static Function create() {
      return new AFunction();
    }
  }

  public void testVarargsCall_fromJavaScript() {
    assertEquals(60, callSumAndMultiply());
    assertEquals(30, callSumAndMultiplyInt());
    Function f = AFunction.create();
    assertSame(f, callAFunction(f));
  }

  @JsMethod
  private static native int callSumAndMultiply();

  @JsMethod
  private static native int callSumAndMultiplyInt();

  @JsMethod
  private static native Object callAFunction(Object obj);

  public void testVarargsCall_jsFunction() {
    Function function = new AFunction();
    assertSame(function, function.f(2, null, null, function, null));
    assertSame(null, function.f(1, null, null, function, null));
  }

  public void testVarargsCall_superCalls() {
    SubSubNativeWithVarargsConstructor object = new SubSubNativeWithVarargsConstructor();
    assertSame(object, object.nonJsVarargsMethod());
    assertSame(object, object.varargsMethod(1, null, object, null));
  }

  private static int sideEffectCount;

  private SubNativeWithVarargsConstructor doSideEffect(SubNativeWithVarargsConstructor obj) {
    sideEffectCount++;
    return obj;
  }

  public void testVarargsCall_sideEffectingInstance() {
    Object arg = new Object();
    SubNativeWithVarargsConstructor object = new SubNativeWithVarargsConstructor(0, arg);
    sideEffectCount = 0;
    Object[] params = new Object[] {object, null};
    assertSame(object, doSideEffect(object).varargsMethod(0, params));
    assertSame(1, sideEffectCount);
  }
}
