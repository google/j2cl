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
package bridgejsmethod;

import static com.google.j2cl.integration.testing.Asserts.assertEquals;
import static com.google.j2cl.integration.testing.Asserts.assertThrowsClassCastException;
import static com.google.j2cl.integration.testing.Asserts.assertTrue;

import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsType;

public class Main {
  public static void main(String... args) {
    testJsMethods();
    testJsMethodForwarding();
    testJsMethodOverriddenInJs();
  }

  private static class A<T> {
    @JsMethod
    public T fun(T t) {
      return t;
    }

    // not a JsMethod.
    public String bar(T t) {
      return "A-bar";
    }
  }

  /** Interface with JsMethod. */
  interface I<T extends Number> {
    @JsMethod(name = "fNumber")
    T fun(T t);
  }

  /** Interface with non-JsMethod. */
  interface J<T> {
    String bar(T t);
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
    public String bar(String t) {
      return "B-bar";
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

  private static class F extends E {
    public String bar(String s) {
      return "F-bar";
    }
  }

  private static Object callFunByA(A a, Object o) {
    return a.fun(o);
  }

  private static String callBarByA(A a, Object t) {
    return a.bar(t);
  }

  private static Number callFunByI(I i, Number o) {
    return i.fun(o);
  }

  private static String callBarByJ(J j, Object t) {
    return j.bar(t);
  }

  @JsMethod
  private static native Object callFun(Object o, Object arg);

  @JsMethod
  private static native boolean callBar(Object o, Object t, Object s);

  private static void testJsMethods() {
    assertTrue(callFunByA(new A<String>(), "abc").equals("abc"));
    assertTrue(callFunByA(new B(), "def").equals("defabc"));
    assertTrue(callFunByA(new C(), 1).equals(new Integer(6)));
    assertTrue(callFunByA(new D(), 2).equals(new Integer(8)));
    assertTrue(callFunByA(new E(), "xyz").equals("xyzabc"));
    assertThrowsClassCastException(() -> callFunByA(new B(), 1), String.class);

    assertEquals("A-bar", callBarByA(new A<String>(), "abc"));
    assertEquals("B-bar", callBarByA(new B(), "abc"));
    assertEquals("A-bar", callBarByA(new C(), 1));
    assertEquals("A-bar", callBarByA(new D(), 2));
    assertEquals("B-bar", callBarByA(new E(), "xyz"));
    assertEquals("F-bar", callBarByA(new F(), "abcd"));
    assertThrowsClassCastException(() -> callBarByA(new B(), 1), String.class);

    assertTrue(callFunByI(new D(), 2).equals(new Integer(8)));
    assertThrowsClassCastException(() -> callFunByI(new D(), new Float(2.2)), Integer.class);

    assertEquals("B-bar", callBarByJ(new E(), "xyz"));
    assertThrowsClassCastException(() -> callBarByJ(new E(), new Object()), String.class);

    assertTrue(new B().fun("def").equals("defabc"));
    assertTrue(new C().fun(1) == 6);
    assertTrue(new D().fun(10) == 16);
    assertTrue(new E().fun("xyz").equals("xyzabc"));

    assertEquals("A-bar", new A<>().bar(new Object()));
    assertEquals("B-bar", new B().bar("abcd"));
    assertEquals("A-bar", new C().bar(1));
    assertEquals("A-bar", new D().bar(1));
    assertEquals("B-bar", new E().bar("abcd"));
    assertEquals("F-bar", new F().bar("abcd"));

    assertTrue(callFun(new A<String>(), "abc").equals("abc"));
    assertTrue(callFun(new B(), "abc").equals("abcabc"));
    assertTrue(callFun(new C(), 1).equals(6));
    assertTrue(callFun(new D(), 10).equals(16));
    assertTrue(callFun(new E(), "xyz").equals("xyzabc"));

    assertTrue(callBar(new B(), "abcd", "defg"));
    assertTrue(callBar(new E(), "abcd", "defg"));
  }

  private static class Top<T, S> {
    @JsMethod
    public void m(T t, S s) {}
  }

  interface Accidental<S> {
    void m(String t, S s);
  }

  private static class Child<S> extends Top<String, S> {
    public void m(String t, S s) {}
  }

  private static class GrandChild extends Child<String> implements Accidental<String> {
    public void m(String t, String s) {}
  }

  private static void testJsMethodForwarding() {
    Top o = new GrandChild();
    assertThrowsClassCastException(
        () -> {
          o.m(null, new Object());
        },
        String.class);
    assertThrowsClassCastException(
        () -> {
          o.m(new Object(), null);
        },
        String.class);

    Accidental a = new GrandChild();
    assertThrowsClassCastException(
        () -> {
          a.m(null, new Object());
        },
        String.class);
  }

  interface InterfaceWithMethod {
    String m();
  }

  interface InterfaceWithDefaultJsMethod extends InterfaceWithMethod {
    @JsMethod
    default String m() {
      return "m from Java default";
    }

    @JsMethod
    String n();
  }

  static class SuperClassWithFinalMethod {
    public final String n() {
      return "n from Java";
    }
  }

  static class ExposesOverrideableJsMethod extends SuperClassWithFinalMethod
      implements InterfaceWithDefaultJsMethod {}

  private static void testJsMethodOverriddenInJs() {
    assertEquals("m from Java default", new ExposesOverrideableJsMethod().m());
    assertEquals(
        "m from Java default", ((InterfaceWithMethod) new ExposesOverrideableJsMethod()).m());
    assertEquals("m from Js", new NativeSubclass().m());
    assertEquals("m from Js", ((InterfaceWithMethod) new NativeSubclass()).m());

    assertEquals("n from Java", new ExposesOverrideableJsMethod().n());
    assertEquals(
        "n from Java", ((SuperClassWithFinalMethod) new ExposesOverrideableJsMethod()).n());
    assertEquals("n from Js", new NativeSubclass().n());
    // TODO(b/280451858): uncomment when bug is fixed.
    // assertEquals("n from Js", ((SuperClassWithFinalMethod) (Object) new NativeSubclass()).n());
  }
}

@JsType(isNative = true)
class NativeSubclass {
  public native String m();

  public native String n();
}
