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
package bridgemethods;

import static com.google.j2cl.integration.testing.Asserts.assertEquals;
import static com.google.j2cl.integration.testing.Asserts.assertThrowsClassCastException;
import static com.google.j2cl.integration.testing.Asserts.assertTrue;
import static com.google.j2cl.integration.testing.TestUtils.isJvm;

/** Tests for bridge methods with accidental overriding. */
public class Main {

  public static void main(String[] args) {
    testSimpleBridges();
    testBridgeForwardsToSpecializedMethod();
    testBridgeSpecializesSuperclassMethod();
    testAbstractHidesSuperGenericMethod();
    testBridgesMultipleOverloads();
    testParameterizedMethodBridge();
  }

  private static void testSimpleBridges() {
    class Parent<T> {

      public String foo(T t) {
        return "Parent";
      }
    }

    class Child extends Parent<String> {

      // a bridge method should be generated for Parent.foo(T).
      @Override
      public String foo(String t) {
        return "Child";
      }
    }

    class AnotherChild extends Parent<String> {
      // no bridge method should be generated, so no ClassCastException will be caught even
      // an argument of wrong type is passed.
      // NOTE: Why might make this more restrictive if we start synthesizing bridges as high as
      // possible in the class hierarchy, and that is OK.
    }

    Parent c = new Child();
    Parent ac = new AnotherChild();
    assertEquals("Child", c.foo(null));
    assertEquals("Child", c.foo("String"));
    assertEquals("Parent", ac.foo(new Object())); // no ClassCastException. But we can change this.
    assertEquals("Parent", ac.foo("String"));
    assertThrowsClassCastException(() -> c.foo(new Object()), String.class);
  }

  private static void testBridgeForwardsToSpecializedMethod() {
    class Parent {
      public Error foo(Error e) {
        return e;
      }
    }

    class Child extends Parent implements SuperInterface<Error> {
      // Parent.foo(Error) accidentally overrides SuperInterface.foo(T)
      // there should be a bridge method foo__Object for SuperInterface.foo(T), and the bridge
      // method delegates to foo__Error() in Parent.
    }

    Child c = new Child();
    Error e = new Error();
    assertTrue((callInterfaceFoo(c, e) == e));
    assertTrue((c.foo(e) == callInterfaceFoo(c, e)));
    assertThrowsClassCastException(() -> callInterfaceFoo(c, new Object()));
  }

  interface SuperInterface<T> {
    T foo(T t);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static Object callInterfaceFoo(SuperInterface intf, Object t) {
    return intf.foo(t);
  }

  private static void testBridgeSpecializesSuperclassMethod() {
    abstract class AbstractSupplier<T> {
      T t;

      AbstractSupplier(T t) {
        this.t = t;
      }

      public T get() {
        return t;
      }

      public T f(T t) {
        return this.t;
      }
    }

    class SupplierStringImpl extends AbstractSupplier<String> implements SupplierString {
      // T AbstractSupplier.get() accidentally overrides String SupplierString.get(). Hence there
      // should be a bridge method String SupplierStringImpl.get() that has a cast check on the
      // return.
      SupplierStringImpl(String s) {
        super(s);
      }
    }

    SupplierStringImpl sImpl = new SupplierStringImpl("Hello");
    SupplierString s = sImpl;
    assertTrue(s.get().equals("Hello"));

    // Assign to raw type to subvert the value.
    AbstractSupplier as = sImpl;
    as.t = new Object();
    assertThrowsClassCastException(() -> s.f(null));
    assertThrowsClassCastException(() -> s.get());
  }

  interface SupplierString {
    String get();

    String f(String s);
  }

  private static void testAbstractHidesSuperGenericMethod() {
    class Setter<T> {
      public String set(T t) {
        return "Setter";
      }
    }

    abstract class StringSetterAbstract extends Setter<String> implements StringSetter<String> {
      public abstract String set(String s);
    }

    // This class is to make sure that when b/64282599 is fixed the proper bridges are still in
    // place.
    class StringSetterImpl extends StringSetterAbstract {
      public String set(String s) {
        return "StringSetterImpl";
      }
    }
    assertEquals("StringSetterImpl", new StringSetterImpl().set(""));
    assertEquals("StringSetterImpl", ((Setter) new StringSetterImpl()).set(""));
  }

  interface StringSetter<String> {
    String set(String s);
  }

  private static void testBridgesMultipleOverloads() {
    class Parent<T extends Number> {
      public String foo(T t) {
        return "Parent";
      }
    }

    class Child extends Parent<Integer> implements SomeInterface<Integer> {
      // accidental overrides.
    }

    class AnotherChild extends Parent<Number> implements SomeInterface<Integer> {
      // SomeInterface<Integer>.foo does NOT override Parent<Number>.foo
      @Override
      public String foo(Integer t) {
        return "AnotherChild";
      }
    }

    Integer int1 = 1;
    Short short1 = (short) 1;

    Child c = new Child();

    assertEquals("Parent", callByInterface(c, int1));

    // TODO(b/254148464): J2CL is more strict about erasure casts than JVM. Here JVM does not throw
    // assertThrows while J2CL does.
    if (!isJvm()) {
      assertThrowsClassCastException(() -> callByInterface(c, short1), Integer.class);
    }

    Parent pc = c;
    assertEquals("Parent", pc.foo(int1));
    assertEquals("Parent", c.foo(int1));

    AnotherChild ac = new AnotherChild();
    Parent rawParentac = ac;

    assertEquals("Parent", rawParentac.foo(short1));
    assertEquals("Parent", rawParentac.foo(int1));
    assertEquals("AnotherChild", callByInterface(ac, int1));
    assertEquals("AnotherChild", ac.foo(int1));
    assertEquals("Parent", ac.foo(short1)); // does not match foo(integer) so this is foo(Number)
  }

  private static void testParameterizedMethodBridge() {
    class Parent<T> {
      public <E extends T> String m(E e) {
        return "super";
      }
    }

    class Child extends Parent<String> {
      public <F extends String> String m(F e) {
        return "child";
      }
    }

    assertEquals("child", new Child().m(""));
    Parent<String> p = new Child();
    assertEquals("child", p.m(""));
  }

  interface SomeInterface<T> {
    String foo(T t);
  }

  private static String callByInterface(SomeInterface<Integer> intf, Integer ae) {
    return intf.foo(ae);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static String callByInterface(SomeInterface intf, Number e) {
    return intf.foo(e);
  }
}
