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
package jsfunction;

import java.util.ArrayList;
import java.util.List;
import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsOptional;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

public class Main {
  @JsFunction
  interface Function<T, U> {
    T apply(U u);
  }

  @JsFunction
  interface JsFunctionInterface {
    int foo(int a);

    @JsOverlay
    default int overlayMethod() {
      return foo(42);
    }
  }

  private static final class JsFunctionImplementation implements JsFunctionInterface {
    public int field;
    public JsFunctionImplementation storedThis;
    public JsFunctionImplementation anotherStoredThis;

    JsFunctionImplementation() {
      storedThis = this;
      // Add an explicit unnecessary cast to make jscompiler happy.
      anotherStoredThis = (JsFunctionImplementation) this;
    }

    public int bar() {
      return 0;
    }

    public int fun() {
      field = 1;
      return bar() + foo(1);
    }

    @Override
    public int foo(int a) {
      return a + this.bar() + this.field;
    }
  }

  @JsMethod
  public static JsFunctionInterface createNativeFunction() {
    return null;
  }

  public static int callFn(JsFunctionInterface fn, int a) {
    return fn.foo(a);
  }

  public void testJsFunction() {
    JsFunctionImplementation func = new JsFunctionImplementation();
    // call by java
    func.foo(10);
    // pass Javascript function to java.
    callFn(createNativeFunction(), 10);
    // call other instance methods and fields.
    int a = func.field;
    func.bar();
  }

  public void testJsFunctionsCapturingLocal() {
    final int n = 4;
    // Use number as a variable to make sure it is aliased properly.
    callFn((number) -> number + n, n);
    callFn(
        new JsFunctionInterface() {
          @Override
          public int foo(int a) {
            return a + n;
          }
        },
        n);

    new JsFunctionInterface() {
      @Override
      public int foo(int a) {
        instanceMethod();
        return 0;
      }
    }.foo(3);
  }

  public void testJsFunctionThis() {
    new JsFunctionInterface() {
      @Override
      public int foo(int a) {
        // captures this
        instanceMethod();
        return 0;
      }
    }.foo(3);
  }

  private void instanceMethod() {}

  public void testJsFunctionErasureCasts() {
    List<Function<String, String>> list = new ArrayList<>();
    acceptsJsFunction(list.get(0));
  }

  public static void acceptsJsFunction(Function<String, String> f) {}

  static class TestCaptureOuterParametricClass<T> {
    public void test() {
      Function<Object, Object> f = object -> new ArrayList<T>();
    }
  }

  @JsFunction
  interface JsFunctionVarargs {
    int m(int i, int... numbers);
  }

  JsFunctionVarargs testJsFunctionVarargs() {
    JsFunctionVarargs f =
        (int i, int... numbers) -> {
          int sum = i;
          for (int number : numbers) {
            sum += number;
          }
          return sum;
        };
    return f;
  }

  JsFunctionVarargs testJsFunctionVarargsInnerClass() {
    return new JsFunctionVarargs() {
      @Override
      public int m(int i, int... numbers) {
        int sum = i;
        for (int number : numbers) {
          sum += number;
        }
        return sum;
      }
    };
  }

  @JsFunction
  interface ForEachCallBack<T> {
    Object onInvoke(T p0, int p1, T[] p2);
  }

  @JsFunction
  interface ElementalJsFunction {
    Object call(Object... args);
  }

  public void testVarArgsMethodReferenceToJsFuncion() {
    ForEachCallBack<ElementalJsFunction> c = ElementalJsFunction::call;
  }

  @JsFunction
  interface JsFunctionVarargsGenerics<T> {
    int m(int i, T... numbers);
  }

  <T> void acceptsVarargsJsFunctionWithTypeVariable(JsFunctionVarargsGenerics<T> x) {}

  <T> void acceptsVarargsJsFunctionWithParemetrizedType(JsFunctionVarargsGenerics<List<T>> x) {}

  @JsMethod
  <T> void acceptsVarargsJsFunctionWithTypeVariableInVarargs(JsFunctionVarargsGenerics<T>... x) {}

  @JsMethod
  <T> void acceptsVarargsJsFunctionWithParemetrizedTypeInVarargs(
      JsFunctionVarargsGenerics<List<T>>... x) {}

  @JsFunction
  interface SimpleJsFunction {
    void m();
  }

  @JsMethod
  void acceptsJsFunctionInVarargs(SimpleJsFunction... x) {}

  void testJsFunctionClassLiterals() {
    SimpleJsFunction[] array = {};
    Object o = SimpleJsFunction.class;
    o = SimpleJsFunction[].class;
  }

  @JsFunction
  interface JsFunctionOptional {
    int m(int i, @JsOptional Double number);
  }

  static final class JsFunctionOptionalImpl implements JsFunctionOptional {
    @Override
    public int m(int i, @JsOptional Double number) {
      return (int) (i + number);
    }
  }

  void testJsFunctionOptional() {
    JsFunctionOptional f = (i, n) -> (int) (i + n);
  }

  @JsFunction
  interface ParametricJsFunction<E> {
    void call(E event);
  }

  ParametricJsFunction<?> jsFunctionFieldWildcard = event -> {};

  ParametricJsFunction<String> jsFunctionFieldParameterized = event -> {};

  interface ApiWithMethodReturningParametricJsFunction {
    <T> ParametricJsFunction<T> anApi();
  }

  static class Implementor implements ApiWithMethodReturningParametricJsFunction {
    @Override
    @JsMethod
    public <T> ParametricJsFunction<T> anApi() {
      return (ParametricJsFunction<T>) null;
    }
  }

  static final class ParamtericImplementation<T> implements ParametricJsFunction<T> {
    @Override
    public void call(T t) {
      Object o = (T) t;
    }
  }

  @JsProperty
  private static void setParametricJsFunction(ParametricJsFunction<Object> fn) {}

  @JsProperty
  private static ParametricJsFunction<Object> getParametricJsFunction() {
    return null;
  }

  void testFunctionExpressionTypeReplacement() {
    ParametricJsFunction<String> f =
        unused -> {
          List<List<?>> l = new ArrayList<>();
          l.add(new ArrayList<String>());
        };
  }

  private static class ClassWithJsFunctionProperty {
    ParametricJsFunction<String> function;

    @JsProperty
    ParametricJsFunction<String> getFunction() {
      return null;
    }
  }

  void testJsFunctionPropertyCall() {
    ClassWithJsFunctionProperty c = new ClassWithJsFunctionProperty();
    c.function.call("");
    c.getFunction().call("");
    (c.function).call("");
    (c != null ? c.function : null).call("");
  }

  @JsFunction
  interface JsBiFunction<T, S extends Number> {
    T apply(T t, S s);
  }

  public static final class DoubleDoubleJsBiFunction implements JsBiFunction<Double, Double> {
    @Override
    public Double apply(Double d, Double i) {
      return d;
    }
  }

  public static final class TIntegerJsBiFunction<T> implements JsBiFunction<T, Integer> {
    @Override
    public T apply(T element, Integer i) {
      return null;
    }
  }

  public static Object callInterfaceRaw(JsBiFunction f, Object o, Number n) {
    return f.apply(o, n);
  }

  public static String callInterfaceParameterized(JsBiFunction<String, Integer> f, String s) {
    return f.apply(s, 1);
  }

  public static <U, V extends Number> U callInterfaceUnparameterized(
      JsBiFunction<U, V> f, U u, V v) {
    return f.apply(u, v);
  }

  public static Object callImplementorRaw(TIntegerJsBiFunction f, Object o, Integer n) {
    return f.apply(o, n);
  }

  public static String callImplementorParameterized(TIntegerJsBiFunction<String> f, String s) {
    return f.apply(s, 1);
  }

  public static void testParameterTypes() {
    JsBiFunction tIntegerJsBiFunction = new TIntegerJsBiFunction<String>();
    JsBiFunction doubleDoubleJsBiFunction = new DoubleDoubleJsBiFunction();
    callInterfaceRaw(tIntegerJsBiFunction, "a", 1);
    callInterfaceRaw(doubleDoubleJsBiFunction, 1.1, 1.1);
    callInterfaceParameterized(tIntegerJsBiFunction, "a");
    callInterfaceUnparameterized(tIntegerJsBiFunction, "a", 1);
    callInterfaceUnparameterized(doubleDoubleJsBiFunction, 1.1, 1.1);
    callImplementorRaw(new TIntegerJsBiFunction<Double>(), 1.1, 1);
    callImplementorParameterized(new TIntegerJsBiFunction<String>(), "");
    tIntegerJsBiFunction.apply("a", 1);
    doubleDoubleJsBiFunction.apply(1.1, 1.1);
    callOnFunction(new DoubleDoubleJsBiFunction());
  }

  @JsMethod
  public static double callOnFunction(JsBiFunction<Double, Double> f) {
    return 0;
  }
  ;

  public static void testCast() {
    Object o = new TIntegerJsBiFunction<String>();
    TIntegerJsBiFunction rawTIntegerJsBiFunction = (TIntegerJsBiFunction) o;
    TIntegerJsBiFunction<String> parameterizedTIntegerJsBiFunction =
        (TIntegerJsBiFunction<String>) o;
    JsBiFunction anotherRawJsBiFunction = (JsBiFunction) o;
    JsBiFunction<String, Integer> anotherParameterizedJsBiFunction =
        (JsBiFunction<String, Integer>) o;
    DoubleDoubleJsBiFunction doubleDoubleJsBiFunction = (DoubleDoubleJsBiFunction) o;
  }

  public static void testNewInstance() {
    TIntegerJsBiFunction rawTIntegerJsBiFunction = new TIntegerJsBiFunction();
    TIntegerJsBiFunction<String> parameterizedTIntegerJsBiFunction =
        (TIntegerJsBiFunction) new TIntegerJsBiFunction<String>();
    JsBiFunction rawJsBiFunction = new DoubleDoubleJsBiFunction();
  }

  @JsType(isNative = true, name = "Array", namespace = JsPackage.GLOBAL)
  static class TestJsFunctionInJsOverlayCapturingOuter {

    @JsOverlay
    final void test() {
      sort(a -> TestJsFunctionInJsOverlayCapturingOuter.this == null ? 0 : 1);
    }

    @JsOverlay
    final void sort(JsFunctionInterface func) {}
  }

  private static final class RecursiveParametricJsFunctionImplementation<
          T extends ParametricJsFunction<T>>
      implements ParametricJsFunction<T> {
    public void call(T t) {}
  }

  private static final class RecursiveJsFunctionImplementation
      implements ParametricJsFunction<RecursiveJsFunctionImplementation> {
    public void call(RecursiveJsFunctionImplementation t) {}
  }
}
