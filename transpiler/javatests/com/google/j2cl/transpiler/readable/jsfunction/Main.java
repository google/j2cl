package com.google.j2cl.transpiler.readable.jsfunction;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;
import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsOptional;

public class Main {
  public static int fun(MyJsFunctionInterface fn, int a) {
    return fn.foo(a);
  }

  public void test() {
    MyJsFunctionImpl func = new MyJsFunctionImpl();
    // call by java
    func.foo(10);
    // call by js
    callAsFunction(func, 10);
    // pass Javascript function to java.
    fun(createMyJsFunction(), 10);
    // call other instance methods and fields.
    int a = func.field;
    func.bar();

    final int n = 4;
    // Use number as a variable to make sure it is aliased properly.
    fun((number) -> number + n, n);
    fun(
        new MyJsFunctionInterface() {
          @Override
          public int foo(int a) {
            return a + n;
          }
        },
        n);

    new MyJsFunctionInterface() {
      @Override
      public int foo(int a) {
        handleReceiveCommands();
        return 0;
      }
    }.foo(3);

  }

  @JsMethod
  public static native int callAsFunction(Object fn, int arg);

  @JsMethod
  public static native MyJsFunctionInterface createMyJsFunction();

  private void handleReceiveCommands() {}

  @JsFunction
  interface Function<T, U> {
    T apply(U u);
  }

  public static void f(Function<String, String> f) {}

  public void test2() {
    List<Function<String, String>> list = new ArrayList<>();
    f(list.get(0));
  }

  public static <A, T> A[] toArray(IntFunction<A[]> generator) {
    List<T> collected = null;
    return collected.toArray(generator.apply(collected.size()));
  }

  @SuppressWarnings("ClassCanBeStatic")
  class SomeClass<T> {
    public void testJsFunctionWithClassCapture() {
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

  interface Api {
    <T> ParametricJsFunction<T> anApi();
  }

  static class Implementor implements Api {
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
}
