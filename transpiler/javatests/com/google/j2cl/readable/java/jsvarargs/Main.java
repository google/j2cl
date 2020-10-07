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
package jsvarargs;

import java.util.List;
import jsinterop.annotations.JsConstructor;
import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

public class Main {

  @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
  static class NativeObject {
    public NativeObject(Object... pars) {}
  }

  static class SubVarargsConstructorClass extends NativeObject {
    @JsConstructor
    public SubVarargsConstructorClass(int i, Object... args) {
      super(args);
    }
  }

  @JsFunction
  interface Function {
    Object f1(int i, Object... args);
  }

  @JsType
  public abstract static class AbstractMethodWithVarargs {
    public abstract void abstractMethod(int... args);
  }

  @JsType
  public interface StaticInterfaceMethodWithVarargs {
    static void staticMethod(int... args) {}
  }

  static final class AFunction implements Function {

    @Override
    public Object f1(int i, Object... args) {
      return args[i];
    }
  }

  static class SubMain extends Main {
    public SubMain() {
      super(10);
    }

    @Override
    public int f3(int m, int... numbers) {
      // multiple arguments.
      int a = super.f3(1, 1, 2);
      // no argument for varargs.
      a += super.f3(1);
      // array literal for varargs.
      a += super.f3(1, new int[] {1, 2});
      // empty array literal for varargs.
      a += super.f3(1, new int[] {});
      // array object for varargs.
      int[] ints = new int[] {1, 2};
      a += super.f3(1, ints);
      return a;
    }
  }

  int field;

  public Main(int f) {
    this.field = f;
  }

  // static JsMethod, with varargs that is not the first parameter.
  @JsMethod
  public static int f1(int multiplier, int... numbers) {
    return numbers.length + numbers[0] + multiplier;
  }

  // static JsMethod, with varargs that is the first parameter.
  @JsMethod
  public static int f2(int... numbers) {
    return numbers.length + numbers[0];
  }

  // instance JsMethod, with varargs that is not the first parameter.
  @JsMethod
  public int f3(int m, int... numbers) {
    return this.field + m + numbers[1];
  }

  // instance JsMethod, with varargs that is the first parameter.
  @JsMethod
  public int f4(int... numbers) {
    return this.field + numbers[1];
  }

  @JsMethod
  public static <T> T generics(T... elements) {
    return elements[0];
  }

  @JsMethod
  public static Main parameterizedType(List<Main>... elements) {
    return elements[0].get(0);
  }

  @JsMethod
  public static <T> T parameterizedByT(List<T>... elements) {
    return elements[0].get(0);
  }

  public void testStaticMethodNotFirst() {
    // multiple arguments.
    f1(1, 1, 2);
    Main.f1(1, 1, 2);
    // no argument for varargs.
    f1(1);
    Main.f1(1);
    // array literal for varargs.
    f1(1, new int[] {1, 2});
    Main.f1(1, new int[] {1, 2});
    // empty array literal for varargs.
    f1(1, new int[] {});
    Main.f1(1, new int[] {});
    // array object for varargs.
    int[] ints = new int[] {1, 2};
    f1(1, ints);
    Main.f1(1, ints);
    Main.f1(1, null);
  }

  public void testStaticMethodFirst() {
    // multiple arguments.
    f2(1, 2);
    generics(1, 2);
    Main.f2(1, 2);
    Main.generics(1, 2);
    // no argument for varargs.
    f2();
    generics();
    Main.f2();
    Main.generics();
    Main.<Integer>generics();
    // array literal for varargs.
    f2(new int[] {1, 2});
    Main.f2(new int[] {1, 2});
    Main.generics(new int[] {1, 2});
    Main.<Integer>generics(new Integer[] {1, 2});
    // empty array literal for varargs.
    f2(new int[] {});
    Main.f2(new int[] {});
    Main.generics(new int[] {});
    // array object for varargs.
    int[] ints = new int[] {1, 2};
    Integer[] integers = new Integer[] {1, 2};
    f2(ints);
    Main.f2(ints);
    Main.<Integer>generics(integers);
  }

  public void testInstanceMethodNotFirst() {
    Main m = new Main(1);
    // multiple arguments.
    m.f3(1, 1, 2);
    // no argument for varargs.
    m.f3(1);
    // array literal for varargs.
    m.f3(1, new int[] {1, 2});
    // empty array literal for varargs.
    m.f3(1, new int[] {});
    // array object for varargs.
    int[] ints = new int[] {1, 2};
    m.f3(1, ints);
  }

  public void testInstanceMethodFirst() {
    Main m = new Main(1);
    // multiple arguments.
    m.f4(1, 2);
    // no argument for varargs.
    m.f4();
    // array literal for varargs.
    m.f4(new int[] {1, 2});
    // empty array literal for varargs.
    m.f4(new int[] {});
    // array object for varargs.
    int[] ints = new int[] {1, 2};
    m.f4(ints);
  }

  public void testJsFunction() {
    AFunction a = new AFunction();
    Object o1 = new Object();
    Object o2 = new Object();
    // multiple arguments.
    a.f1(0, o1, o2);
    // no argument for varargs.
    a.f1(0);
    // array literal for varargs.
    a.f1(0, new Object[] {o1, o2});
    // empty array literal for varargs.
    a.f1(0, new Object[] {});
    // array object for varargs.
    Object[] os = new Object[] {o1, o2};
    a.f1(0, os);
  }

  public void testSideEffect() {
    int[] ints = new int[] {1, 2};
    new Main(1).f3(1, ints);
  }

  public void testNullJsVarargs() {
    int[] ints = null;
    Main.f2(ints);
  }

  @JsFunction
  interface GenericFunction<T> {
    Object m(T i, T... args);
  }

  public <U> void testGenericJsFunctionWithVarags() {
    GenericFunction<U> function = (n, param) -> param;
  }
}
