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
package com.google.j2cl.ported.java6;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests enum functionality. */
@RunWith(JUnit4.class)
@SuppressWarnings({"BoxedPrimitiveConstructor", "MethodCanBeStatic"})
public class EnumsTest {

  enum BarelyReferenced {
    A,
    B,
    C
  }

  enum BarelyReferenced2 {
    A,
    B,
    C
  }

  enum Basic {
    A,
    B,
    C
  }

  enum Complex {
    A("X"),
    B("Y"),
    C("Z");

    final String value;

    Complex(String value) {
      this.value = value;
    }

    public String value() {
      return value;
    }
  }

  enum Subclassing {
    A {
      @Override
      public String value() {
        return "X";
      }
    },
    B {
      @Override
      public String value() {
        return "Y";
      }
    },
    C {
      @Override
      public String value() {
        return "Z";
      }
    };

    public abstract String value();
  }

  enum BasicWithOverloadedValueOf {
    A(1),
    B(2),
    C(3);

    private final int id;

    private BasicWithOverloadedValueOf(int id) {
      this.id = id;
    }

    public static BasicWithOverloadedValueOf valueOf(Integer id) {
      for (BasicWithOverloadedValueOf val : BasicWithOverloadedValueOf.values()) {
        if (val.id == id) {
          return val;
        }
      }
      throw new IllegalArgumentException();
    }
  }

  /*
   * An enum that will have a static impl generated, which will allow it to
   * become ordinalizable. Once ordinalized, need to make sure it's clinit
   * is handled properly.
   */
  enum EnumWithStaticImpl {
    VALUE1;

    // a mock formatter, to simulate the case that inspired this test
    private static class MockFormat {
      private String pattern;

      public MockFormat(String pattern) {
        this.pattern = pattern;
      }

      public String format(Long date) {
        return pattern + date.toString();
      }
    }

    private static final MockFormat FORMATTER = new MockFormat("asdf");

    public String formatDate(long date) {
      switch (this) {
        case VALUE1:
          return FORMATTER.format(new Long(date));
        default:
          return null;
      }
    }
  }

  @Test
  public void testCompareTo() {
    assertThat(Basic.A.compareTo(Basic.valueOf("A")) == 0).isTrue();
    assertThat(Basic.B.compareTo(Basic.A) > 0).isTrue();
    assertThat(Basic.A.compareTo(Basic.B) < 0).isTrue();

    assertThat(Complex.A.compareTo(Complex.valueOf("A")) == 0).isTrue();
    assertThat(Complex.B.compareTo(Complex.A) > 0).isTrue();
    assertThat(Complex.A.compareTo(Complex.B) < 0).isTrue();

    assertThat(Subclassing.A.compareTo(Subclassing.valueOf("A")) == 0).isTrue();
    assertThat(Subclassing.B.compareTo(Subclassing.A) > 0).isTrue();
    assertThat(Subclassing.A.compareTo(Subclassing.B) < 0).isTrue();
  }

  @Test
  public void testField() {
    assertThat((Object) Complex.A.value).isEqualTo("X");
    assertThat((Object) Complex.B.value).isEqualTo("Y");
    assertThat((Object) Complex.C.value).isEqualTo("Z");
  }

  @Test
  public void testGetDeclaringClass() {
    assertThat((Object) Basic.A.getDeclaringClass()).isEqualTo(Basic.class);
    assertThat((Object) Complex.A.getDeclaringClass()).isEqualTo(Complex.class);
    assertThat((Object) Subclassing.A.getDeclaringClass()).isEqualTo(Subclassing.class);
  }

  @Test
  public void testMethod() {
    assertThat((Object) Complex.A.value()).isEqualTo("X");
    assertThat(Complex.B.value()).isEqualTo("Y");
    assertThat(Complex.C.value()).isEqualTo("Z");

    assertThat(Subclassing.A.value()).isEqualTo("X");
    assertThat(Subclassing.B.value()).isEqualTo("Y");
    assertThat(Subclassing.C.value()).isEqualTo("Z");
  }

  @Test
  public void testName() {
    assertThat(Basic.A.name()).isEqualTo("A");
    assertThat(Basic.B.name()).isEqualTo("B");
    assertThat(Basic.C.name()).isEqualTo("C");

    assertThat(Complex.A.name()).isEqualTo("A");
    assertThat(Complex.B.name()).isEqualTo("B");
    assertThat(Complex.C.name()).isEqualTo("C");

    assertThat(Subclassing.A.name()).isEqualTo("A");
    assertThat(Subclassing.B.name()).isEqualTo("B");
    assertThat(Subclassing.C.name()).isEqualTo("C");
  }

  @Test
  public void testOrdinals() {
    assertThat(Basic.A.ordinal()).isEqualTo(0);
    assertThat(Basic.B.ordinal()).isEqualTo(1);
    assertThat(Basic.C.ordinal()).isEqualTo(2);

    assertThat(Complex.A.ordinal()).isEqualTo(0);
    assertThat(Complex.B.ordinal()).isEqualTo(1);
    assertThat(Complex.C.ordinal()).isEqualTo(2);

    assertThat(Subclassing.A.ordinal()).isEqualTo(0);
    assertThat(Subclassing.B.ordinal()).isEqualTo(1);
    assertThat(Subclassing.C.ordinal()).isEqualTo(2);
  }

  @Test
  public void testSwitch() {
    switch (Basic.A) {
      case A:
        break;
      case B:
        fail("Switch failed");
        break;
      case C:
        fail("Switch failed");
        break;
      default:
        fail("Switch failed");
        break;
    }
    switch (Complex.B) {
      case A:
        fail("Switch failed");
        break;
      case B:
        break;
      case C:
        fail("Switch failed");
        break;
      default:
        fail("Switch failed");
        break;
    }
    switch (Subclassing.C) {
      case A:
        fail("Switch failed");
        break;
      case B:
        fail("Switch failed");
        break;
      default:
        break;
    }
  }

  // TODO(b/36863439): Uncomment when Enum.valueOf is implemented.
  // @Test
  public void testBarelyReferencedValueOf() {
    try {
      Enum.valueOf(BarelyReferenced.class, "foo");
      fail(
          "Passed an invalid enum constant name to Enum.valueOf; expected "
              + "IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
      // expected
    }
  }

  // TODO(b/30745420): Enable this test once Class.getEnumConstants() is implemented.
  // @Test
  public void testBarelyReferencedGetEnumConstants() {
    assertThat(BarelyReferenced2.class.getEnumConstants()).isNotNull();
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testValueOf() {
    assertThat(Basic.valueOf("A")).isEqualTo(Basic.A);
    assertThat(Basic.valueOf("B")).isEqualTo(Basic.B);
    assertThat(Basic.valueOf("C")).isEqualTo(Basic.C);
    try {
      Basic.valueOf("D");
      fail("Basic.valueOf(\"D\") -- expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }

    assertThat(Complex.valueOf("A")).isEqualTo(Complex.A);
    assertThat(Complex.valueOf("B")).isEqualTo(Complex.B);
    assertThat(Complex.valueOf("C")).isEqualTo(Complex.C);
    try {
      Complex.valueOf("D");
      fail("Complex.valueOf(\"D\") -- expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }

    assertThat(Subclassing.valueOf("A")).isEqualTo(Subclassing.A);
    assertThat(Subclassing.valueOf("B")).isEqualTo(Subclassing.B);
    assertThat(Subclassing.valueOf("C")).isEqualTo(Subclassing.C);
    try {
      Subclassing.valueOf("D");
      fail("Subclassing.valueOf(\"D\") -- expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }

    // TODO(b/30745420): Uncomment when Class.getEnumConstants() is implemented.
    // enumValuesTest(Basic.class);
    // enumValuesTest(Complex.class);
    // enumValuesTest(Subclassing.class);

    // TODO(b/36863439): Uncomment when Enum.valueOf is implemented.
    // try {
    // Enum.valueOf(Basic.class, "foo");
    //   fail(
    //       "Passed an invalid enum constant name to Enum.valueOf; expected "
    //           + "IllegalArgumentException");
    // } catch (IllegalArgumentException expected) {
    // }

    // try {
    //   @SuppressWarnings("all")
    //   Class fakeEnumClass = String.class;
    //   Enum.valueOf(fakeEnumClass, "foo");
    //   fail("Passed a non enum class to Enum.valueOf; expected IllegalArgumentException");
    // } catch (IllegalArgumentException expected) {
    // }

    // try {
    //   Class<Basic> nullEnumClass = null;
    //   Enum.valueOf(nullEnumClass, "foo");
    //   fail("Passed a null enum class to Enum.valueOf; expected NullPointerException");
    // } catch (NullPointerException expected) {
    // }

    // try {
    //   Enum.valueOf(Basic.class, null);
    //   fail("Passed a null enum constant to Enum.valueOf; expected NullPointerException");
    // } catch (NullPointerException expected) {
    // }
  }

  @Test
  public void testValueOfOverload() {
    // TODO(b/36863439): Uncomment when Enum.valueOf is implemented.
    // BasicWithOverloadedValueOf val1 = Enum.valueOf(BasicWithOverloadedValueOf.class, "A");
    // BasicWithOverloadedValueOf valById1 = BasicWithOverloadedValueOf.valueOf(1);
    // assertThat(valById1).isEqualTo(val1);

    BasicWithOverloadedValueOf val2 = BasicWithOverloadedValueOf.valueOf("B");
    BasicWithOverloadedValueOf valById2 = BasicWithOverloadedValueOf.valueOf(2);
    assertThat(valById2).isEqualTo(val2);
  }

  @Test
  public void testValues() {
    Basic[] simples = Basic.values();
    assertThat(simples.length).isEqualTo(3);
    assertThat(simples[0]).isEqualTo(Basic.A);
    assertThat(simples[1]).isEqualTo(Basic.B);
    assertThat(simples[2]).isEqualTo(Basic.C);

    Complex[] complexes = Complex.values();
    assertThat(complexes.length).isEqualTo(3);
    assertThat(complexes[0]).isEqualTo(Complex.A);
    assertThat(complexes[1]).isEqualTo(Complex.B);
    assertThat(complexes[2]).isEqualTo(Complex.C);

    Subclassing[] subs = Subclassing.values();
    assertThat(subs.length).isEqualTo(3);
    assertThat(subs[0]).isEqualTo(Subclassing.A);
    assertThat(subs[1]).isEqualTo(Subclassing.B);
    assertThat(subs[2]).isEqualTo(Subclassing.C);
  }

  @Test
  public void testValues_Unmodifiable() {
    Basic[] simples = Basic.values();
    simples[0] = simples[1];
    assertThat(Basic.values()[0]).isEqualTo(Basic.A);
  }

  /*
   * Test that a call to an enum instance method, which gets transformed to a
   * static impl, produces valid executable javascript, once the enum gets
   * ordinalized. This test is in response to a case where invalid javascript
   * was being generated, by not generating a clinit for the enum prior to
   * referencing a static impl method.
   */
  @Test
  public void testEnumWithStaticImpl() {
    EnumWithStaticImpl ewsi = EnumWithStaticImpl.VALUE1;
    Long submitTime = new Long(1234567890);
    String fmtDate = ewsi.formatDate(submitTime);
    // we just need to make sure we get to this point without timing out
    assertThat(fmtDate != null).isTrue();
  }

  // TODO(b/30745420): Uncomment when Class.getEnumConstants() is implemented.
  // private <T extends Enum<T>> void enumValuesTest(Class<T> enumClass) {
  //  T[] constants = enumClass.getEnumConstants();
  //  for (T constant : constants) {
  //    assertThat(Enum.valueOf(enumClass, constant.name())).isEqualTo(constant);
  //  }
  //}
}
