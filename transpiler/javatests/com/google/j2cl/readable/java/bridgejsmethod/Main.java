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

import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsType;

public class Main {
  public static class A<T> {
    @JsMethod
    public T fun(T t) {
      return t;
    }

    // not a JsMethod.
    public void bar(T t) {}
  }

  /** Interface with JsMethod. */
  public interface I<T extends Number> {
    @JsMethod(name = "mNumber")
    T fun(T t);
  }

  /** Interface with non-JsMethod. */
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

  public interface G<V> {
    V enclose(V value);
  }

  public static class H<V> implements G<V> {
    /**
     * The implemented enclose() has now been marked @JsMethod but mangled H.enclose() is still
     * required to execute the behavior defined here. A bridge method is needed from the mangled to
     * unmangled form. The version of the mangled form that is found in super types references type
     * variable C_G_V and must be specialized to C_H_V before being added in type H.
     */
    @JsMethod
    @Override
    public V enclose(V value) {
      return null;
    }
  }

  public static class K<K1, K2> {
    void fun(K1 k1, K2 k2) {}
  }

  public static class L<L1> extends K<String, L1> {
    /**
     * Like the G/H example but requires type specialization across classes instead of interfaces,
     * with multiple type variables and some terminating in both concrete types and type variables.
     */
    @JsMethod
    @Override
    void fun(String string, L1 l1) {}
  }

  interface M {
    B getB();
  }

  @JsType
  @SuppressWarnings("ClassCanBeStatic")
  abstract class N implements M {
    N() {}

    @Override
    public abstract B getB();
  }

  class O extends N {
    private B b;

    @Override
    public B getB() {
      return b;
    }
  }

  interface P {
    String getKey();
  }

  @JsType
  @SuppressWarnings("ClassCanBeStatic")
  abstract class Q implements P {
    @Override
    public abstract String getKey();
  }

  @JsType
  abstract class R extends Q {
    @Override
    public String getKey() {
      return null;
    }
  }

  class S extends R {}

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
    H<Integer> h = new H<>();
    h.enclose(1);
    L<Integer> l = new L<>();
    l.fun("foo", 1);
  }

  // Repro for b/144844889
  class GrandParent<T> {
    @JsMethod
    // This method would have the signature jsMethod(Object) hence it would be mangled as
    // jsMethod_Object if it was not a jsMethod.
    public void jsMethod(T t) {}

    public void method(T t) {}
  }

  class Parent<T extends Parent<?>> extends GrandParent<T> {
    @Override
    @JsMethod
    // This method would have the signature jsMethod(Parent) hence it would be mangled as
    // jsMethod_Parent if it was not a jsMethod, requiring a bridge from jsMethod_Object. But since
    // they are JsMethods the bridge is not created, and jsMethod takes a Parent as a parameter.
    public void jsMethod(T t) {}

    @Override
    public void method(T t) {}
  }

  // The bridge method creation scheme works independently form the JsMethod bridge creation, and
  // here it determines that it needs a two bridges one jsMethod(Parent) to super.jsMethod(Parent)
  // and one from jsMethod(Object) to super.jsMethod(Object) but since they are JsMethod and the
  // names collide only one is created resulting in an inconsistent override.
  class ChildWithoutOverrides extends Parent<ChildWithoutOverrides> {}

  class ChildWithOverrides<T extends ChildWithOverrides<T>> extends Parent<T> {
    @Override
    @JsMethod
    // This method would have the signature jsMethod(ChildWithOverrides) hence it would be mangled
    // as jsMethod_ChildWithOverrides if it was not a jsMethod, requiring a bridge from
    // jsMethod_Object. But since they are JsMethods the bridge is not created and jsMethod takes
    // a ChildWithOverrides as a parameter.
    public void jsMethod(T t) {}

    @Override
    public void method(T t) {}
  }

  class ChildWithRenamedOverride extends GrandParent<ChildWithRenamedOverride> {
    @Override
    @JsMethod(name = "renamedJsMethod")
    // This method will be a JsMethod that is bridged from another JsMethod with a different name.
    public void jsMethod(ChildWithRenamedOverride t) {}
  }
}
