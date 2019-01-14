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
package com.google.j2cl.transpiler.integration.bridgejsmethod;

import static com.google.j2cl.transpiler.utils.Asserts.assertTrue;

import jsinterop.annotations.JsMethod;

public class Main {
  public static void main(String... args) {
    assertTrue(callFunByA(new A<String>(), "abc").equals("abc"));
    assertTrue(callFunByA(new B(), "def").equals("defabc"));
    assertTrue(callFunByA(new C(), 1).equals(new Integer(6)));
    assertTrue(callFunByA(new D(), 2).equals(new Integer(8)));
    assertTrue(callFunByA(new E(), "xyz").equals("xyzabc"));

    try {
      callFunByA(new B(), 1);
      assertTrue(false);
    } catch (ClassCastException e) {
      // expected.
    }

    assertTrue(callBarByA(new A<String>(), "abc", "abc"));
    assertTrue(callBarByA(new B(), "abc", "abc"));
    assertTrue(callBarByA(new C(), 1, 1));
    assertTrue(callBarByA(new D(), 2, 2));
    assertTrue(callBarByA(new E(), "xyz", "xyz"));

    try {
      callBarByA(new B(), 1, 2);
      assertTrue(false);
    } catch (ClassCastException e) {
      // expected.
    }

    assertTrue(callFunByI(new D(), 2).equals(new Integer(8)));
    try {
      callFunByI(new D(), new Float(2.2));
      assertTrue(false);
    } catch (ClassCastException e) {
      // expected.
    }

    assertTrue(callBarByJ(new E(), "xyz", "xyz"));
    try {
      callBarByJ(new E(), new Object(), new Object());
      assertTrue(false);
    } catch (ClassCastException e) {
      // expected.
    }

    assertTrue(new B().fun("def").equals("defabc"));
    assertTrue(new C().fun(1) == 6);
    assertTrue(new D().fun(10) == 16);
    assertTrue(new E().fun("xyz").equals("xyzabc"));

    assertTrue(new B().bar("abcd", "defg"));
    assertTrue(!new C().bar(1, 2));
    assertTrue(new D().bar(2, 2));
    assertTrue(new E().bar("xyz", "qwe"));

    assertTrue(callFun(new A<String>(), "abc").equals("abc"));
    assertTrue(callFun(new B(), "abc").equals("abcabc"));
    assertTrue(callFun(new C(), 1).equals(6));
    assertTrue(callFun(new D(), 10).equals(16));
    assertTrue(callFun(new E(), "xyz").equals("xyzabc"));

    assertTrue(callBar(new B(), "abcd", "defg"));
    assertTrue(callBar(new E(), "abcd", "defg"));
  }

  private static class A<T> {
    @JsMethod
    public T fun(T t) {
      return t;
    }

    // not a JsMethod.
    public boolean bar(T t1, T t2) {
      return t1.equals(t2);
    }
  }

  /** Interface with JsMethod. */
  interface I<T extends Number> {
    @JsMethod
    T fun(T t);
  }

  /** Interface with non-JsMethod. */
  interface J<T> {
    boolean bar(T t, T s);
  }

  private static class B extends A<String> {
    // It is a JsMethod itself and also overrides a JsMethod.
    @Override
    @JsMethod
    public String fun(String s) {
      return s + "abc";
    }

    // It is a JsMethod itself and overrides a non-JsMethod.
    // Two bridges, one for the generic method, and one for exposing non-JsMethod, should result
    // in only one bridge.
    @JsMethod
    @Override
    public boolean bar(String s, String t) {
      return s.length() == t.length();
    }
  }

  private static class C extends A<Integer> {
    // Not directly annotated as a JsMethod, but overrides a JsMethod.
    @Override
    public Integer fun(Integer i) {
      return i + 5;
    }
  }

  private static class D extends A<Integer> implements I<Integer> {
    // Two JS bridges: fun__Object, and fun__Number, should result in one bridge.
    @Override
    public Integer fun(Integer i) {
      return i + 6;
    }
  }

  private static class E extends B implements J<String> {
    // inherited B.bar() accidentally overrides J.bar().
    // Two bridges, one for generic method, the other for exposing non-JsMethod, should result in
    // only one bridge.
  }

  private static Object callFunByA(A a, Object o) {
    return a.fun(o);
  }

  private static boolean callBarByA(A a, Object t, Object s) {
    return a.bar(t, s);
  }

  private static Number callFunByI(I i, Number o) {
    return i.fun(o);
  }

  private static boolean callBarByJ(J j, Object t, Object s) {
    return j.bar(t, s);
  }

  @JsMethod
  private static native Object callFun(Object o, Object arg);

  @JsMethod
  private static native boolean callBar(Object o, Object t, Object s);
}
