/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jstypevarargs;

import static com.google.j2cl.integration.testing.Asserts.assertEquals;
import static com.google.j2cl.integration.testing.Asserts.assertThrowsNullPointerException;
import static com.google.j2cl.integration.testing.Asserts.assertTrue;
import static com.google.j2cl.integration.testing.TestUtils.isWasm;
import static jsinterop.annotations.JsPackage.GLOBAL;
import static jstypevarargs.Main.Varargs.f1;
import static jstypevarargs.Main.Varargs.f2;
import static jstypevarargs.Main.Varargs.sumAndMultiply;

import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsType;

public class Main {
  public static void main(String... args) {
    testInstanceMethodFirst();
    testInstanceMethodNotFirst();
    testStaticMethodFirst();
    testStaticMethodNotFirst();
    testJsFunction();
    testSideEffect();
    testSuperMethodCall();
    testCallVarargsWithNull();
    testUnboxedType();
    testVarargsBridges();
    testOverloadedNativeVarargs();
    QualifiedSuperMethodCall.test();
  }

  @JsFunction
  interface Function {
    int f1(int i, Varargs... args);
  }

  static final class AFunction implements Function {

    @Override
    public int f1(int i, Varargs... args) {
      if (i >= args.length) {
        return -1;
      }
      return args[i] == null ? -1 : args[i].field;
    }
  }

  static class Varargs {
    int field;

    public Varargs(int f) {
      this.field = f;
    }

    public Varargs sideEffect(int a) {
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
  }

  static class SubMain extends Varargs {
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

  private static void testUnboxedType() {
    // multiple arguments.
    assertTrue(sumAndMultiply(10.0, 1.0, 2.0) == 30.0);
    assertTrue(Varargs.sumAndMultiply(10.0, 1.0, 2.0) == 30);
    // no argument for varargs.
    assertTrue(sumAndMultiply(10.0) == 0);
    assertTrue(Varargs.sumAndMultiply(10.0) == 0);
    // array literal for varargs.
    assertTrue(sumAndMultiply(10.0, new Double[] {1.0, 2.0}) == 30);
    assertTrue(Varargs.sumAndMultiply(10.0, new Double[] {1.0, 2.0}) == 30);
    // empty array literal for varargs.
    assertTrue(sumAndMultiply(10.0, new Double[] {}) == 0);
    assertTrue(Varargs.sumAndMultiply(10.0, new Double[] {}) == 0);
    // array object for varargs.
    Double[] ds = new Double[] {1.0, 2.2};
    assertTrue(sumAndMultiply(10.0, ds) == 32.0);
    assertTrue(Varargs.sumAndMultiply(10.0, ds) == 32.0);
    // call by JS.
    if (!isWasm()) {
      assertTrue(callSumAndMultiply() == 30.0);
    }
  }

  private static void testStaticMethodNotFirst() {
    // multiple arguments.
    assertTrue(f1(10, 1, 2) == 30);
    assertTrue(Varargs.f1(10, 1, 2) == 30);
    // no argument for varargs.
    assertTrue(f1(10) == 0);
    assertTrue(Varargs.f1(10) == 0);
    // array literal for varargs.
    assertTrue(f1(10, new int[] {1, 2}) == 30);
    assertTrue(Varargs.f1(10, new int[] {1, 2}) == 30);
    // empty array literal for varargs.
    assertTrue(f1(10, new int[] {}) == 0);
    assertTrue(Varargs.f1(10, new int[] {}) == 0);
    // array object for varargs.
    int[] ints = new int[] {1, 2};
    assertTrue(f1(10, ints) == 30);
    assertTrue(Varargs.f1(10, ints) == 30);
    // call by JS.
    if (!isWasm()) {
      assertTrue(callF1() == 30);
    }
  }

  private static void testStaticMethodFirst() {
    // multiple arguments.
    assertTrue(f2(1, 2) == 300);
    assertTrue(Varargs.f2(1, 2) == 300);
    // no argument for varargs.
    assertTrue(f2() == 0);
    assertTrue(Varargs.f2() == 0);
    // array literal for varargs.
    assertTrue(f2(new int[] {1, 2}) == 300);
    assertTrue(Varargs.f2(new int[] {1, 2}) == 300);
    // empty array literal for varargs.
    assertTrue(f2(new int[] {}) == 0);
    assertTrue(Varargs.f2(new int[] {}) == 0);
    // array object for varargs.
    int[] ints = new int[] {1, 2};
    assertTrue(f2(ints) == 300);
    assertTrue(Varargs.f2(ints) == 300);
    // call by JS.
    if (!isWasm()) {
      assertTrue(callF2() == 300);
    }
    assertTrue(1 == Varargs.generics(1, 2));
    assertTrue("abc".equals(Varargs.generics("abc", "def")));
  }

  private static void testInstanceMethodNotFirst() {
    Varargs m = new Varargs(1);
    // multiple arguments.
    assertTrue(m.f3(10, 1, 2) == 40);
    // no argument for varargs.
    assertTrue(m.f3(10) == 10);
    // array literal for varargs.
    assertTrue(m.f3(10, new int[] {1, 2}) == 40);
    // empty array literal for varargs.
    assertTrue(m.f3(10, new int[] {}) == 10);
    // array object for varargs.
    int[] ints = new int[] {1, 2};
    assertTrue(m.f3(10, ints) == 40);
    // call by JS.
    if (!isWasm()) {
      assertTrue(callF3(m) == 40);
    }
  }

  private static void testInstanceMethodFirst() {
    Varargs m = new Varargs(1);
    // multiple arguments.
    assertTrue(m.f4(1, 2) == 400);
    // no argument for varargs.
    assertTrue(m.f4() == 100);
    // array literal for varargs.
    assertTrue(m.f4(new int[] {1, 2}) == 400);
    // empty array literal for varargs.
    assertTrue(m.f4(new int[] {}) == 100);
    // array object for varargs.
    int[] ints = new int[] {1, 2};
    assertTrue(m.f4(ints) == 400);
    // call by JS.
    if (!isWasm()) {
      assertTrue(callF4(m) == 400);
    }
  }

  private static void testJsFunction() {
    AFunction a = new AFunction();
    Varargs m1 = new Varargs(12);
    Varargs m2 = new Varargs(34);
    // multiple arguments.
    assertTrue(a.f1(1, m1, m2) == 34);
    // no argument for varargs.
    assertTrue(a.f1(1) == -1);
    // array literal for varargs.
    assertTrue(a.f1(0, new Varargs[] {m1, m2}) == 12);
    // empty array literal for varargs.
    assertTrue(a.f1(0, new Varargs[] {}) == -1);
    // array object for varargs.
    Varargs[] os = new Varargs[] {m1, m2};
    assertTrue(a.f1(1, os) == 34);
    // call by JS
    if (!isWasm()) {
      assertTrue(callJsFunction(a) == -1);
    }
  }

  private static void testSideEffect() {
    Varargs m = new Varargs(10);
    assertTrue(m.field == 10);
    int[] ints = new int[] {1, 2};
    m.sideEffect(5).f3(10, ints);
    assertTrue(m.field == 15);
  }

  private static void testSuperMethodCall() {
    SubMain sm = new SubMain(1, 0);
    assertTrue(sm.test1() == 40);
    assertTrue(sm.test2() == 10);
    assertTrue(sm.test3() == 40);
    assertTrue(sm.test4() == 10);
    assertTrue(sm.test5() == 40);
  }

  @JsMethod
  private static int count(String... strings) {
    return strings.length;
  }

  private static void testCallVarargsWithNull() {
    assertTrue(count("Hello") == 1);
    String[] strings = null;
    assertThrowsNullPointerException(() -> count(strings));
  }

  private static class IntegerSummarizer {
    public int summarize(Integer... values) {
      int sum = 0;
      for (int value : values) {
        sum += value;
      }
      return sum;
    }
  }

  interface Summarizer<T> {
    @JsMethod
    int summarize(T... values);
  }

  private static class SummarizerImplementor extends IntegerSummarizer
      implements Summarizer<Integer> {}

  private static void testBridgeWithVarargs() {
    assertEquals(6, new SummarizerImplementor().summarize(1, 2, 3));
    Summarizer rawSummarizer = new SummarizerImplementor();
    // Simulate a call from JS with an untyped array.
    assertEquals(6, rawSummarizer.summarize(1, 2, 3));
  }

  // Repro for b/587851444.
  interface VarargsBridges<T, S> {
    @JsMethod
    String arrayOverriddenByVarargs(T[] values);

    @JsMethod
    String varargsOverriddenByArray(T... values);

    @JsMethod
    String genericOverriddenByVarargs(S values);
  }

  private static class VarargsBridgesImpl implements VarargsBridges<String, String[]> {
    @Override
    public String arrayOverriddenByVarargs(String... values) {
      return values[0] + ":" + values.length;
    }

    @Override
    public String varargsOverriddenByArray(String[] values) {
      return values[0] + ":" + values.length;
    }

    @Override
    public String genericOverriddenByVarargs(String... values) {
      return values[0] + ":" + values.length;
    }
  }

  private static void testVarargsBridges() {
    VarargsBridges<String, String[]> bridges = new VarargsBridgesImpl();
    assertEquals("a:2", bridges.arrayOverriddenByVarargs(new String[] {"a", "b"}));
    assertEquals("a:2", bridges.varargsOverriddenByArray("a", "b"));
    assertEquals("a:2", bridges.genericOverriddenByVarargs(new String[] {"a", "b"}));
  }

  @JsMethod(namespace = "jstypevarargs.helper")
  private static native Double callSumAndMultiply();

  @JsMethod(namespace = "jstypevarargs.helper")
  private static native int callF1();

  @JsMethod(namespace = "jstypevarargs.helper")
  private static native int callF2();

  @JsMethod(namespace = "jstypevarargs.helper")
  private static native int callF3(Varargs m);

  @JsMethod(namespace = "jstypevarargs.helper")
  private static native int callF4(Varargs m);

  @JsMethod(namespace = "jstypevarargs.helper")
  private static native int callJsFunction(AFunction a);

  @JsType(isNative = true, namespace = GLOBAL, name = "Object")
  private static class NativeObj {}

  @JsMethod(namespace = "jstypevarargs.helper", name = "countArgs")
  private static native int countArgs(NativeObj a);

  @JsMethod(namespace = "jstypevarargs.helper", name = "countArgs")
  private static native int countArgs(NativeObj... values);

  @JsMethod(namespace = "jstypevarargs.helper", name = "countArgs")
  private static native int countArgs(int prefix, NativeObj... rest);

  private static void testOverloadedNativeVarargs() {
    NativeObj o = new NativeObj();
    assertEquals(1, countArgs(o));
    assertEquals(3, countArgs(o, o, o));
    assertEquals(3, countArgs(42, o, o));
  }
}
