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
package varargs;

import static com.google.j2cl.integration.testing.Asserts.assertEquals;
import static com.google.j2cl.integration.testing.Asserts.assertTrue;
import static com.google.j2cl.integration.testing.TestUtils.isWasm;

import java.util.ArrayList;
import java.util.List;
import varargs.innerpackage.SubclassWithImplicitConstructor;
import varargs.innerpackage.SuperWithNoPublicConstructors;

/** Tests varargs. */
public class Main {
  public static void main(String... args) {
    testVarargs_method();
    testVarargs_constructor();
    testVarargs_superMethodCall();
    testVarargs_implicitSuperConstructorCall();
    testVarargs_implicitSuperConstructorCall_implicitParameters();
    testVarargs_implicitSuperConstructorCall_enum();
    testVarargs_implicitSuperConstructorCall_genericTypes();
    testVarargs_implicitSuperConstructorCall_visibility();
    testVarargs_genericVarargsParameter();
  }

  private static void testVarargs_constructor() {
    class Parent {
      public int value;

      public Parent(int... args) {
        for (int i = 0; i < args.length; i++) {
          value += args[i];
        }
      }

      public Parent(String f) {
        // call the vararg-constructor by this() constructor call.
        this(1);
      }
    }

    class Child extends Parent {
      public Child() {
        // call the vararg-constructor by super() constructor call.
        super(2);
      }
    }

    Parent p = new Parent(1, 2, 3); // constructor call with varargs.
    assertTrue((p.value == 6));

    Parent pp = new Parent(""); // constructor call with varargs is invoked by this() call.
    assertTrue((pp.value == 1));

    Child cc = new Child(); // constructor call with varargs is invoked by super() constructor call.
    assertTrue((cc.value == 2));
  }

  private static void testVarargs_genericVarargsParameter() {
    assertTrue((generics(1, 2) == 1));
    assertTrue((generics("abc", "def").equals("abc")));
  }

  private static <T> T generics(T... elements) {
    return elements[0];
  }

  private static void testVarargs_superMethodCall() {
    class Parent {
      public int sum(int... args) {
        int sum = 0;
        for (int i = 0; i < args.length; i++) {
          sum += args[i];
        }
        return sum;
      }
    }

    class Child extends Parent {
      public int sum(int a, int b, int c, int d) {
        // call the vararg-constructor by super() method call.
        return super.sum(a, b, c, d);
      }
    }
    // method call with varargs is invoked by super() method call.
    assertTrue((new Child().sum(1, 2, 3, 4) == new Parent().sum(1, 2, 3, 4)));
  }

  private static class SuperWithVarargsConstructors {
    String which;

    SuperWithVarargsConstructors(Object... args) {
      which = args.getClass().getComponentType().getSimpleName();
    }

    SuperWithVarargsConstructors(String... args) {
      which = args.getClass().getComponentType().getSimpleName();
    }
  }

  private static class ChildWithImplicitSuperCall extends SuperWithVarargsConstructors {
    public ChildWithImplicitSuperCall() {
      // Implicit super call should call SuperWithVarargsConstructors(String...).
    }
  }

  private static void testVarargs_implicitSuperConstructorCall() {
    if (isWasm()) {
      // TODO(b/184675805): Enable when arrays have metadata.
      return;
    }

    assertEquals("String", new ChildWithImplicitSuperCall().which);
  }

  private static void testVarargs_implicitSuperConstructorCall_implicitParameters() {
    if (isWasm()) {
      // TODO(b/184675805): Enable when arrays have metadata.
      return;
    }

    int captured = 1;
    class Outer {

      class Parent {

        String which;
        int value;

        Parent(Object... args) {
          which = args.getClass().getComponentType().getSimpleName();
          value = captured;
        }

        Parent(String... args) {
          which = args.getClass().getComponentType().getSimpleName();
          value = captured;
        }
      }

      class Child extends Parent {

        public Child() {
          // Implicit super call should call Parent(String...).
        }
      }
    }

    assertEquals("String", new Outer().new Child().which);
  }

  private static void testVarargs_implicitSuperConstructorCall_visibility() {
    class Parent {
      String which;

      private Parent() {
        which = "Private";
      }

      public Parent(Object... args) {
        which = "Public";
      }
    }

    class Child extends Parent {
      public Child() {
        // Implicit super call should call Parent().
      }
    }

    // private is accessible in the same inner class context.
    assertEquals("Private", new Child().which);
    assertEquals("PackagePrivate", new SubclassWithImplicitConstructor().which);

    class SubclassInDifferentPackage extends SuperWithNoPublicConstructors {
      public SubclassInDifferentPackage() {}
    }
    assertEquals("Protected", new SubclassInDifferentPackage().which);
  }

  private static void testVarargs_implicitSuperConstructorCall_genericTypes() {
    if (isWasm()) {
      // TODO(b/184675805): Enable when arrays have metadata.
      return;
    }

    int captured = 1;
    class Parent<T extends List<?>, U extends ArrayList<?>> {
      String which;
      int value;

      Parent(T... args) {
        which = args.getClass().getComponentType().getSimpleName();
        value = captured;
      }

      Parent(U... args) {
        which = args.getClass().getComponentType().getSimpleName();
        value = captured;
      }
    }

    class Child extends Parent<List<?>, ArrayList<?>> {
      public Child() {
        // Implicit super call should call Parent(U...).
      }
    }

    assertEquals("ArrayList", new Child().which);
  }

  @SuppressWarnings("ImmutableEnumChecker")
  private enum MyEnum {
    A,
    B(new Object[0]),
    C {},
    D(new Object[0]) {},
    E(new String[0]) {};

    MyEnum(Object... args) {
      which = args.getClass().getComponentType().getSimpleName();
    }

    MyEnum(String... args) {
      which = args.getClass().getComponentType().getSimpleName();
    }

    String which;
  }

  private static void testVarargs_implicitSuperConstructorCall_enum() {
    if (isWasm()) {
      // TODO(b/184675805): Enable when arrays have metadata.
      return;
    }

    assertEquals("String", MyEnum.A.which);
    assertEquals("Object", MyEnum.B.which);
    assertEquals("String", MyEnum.C.which);
    assertEquals("Object", MyEnum.D.which);
    assertEquals("String", MyEnum.E.which);
  }

  private static void testVarargs_method() {
    Integer i1 = new Integer(1);
    Integer i2 = new Integer(2);
    Integer i3 = new Integer(3);
    Integer i = new Integer(4);
    int a = bar(i1, i2, i3, i); // varargs with mulitple arguments.
    assertTrue((a == 10));

    int b = bar(i1); // no argument for varargs.
    assertTrue((b == 1));

    int c = bar(i1, i2); // varargs with one element.
    assertTrue((c == 3));

    int d = bar(i1, new Integer[] {i2, i3, i}); // array argument for the varargs.
    assertTrue((d == 10));

    int e = bar(i1, new Integer[] {}); // empty array for the varargs.
    assertTrue((e == 1));

    int f = bar(i1, null); // null for the varargs.
    assertTrue((f == 1));
  }

  public static int bar(Integer a, Integer... args) {
    int result = a.intValue();
    if (args != null) {
      for (int i = 0; i < args.length; i++) {
        result += args[i].intValue();
      }
    }
    return result;
  }
}
