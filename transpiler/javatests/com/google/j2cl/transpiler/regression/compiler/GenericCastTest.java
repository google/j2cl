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
package com.google.j2cl.transpiler.regression.compiler;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for appropriate generation of type checks when generic methods/fields are referenced. This
 * is actually testing GenerateJavaAST's appropriate use of maybeCast(). We test such references in
 * so many contexts (field, method, as qualifier, etc) to ensures we cover all the code paths,
 * because JDT has a lot of representation variants with respect to fields (SingleNameReference,
 * QualifiedNameReference, FieldAccess).
 */
@RunWith(JUnit4.class)
public class GenericCastTest {

  private static final String EXPECTED_CLASS_CAST_MESSAGE =
      "java.lang.Object cannot be cast to "
          + "com.google.j2cl.transpiler.regression.compiler.GenericCastTest$Foo";

  @SuppressWarnings("unused")
  /** Always contains an Object internally, the parameterization is a lie. */
  static class Liar<T> {
    @SuppressWarnings("unchecked")
    public final T value = (T) new Object();

    public T get() {
      return value;
    }
  }

  static class LiarFoo extends Liar<Foo> {
    @Test
    public void testOuterField() {
      new Runnable() {
        @Override
        public void run() {
          // Should succeed
          Object unusedObject = value;

          try {
            Foo unusedFoo = value;
            fail("Expected ClassCastException 1");
          } catch (ClassCastException expected) {
            assertThat(expected.getMessage()).isEqualTo(EXPECTED_CLASS_CAST_MESSAGE);
          }
          try {
            String unusedString = value.bar;
            fail("Expected ClassCastException 2");
          } catch (ClassCastException expected) {
            assertThat(expected.getMessage()).isEqualTo(EXPECTED_CLASS_CAST_MESSAGE);
          }
          try {
            String unusedString = value.baz();
            fail("Expected ClassCastException 3");
          } catch (ClassCastException expected) {
            assertThat(expected.getMessage()).isEqualTo(EXPECTED_CLASS_CAST_MESSAGE);
          }
        }
      }.run();
    }

    @Test
    public void testOuterMethod() {
      new Runnable() {
        @Override
        public void run() {
          // Should succeed
          Object unusedObject = get();

          try {
            Foo unusedFoo = get();
            fail("Expected ClassCastException 1");
          } catch (ClassCastException expected) {
            assertThat(expected.getMessage()).isEqualTo(EXPECTED_CLASS_CAST_MESSAGE);
          }
          try {
            String unusedString = get().bar;
            fail("Expected ClassCastException 2");
          } catch (ClassCastException expected) {
            assertThat(expected.getMessage()).isEqualTo(EXPECTED_CLASS_CAST_MESSAGE);
          }
          try {
            String unusedString = get().baz();
            fail("Expected ClassCastException 3");
          } catch (ClassCastException expected) {
            assertThat(expected.getMessage()).isEqualTo(EXPECTED_CLASS_CAST_MESSAGE);
          }
        }
      }.run();
    }

    public void testSuperField() {
      // Should succeed
      Object unusedObject = value;

      try {
        Foo unusedFoo = value;
        fail("Expected ClassCastException 1");
      } catch (ClassCastException expected) {
        assertThat(expected.getMessage()).isEqualTo(EXPECTED_CLASS_CAST_MESSAGE);
      }
      try {
        String unusedString = value.bar;
        fail("Expected ClassCastException 2");
      } catch (ClassCastException expected) {
        assertThat(expected.getMessage()).isEqualTo(EXPECTED_CLASS_CAST_MESSAGE);
      }
      try {
        String unusedString = value.baz();
        fail("Expected ClassCastException 3");
      } catch (ClassCastException expected) {
        assertThat(expected.getMessage()).isEqualTo(EXPECTED_CLASS_CAST_MESSAGE);
      }
    }

    public void testSuperMethod() {
      // Should succeed
      Object unusedObject = get();

      try {
        Foo unusedFoo = get();
        fail("Expected ClassCastException 1");
      } catch (ClassCastException expected) {
        assertThat(expected.getMessage()).isEqualTo(EXPECTED_CLASS_CAST_MESSAGE);
      }
      try {
        String unusedString = get().bar;
        fail("Expected ClassCastException 2");
      } catch (ClassCastException expected) {
        assertThat(expected.getMessage()).isEqualTo(EXPECTED_CLASS_CAST_MESSAGE);
      }
      try {
        String unusedString = get().baz();
        fail("Expected ClassCastException 3");
      } catch (ClassCastException expected) {
        assertThat(expected.getMessage()).isEqualTo(EXPECTED_CLASS_CAST_MESSAGE);
      }
    }

    public void testInternalAccess() {
      new Runnable() {
        @Override
        public void run() {
          Object unusedObject = get();
          try {
            Foo unusedFoo = get();
            fail("Expected ClassCastException 5a");
          } catch (ClassCastException expected) {
            assertThat(expected.getMessage()).isEqualTo(EXPECTED_CLASS_CAST_MESSAGE);
          }
          try {
            String unusedString = get().bar;
            fail("Expected ClassCastException 5b");
          } catch (ClassCastException expected) {
            assertThat(expected.getMessage()).isEqualTo(EXPECTED_CLASS_CAST_MESSAGE);
          }
          try {
            String unusedString = get().baz();
            fail("Expected ClassCastException 5c");
          } catch (ClassCastException expected) {
            assertThat(expected.getMessage()).isEqualTo(EXPECTED_CLASS_CAST_MESSAGE);
          }

          unusedObject = value;
          try {
            Foo unusedFoo = value;
            fail("Expected ClassCastException 6a");
          } catch (ClassCastException expected) {
            assertThat(expected.getMessage()).isEqualTo(EXPECTED_CLASS_CAST_MESSAGE);
          }
          try {
            String unusedString = value.bar;
            fail("Expected ClassCastException 6b");
          } catch (ClassCastException expected) {
            assertThat(expected.getMessage()).isEqualTo(EXPECTED_CLASS_CAST_MESSAGE);
          }
          try {
            String unusedString = value.baz();
            fail("Expected ClassCastException 6c");
          } catch (ClassCastException expected) {
            assertThat(expected.getMessage()).isEqualTo(EXPECTED_CLASS_CAST_MESSAGE);
          }
        }
      }.run();
    }
  }

  static class Foo {
    public String bar = "w00t";

    public String baz() {
      return bar;
    }
  }

  /** Test explicit references through a local variable qualifier. */
  @Test
  public void testExplicitField() {
    Liar<Foo> bug = new Liar<Foo>();

    // Should succeed
    Object unusedObject = bug.value;

    try {
      Foo unusedFoo = bug.value;
      fail("Expected ClassCastException 1");
    } catch (ClassCastException expected) {
      assertThat(expected.getMessage()).isEqualTo(EXPECTED_CLASS_CAST_MESSAGE);
    }
    try {
      String unusedString = bug.value.bar;
      fail("Expected ClassCastException 2");
    } catch (ClassCastException expected) {
      assertThat(expected.getMessage()).isEqualTo(EXPECTED_CLASS_CAST_MESSAGE);
    }
    try {
      String unusedString = bug.value.baz();
      fail("Expected ClassCastException 3");
    } catch (ClassCastException expected) {
      assertThat(expected.getMessage()).isEqualTo(EXPECTED_CLASS_CAST_MESSAGE);
    }
  }

  /** Test explicit references through a local variable qualifier. */
  @Test
  public void testExplicitMethod() {
    Liar<Foo> bug = new Liar<Foo>();

    // Should succeed
    Object unusedObject = bug.get();

    try {
      Foo unusedFoo = bug.get();
      fail("Expected ClassCastException 1");
    } catch (ClassCastException expected) {
      assertThat(expected.getMessage()).isEqualTo(EXPECTED_CLASS_CAST_MESSAGE);
    }
    try {
      String unusedString = bug.get().bar;
      fail("Expected ClassCastException 2");
    } catch (ClassCastException expected) {
      assertThat(expected.getMessage()).isEqualTo(EXPECTED_CLASS_CAST_MESSAGE);
    }
    try {
      String unusedString = bug.get().baz();
      fail("Expected ClassCastException 3");
    } catch (ClassCastException expected) {
      assertThat(expected.getMessage()).isEqualTo(EXPECTED_CLASS_CAST_MESSAGE);
    }
  }

  /** Test implicit references through an outer class. */
  @Test
  public void testOuterField() {
    new LiarFoo().testSuperField();
  }

  /** Test implicit references through an outer class. */
  @Test
  public void testOuterMethod() {
    new LiarFoo().testSuperMethod();
  }

  /** Test implicit references through a super class. */
  @Test
  public void testSuperField() {
    new LiarFoo().testSuperField();
  }

  /** Test implicit references through a super class. */
  @Test
  public void testSuperMethod() {
    new LiarFoo().testSuperMethod();
  }

  /** Test implicit references through a super class. */
  @Test
  public void testInternalAccess() {
    new LiarFoo().testInternalAccess();
  }

  private static <T> T[] newGenericArray() {
    return (T[]) new String[] {"Hello"};
  }

  private static <T, U> T getElement(U[] array, int index) {
    return (T) array[index];
  }

  @Test
  public void testGenericArrayCasts() {
    String[] array = newGenericArray();
    assertThat(array[0]).isEqualTo("Hello");
    assertThat(newGenericArray()[0]).isEqualTo("Hello");
    try {
      Integer[] intArray = newGenericArray();
      fail("Expected ClassCastException (1)");
    } catch (ClassCastException expected) {
      assertThat(expected.getMessage())
          .isEqualTo("[Ljava.lang.String; cannot be cast to [Ljava.lang.Integer;");
    }
    try {
      Integer n = getElement(newGenericArray(), 0);
      fail("Expected ClassCastException (2)");
    } catch (ClassCastException expected) {
      assertThat(expected.getMessage())
          .isEqualTo("java.lang.String cannot be cast to java.lang.Integer");
    }
  }
}
