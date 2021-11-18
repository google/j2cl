/*
 * Copyright 2021 Google Inc.
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
package com.google.j2cl.integration.optimizedenums;

import static com.google.j2cl.integration.testing.Asserts.assertFalse;
import static com.google.j2cl.integration.testing.Asserts.assertTrue;

public class Main {
  public static void main(String[] args) {
    testOrdinal();
    testName();
    testInstanceMethod();
    testStaticFields();
    testEnumWithConstructors();
    testInstanceOf();
  }

  private static void testOrdinal() {
    assertTrue(SimpleEnum.FOO.ordinal() == 0);
    assertTrue(SimpleEnum.BAR.ordinal() == 1);
    assertTrue(EnumWithStaticField.FOO.ordinal() == 0);
    assertTrue(EnumWithStaticField.BAR.ordinal() == 1);
    assertTrue(OtherOptimizedEnum.FOO.ordinal() == 0);
    assertTrue(OtherOptimizedEnum.BAR.ordinal() == 1);
    assertTrue(EnumWithCtor.FOO.ordinal() == 0);
    assertTrue(EnumWithCtor.BAR.ordinal() == 1);
    assertTrue(EnumWithInstanceMethod.FOO.ordinal() == 0);
    assertTrue(EnumWithInstanceMethod.BAR.ordinal() == 1);
  }

  private static void testName() {
    assertTrue(SimpleEnum.FOO.name().equals("FOO"));
    assertTrue(SimpleEnum.BAR.name().equals("BAR"));
    assertTrue(EnumWithStaticField.FOO.name().equals("FOO"));
    assertTrue(EnumWithStaticField.BAR.name().equals("BAR"));
    assertTrue(OtherOptimizedEnum.FOO.name().equals("FOO"));
    assertTrue(OtherOptimizedEnum.BAR.name().equals("BAR"));
    assertTrue(EnumWithCtor.FOO.name().equals("FOO"));
    assertTrue(EnumWithCtor.BAR.name().equals("BAR"));
    assertTrue(EnumWithInstanceMethod.FOO.name().equals("FOO"));
    assertTrue(EnumWithInstanceMethod.BAR.name().equals("BAR"));
  }

  private static void testInstanceMethod() {
    assertTrue("S0".equals(EnumWithInstanceMethod.FOO.getS()));
    assertTrue("S1".equals(EnumWithInstanceMethod.BAR.getS()));
  }

  private static void testStaticFields() {
    assertTrue(EnumWithStaticField.ENUM_SET.length == 2);
    for (EnumWithStaticField e : EnumWithStaticField.ENUM_SET) {
      assertTrue(e != null);
    }
    assertTrue(EnumWithStaticField.OTHER_ENUM_SET.length == 2);
    for (OtherOptimizedEnum e : EnumWithStaticField.OTHER_ENUM_SET) {
      assertTrue(e != null);
    }

    assertTrue(EnumWithStaticField.field == null);
  }

  private static void testEnumWithConstructors() {
    assertTrue(EnumWithCtor.FOO.i == 0);
    assertTrue(EnumWithCtor.FOO.b);
    assertTrue(EnumWithCtor.FOO.s.equals("default"));
    assertTrue(EnumWithCtor.BAR.i == 1);
    assertTrue(!EnumWithCtor.BAR.b);
    assertTrue(EnumWithCtor.BAR.s.equals("bar"));
  }

  private static void testInstanceOf() {
    Object foo = SimpleEnum.FOO;
    assertTrue(foo instanceof SimpleEnum);
    assertTrue(foo instanceof Enum);
    assertFalse(foo instanceof EmptyEnum);

    Object bar = EnumWithInstanceMethod.BAR;
    assertTrue(bar instanceof EnumWithInstanceMethod);
    assertTrue(bar instanceof MyEnumInterface);
  }

  private static void testDefaultMethod() {
    assertTrue(EnumWithInstanceMethod.FOO == EnumWithInstanceMethod.FOO.getDefaultInstance());
  }

  enum SimpleEnum {
    FOO,
    BAR,
    UNREFERENCED_VALUE,
  }

  enum EnumWithStaticField {
    FOO,
    BAR;

    static EnumWithStaticField f(Object o) {
      return null;
    }

    static EnumWithStaticField[] ENUM_SET = {EnumWithStaticField.FOO, EnumWithStaticField.BAR};
    static OtherOptimizedEnum[] OTHER_ENUM_SET = {OtherOptimizedEnum.FOO, OtherOptimizedEnum.BAR};
    static EnumWithStaticField field = f(new Object());
  }

  enum OtherOptimizedEnum {
    FOO,
    BAR
  }

  enum EmptyEnum {}

  enum EnumWithCtor {
    FOO,
    BAR(1, false, "bar");

    private final int i;
    private final boolean b;
    private final String s;

    EnumWithCtor() {
      this(0, true, "default");
    }

    EnumWithCtor(int i, boolean b, String s) {
      this.i = i;
      this.b = b;
      this.s = s;
    }
  }

  enum EnumWithInstanceMethod implements MyEnumInterface {
    FOO,
    BAR;

    public String getS() {
      return "S" + ordinal();
    }
  }

  interface MyEnumInterface {
    MyEnumInterface DEFAULT_INSTANCE = EnumWithInstanceMethod.FOO;

    String getS();

    default MyEnumInterface getDefaultInstance() {
      return DEFAULT_INSTANCE;
    }
  }
}
