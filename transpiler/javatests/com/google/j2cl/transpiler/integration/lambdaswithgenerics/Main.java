package com.google.j2cl.transpiler.integration.lambdaswithgenerics;

import java.util.Spliterator;
import java.util.function.Function;
import java.util.function.Supplier;

interface MyInterface<T> {
  T foo(T i);
}

public class Main {
  public int field;

  public Main(int f) {
    this.field = f;
  }

  public int test(MyInterface<Main> intf, Main m) {
    return this.field + intf.foo(m).field;
  }

  public void testLambdaNoCapture() {
    Main m = new Main(100);
    int result = test(i -> new Main(i.field * 3), m);
    assert (result == 310);
  }

  public void testLambdaCaptureField() {
    Main m = new Main(100);
    int result =
        test(
            i -> {
              int f = i.field + field;
              return new Main(f);
            },
            m);
    assert (result == 120);
  }

  public void testLambdaCaptureLocal() {
    final Main m = new Main(100);
    int result =
        test(
            i -> {
              int f = i.field + m.field;
              return new Main(f);
            },
            m);
    assert (result == 210);
  }

  public static void main(String[] args) {
    Main m = new Main(10);
    m.testLambdaNoCapture();
    m.testLambdaCaptureField();
    m.testLambdaCaptureLocal();
  }

  // The next definitions are testing that types variables are created correctly in the lambda
  // class implementations and if not the compile through jscompiler will fail.
  public interface Stream<T> {
    <R> Stream<R> flatMap(Function<? super T, ? extends Stream<? extends R>> mapper);

    static <T> Stream<T> generate(Supplier<T> s) {
      return null;
    }
  }

  public static <T> Stream<T> stream(Supplier<? extends Spliterator<T>> supplier) {
    return Stream.generate(supplier).flatMap(spliterator -> stream(spliterator));
  }

  public static <T> Stream<T> stream(Spliterator<T> spliterator) {
    return null;
  }
}
