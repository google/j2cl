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
package enums;

import static com.google.j2cl.integration.testing.Asserts.assertEquals;
import static com.google.j2cl.integration.testing.Asserts.assertTrue;

import java.util.function.Function;

/** Contains test for enums that cannot be optimized according to go/j2cl-enums-optimization. */
public class Main {
  public static void main(String... args) {
    testOrdinal();
    testName();
    testInstanceMethod();
    testStaticFields();
    testEnumInitializedWithLambdas();
    testEnumWithConstructors();
    testEnumWithOverriddenMethodsInSeparateLibrary();
  }

  private static void testOrdinal() {
    assertTrue(Foo.FOO.ordinal() == 0);
    assertTrue(Foo.FOZ.ordinal() == 1);

    
    assertTrue(Bar.FOO.ordinal() == 0);
    assertTrue(Bar.BAR.ordinal() == 1);
    assertTrue(Bar.BAZ.ordinal() == 2);
    assertTrue(Bar.BANG.ordinal() == 3);
  }

  private static void testName() {
    assertTrue(Foo.FOO.name().equals("FOO"));
    assertTrue(Foo.FOZ.name().equals("FOZ"));

    assertTrue(Bar.BAR.name().equals("BAR"));
    assertTrue(Bar.BAZ.name().equals("BAZ"));
    assertTrue(Bar.BANG.name().equals("BANG"));
  }

  private static void testInstanceMethod() {
    assertTrue(Bar.FOO.getF() == -1);
    assertTrue(Bar.BAR.getF() == 1);
    assertTrue(Bar.BAZ.getF() == 0);
    assertTrue(Bar.BANG.getF() == 7);
  }

  private static void testStaticFields() {
    // Check use-before-def assigning undefined
    // Although it isn't likely the test will make it this far
    for (Bar b : Bar.ENUM_SET) {
      assertTrue(b != null);
    }

    assertTrue(Bar.staticField == null);
  }

  private static void testEnumWithConstructors() {
    assertTrue(Bar.FOO.f == -1);
    assertTrue(Bar.BAR.f == 1);
    assertTrue(Bar.BAZ.f == 0);
    assertTrue(Bar.BANG.f == 5);
  }

  enum Foo {
    FOO,
    FOZ,
    UNREFERENCED_VALUE,
    UNREFERENCED_SUBCLASS_VALUE {},
  }

  @SuppressWarnings("ImmutableEnumChecker")
  enum Bar {
    FOO,
    BAR(1),
    BAZ(Foo.FOO),
    BANG(5) {
      @Override
      int getF() {
        return f + 2;
      }
    };

    static Bar sf(Object o) {
      return null;
    }

    static Bar[] ENUM_SET = {Bar.BAR, Bar.BAZ, Bar.BANG};
    static Bar staticField = sf(null);

    int f;

    Bar() {
      this(-1);
    }

    Bar(int i) {
      f = i;
    }

    Bar(Foo f) {
      this(f.ordinal());
    }

    int getF() {
      return f;
    }
  }

  private static void testEnumInitializedWithLambdas() {
    assertTrue(1 == Functions.PLUS1.function.apply(0));
    assertTrue(1 == Functions.MINUS1.function.apply(2));
  }

  @SuppressWarnings("ImmutableEnumChecker")
  enum Functions {
    PLUS1(n -> n + 1),
    MINUS1(n -> n - 1);

    private final Function<Integer, Integer> function;

    Functions(Function<Integer, Integer> function) {
      this.function = function;
    }
  }

  private static void testEnumWithOverriddenMethodsInSeparateLibrary() {
    // Repro for b/341721484.
    assertEquals("A", EnumWithOverriddenMethods.A.getConstName());
    assertEquals("B", EnumWithOverriddenMethods.B.getConstName());
  }
}
