package com.google.j2cl.transpiler.readable.bridgejsmethod;

import jsinterop.annotations.JsMethod;

public class Main {
  public static class A<T> {
    @JsMethod
    public T fun(T t) {
      return t;
    }

    // not a JsMethod.
    public void bar(T t) {}
  }

  /**
   * Interface with JsMethod.
   */
  public interface I<T extends Number> {
    @JsMethod
    T fun(T t);
  }

  /**
   * Interface with non-JsMethod.
   */
  public interface J<T> {
    void bar(T t);
  }

  public static class B extends A<String> {
    // It is a JsMethod itself and also overrides a JsMethod.
    // One JS bridge method for fun__Object(), but since the bridge method is a JsMethod, this
    // real implementation method is emit as non-JsMethod.
    @Override
    @JsMethod
    public String fun(String s) {
      return s;
    }

    // It is a JsMethod itself and overrides a non-JsMethod.
    // Two bridge methods, one for generic method, and one for exposing non-JsMethod, should result
    // in only one bridge method.
    @JsMethod
    @Override
    public void bar(String s) {}
  }

  public static class C extends A<Integer> {
    // It's not a directly annotated JsMethod, but overrides a JsMethod.
    // The bridge method is a JsMethod, and this real implementation method is non-JsMethod.
    @Override
    public Integer fun(Integer i) {
      return i;
    }
  }

  public static class D extends A<Integer> implements I<Integer> {
    // Two JS bridges: fun__Object, and fun__Number, and should result in only one, and this real
    // implementation method is non-JsMethod.
    @Override
    public Integer fun(Integer i) {
      return i;
    }
  }

  public static class E extends B implements J<String> {
    // inherited B.bar() accidentally overrides J.bar().
    // Two bridges, one for generic method, the other for exposing non-JsMethod, should result in
    // only one bridge.
  }

  public static class F extends A<Integer> implements I<Integer> {}

  public static void test() {
    A a = new A<Integer>();
    a.fun(1);
    a.bar(1);
    B b = new B();
    b.fun("abc");
    b.bar("abc");
    C c = new C();
    c.fun(1);
    c.bar(1);
    D d = new D();
    d.fun(1);
    d.bar(1);
    E e = new E();
    e.fun("abc");
    e.bar("abc");
  }
}
