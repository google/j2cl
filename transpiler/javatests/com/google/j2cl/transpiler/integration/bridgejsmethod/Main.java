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

import jsinterop.annotations.JsMethod;

public class Main {
  public static class A<T> {
    @JsMethod
    public T fun(T t) {
      return t;
    }

    // not a JsMethod.
    public boolean bar(T t1, T t2) {
      return t1.equals(t2);
    }
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
    boolean bar(T t, T s);
  }

  public static class B extends A<String> {
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

  public static class C extends A<Integer> {
    // Not directly annotated as a JsMethod, but overrides a JsMethod.
    @Override
    public Integer fun(Integer i) {
      return i + 5;
    }
  }

  public static class D extends A<Integer> implements I<Integer> {
    // Two JS bridges: fun__Object, and fun__Number, should result in one bridge.
    @Override
    public Integer fun(Integer i) {
      return i + 6;
    }
  }

  public static class E extends B implements J<String> {
    // inherited B.bar() accidentally overrides J.bar().
    // Two bridges, one for generic method, the other for exposing non-JsMethod, should result in
    // only one bridge.
  }

  public static Object callFunByA(A a, Object o) {
    return a.fun(o);
  }

  public static boolean callBarByA(A a, Object t, Object s) {
    return a.bar(t, s);
  }

  public static Number callFunByI(I i, Number o) {
    return i.fun(o);
  }

  public static boolean callBarByJ(J j, Object t, Object s) {
    return j.bar(t, s);
  }

  @JsMethod
  public static native Object callFun(Object o, Object arg);

  @JsMethod
  public static native boolean callBar(Object o, Object t, Object s);

  public static void main(String... args) {
    assert callFunByA(new A<String>(), "abc").equals("abc");
    assert callFunByA(new B(), "def").equals("defabc");
    assert callFunByA(new C(), 1).equals(new Integer(6));
    assert callFunByA(new D(), 2).equals(new Integer(8));
    assert callFunByA(new E(), "xyz").equals("xyzabc");

    try {
      callFunByA(new B(), 1);
      assert false;
    } catch (ClassCastException e) {
      // expected.
    }

    assert callBarByA(new A<String>(), "abc", "abc");
    assert callBarByA(new B(), "abc", "abc");
    assert callBarByA(new C(), 1, 1);
    assert callBarByA(new D(), 2, 2);
    assert callBarByA(new E(), "xyz", "xyz");

    try {
      callBarByA(new B(), 1, 2);
      assert false;
    } catch (ClassCastException e) {
      // expected.
    }

    assert callFunByI(new D(), 2).equals(new Integer(8));
    try {
      callFunByI(new D(), new Float(2.2));
      assert false;
    } catch (ClassCastException e) {
      // expected.
    }

    assert callBarByJ(new E(), "xyz", "xyz");
    try {
      callBarByJ(new E(), new Object(), new Object());
      assert false;
    } catch (ClassCastException e) {
      // expected.
    }

    assert new B().fun("def").equals("defabc");
    assert new C().fun(1) == 6;
    assert new D().fun(10) == 16;
    assert new E().fun("xyz").equals("xyzabc");

    assert new B().bar("abcd", "defg");
    assert !new C().bar(1, 2);
    assert new D().bar(2, 2);
    assert new E().bar("xyz", "qwe");

    assert callFun(new A<String>(), "abc").equals("abc");
    assert callFun(new B(), "abc").equals("abcabc");
    assert callFun(new C(), 1).equals(6);
    assert callFun(new D(), 10).equals(16);
    assert callFun(new E(), "xyz").equals("xyzabc");

    assert callBar(new B(), "abcd", "defg");
    assert callBar(new E(), "abcd", "defg");
  }
}
