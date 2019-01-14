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
package com.google.j2cl.transpiler.integration.enums;

import static com.google.j2cl.transpiler.utils.Asserts.assertTrue;

import java.util.function.Function;

public class Main {
  public static void main(String[] args) {
    testOrdinal();
    testName();
    testInstanceMethod();
    testStaticFields();
    testEnumInitializedWithLambdas();
  }

  private static void testOrdinal() {
    assertTrue(Foo.FOO.ordinal() == 0);
    assertTrue(Foo.FOZ.ordinal() == 1);

    assertTrue(Bar.BAR.ordinal() == 0);
    assertTrue(Bar.BAZ.ordinal() == 1);
    assertTrue(Bar.BANG.ordinal() == 2);

    assertTrue(Blah.BLAH.ordinal() == 0);
  }

  private static void testName() {
    assertTrue(Foo.FOO.name().equals("FOO"));
    assertTrue(Foo.FOZ.name().equals("FOZ"));

    assertTrue(Bar.BAR.name().equals("BAR"));
    assertTrue(Bar.BAZ.name().equals("BAZ"));
    assertTrue(Bar.BANG.name().equals("BANG"));

    assertTrue(Blah.BLAH.name().equals("BLAH"));
  }

  private static void testInstanceMethod() {
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

    assertTrue(Baz.field == null);
  }

  enum Foo {
    FOO,
    FOZ,
    UNREFERENCED_VALUE,
    UNREFERENCED_SUBCLASS_VALUE {},
  }

  enum Bar {
    BAR(1),
    BAZ(Foo.FOO),
    BANG(5) {
      @Override
      int getF() {
        return f + 2;
      }
    };

    static Bar[] ENUM_SET = {Bar.BAR, Bar.BAZ, Bar.BANG};

    int f;

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

  enum Baz {
    A;

    static Baz f(Object o) {
      return null;
    }

    static Baz field = f(new Object());
  }

  enum Blah {
    BLAH,
    UNREFERENCED_VALUE,
  }

  private static void testEnumInitializedWithLambdas() {
    assertTrue(1 == Functions.PLUS1.function.apply(0));
    assertTrue(1 == Functions.MINUS1.function.apply(2));
  }

  enum Functions {
    PLUS1(n -> n + 1),
    MINUS1(n -> n - 1);

    private final Function<Integer, Integer> function;

    Functions(Function<Integer, Integer> function) {
      this.function = function;
    }
  }
}
