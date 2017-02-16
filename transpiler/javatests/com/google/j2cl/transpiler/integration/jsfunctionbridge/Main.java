package com.google.j2cl.transpiler.integration.jsfunctionbridge;

import jsinterop.annotations.JsFunction;

public class Main {
  public static void main(String... args) {
    test();
  }

  public static void test() {
    ApplyFunction<String> foo =
        new ApplyFunction<String>() {
          public String field = "a";

          @Override
          public String apply(String s) {
            return s + field + fun();
          }

          public String fun() {
            return field + "d";
          }
        };
    assert ("eaad".equals(callGeneric(foo, "e")));
    try {
      callGeneric(foo, new Object());
      assert false;
    } catch (ClassCastException e) {
      // expected.
    }
    assert ("eaad".equals(callParametric(foo, "e")));
    assert ("eaad".equals(foo.apply("e")));
    assert "hello".equals(new Identity().apply("hello"));
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public static Object callGeneric(ApplyFunction af, Object o) {
    return af.apply(o);
  }

  public static String callParametric(ApplyFunction<String> af, String s) {
    return af.apply(s);
  }

  @JsFunction
  interface ApplyFunction<T> {
    T apply(T element);
  }

  private static final class Identity implements ApplyFunction<Object> {
    @Override
    public Object apply(Object element) {
      return element;
    }
  }
}
