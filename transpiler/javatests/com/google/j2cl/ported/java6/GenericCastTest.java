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
import static org.junit.Assert.assertThrows;

import org.junit.Test;
import org.junit.function.ThrowingRunnable;
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

          assertThrowsClassCastException(
              () -> {
                Foo unusedFoo = value;
              },
              Foo.class);

          assertThrowsClassCastException(
              () -> {
                String unusedString = value.bar;
              },
              Foo.class);

          assertThrowsClassCastException(
              () -> {
                String unusedString = value.baz();
              },
              Foo.class);
        }
      }.run();
    }

    @Test
    public void testOuterMethod() {
      new Runnable() {
        @Override
        public void run() {
          assertThrowsClassCastException(
              () -> {
                // Java semantics don't require a type erasure cast here but J2CL always inserts one
                // if needed to the inferred type of the expression disregaring the left hand side
                // type.
                Object unusedObject = get();
              },
              Foo.class);

          assertThrowsClassCastException(
              () -> {
                Foo unusedFoo = get();
              },
              Foo.class);

          assertThrowsClassCastException(
              () -> {
                String unusedString = get().bar;
              },
              Foo.class);

          assertThrowsClassCastException(
              () -> {
                String unusedString = get().baz();
              },
              Foo.class);
        }
      }.run();
    }

    public void testSuperField() {
      assertThrowsClassCastException(
          () -> {
            // Java semantics don't require a type erasure cast here but J2CL always inserts one
            // if needed to the inferred type of the expression disregaring the left hand side type.
            Object unusedObject = value;
          },
          Foo.class);

      assertThrowsClassCastException(
          () -> {
            Foo unusedFoo = value;
          },
          Foo.class);

      assertThrowsClassCastException(
          () -> {
            String unusedString = value.bar;
          },
          Foo.class);

      assertThrowsClassCastException(
          () -> {
            String unusedString = value.baz();
          },
          Foo.class);
    }

    public void testSuperMethod() {
      assertThrowsClassCastException(
          () -> {
            // Java semantics don't require a type erasure cast here but J2CL always inserts one
            // if needed to the inferred type of the expression disregaring the left hand side type.
            Object unusedObject = get();
          },
          Foo.class);

      assertThrowsClassCastException(
          () -> {
            Foo unusedFoo = get();
          },
          Foo.class);

      assertThrowsClassCastException(
          () -> {
            String unusedString = get().bar;
          },
          Foo.class);

      assertThrowsClassCastException(
          () -> {
            String unusedString = get().baz();
          },
          Foo.class);
    }

    public void testInternalAccess() {
      new Runnable() {
        @Override
        public void run() {
          assertThrowsClassCastException(
              () -> {
                // Java semantics don't require a type erasure cast here but J2CL always inserts one
                // if needed to the inferred type of the expression disregaring the left hand side
                // type.
                Object unusedObject = get();
              },
              Foo.class);

          assertThrowsClassCastException(
              () -> {
                Foo unusedFoo = get();
              },
              Foo.class);

          assertThrowsClassCastException(
              () -> {
                String unusedString = get().bar;
              },
              Foo.class);

          assertThrowsClassCastException(
              () -> {
                String unusedString = get().baz();
              },
              Foo.class);

          assertThrowsClassCastException(
              () -> {
                // Java semantics don't require a type erasure cast here but J2CL always inserts one
                // if needed to the inferred type of the expression disregaring the left hand side
                // type.
                Object unusedObject = value;
              },
              Foo.class);

          assertThrowsClassCastException(
              () -> {
                Foo unusedFoo = value;
              },
              Foo.class);

          assertThrowsClassCastException(
              () -> {
                String unusedString = value.bar;
              },
              Foo.class);

          assertThrowsClassCastException(
              () -> {
                String unusedString = value.baz();
              },
              Foo.class);
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

    assertThrowsClassCastException(
        () -> {
          // Java semantics don't require a type erasure cast here but J2CL always inserts one
          // if needed to the inferred type of the expression disregaring the left hand side type.
          Object unusedObject = bug.value;
        },
        Foo.class);

    assertThrowsClassCastException(
        () -> {
          Foo unusedFoo = bug.value;
        },
        Foo.class);

    assertThrowsClassCastException(
        () -> {
          String unusedString = bug.value.bar;
        },
        Foo.class);
    assertThrowsClassCastException(
        () -> {
          String unusedString = bug.value.baz();
        },
        Foo.class);
  }

  /** Test explicit references through a local variable qualifier. */
  @Test
  public void testExplicitMethod() {
    Liar<Foo> bug = new Liar<Foo>();
    assertThrowsClassCastException(
        () -> {
          // Java semantics don't require a type erasure cast here but J2CL always inserts one
          // if needed to the inferred type of the expression disregaring the left hand side type.
          Object unusedObject = bug.get();
        },
        Foo.class);

    assertThrowsClassCastException(
        () -> {
          Foo unusedFoo = bug.get();
        },
        Foo.class);
    assertThrowsClassCastException(
        () -> {
          String unusedString = bug.get().bar;
        },
        Foo.class);
    assertThrowsClassCastException(
        () -> {
          String unusedString = bug.get().baz();
        },
        Foo.class);
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

    assertThrowsClassCastException(
        () -> {
          Integer[] unused = newGenericArray();
        },
        Integer[].class);
    assertThrowsClassCastException(
        () -> {
          Integer unused = getElement(newGenericArray(), 0);
        },
        Integer.class);
  }

  @Test
  public void testArrayAccess() {
    assertThrowsClassCastException(
        () -> {
          Object unused = GenericCastTest.<String[]>newTArray()[0];
        },
        String[].class);
  }

  private static <T> T newTArray() {
    return (T) new Object[] {"Hello"};
  }

  @Test
  public void testAssignmentReference() {
    Container<String> container = (Container<String>) new Container(1);

    // Code like
    //     container.value = container.value;
    // does not require a cast per Java spec, since only the minimal casts to preserve type safety
    // are required. J2CL deviates from this policy and may emit more erasure casts than strictly
    // needed. These casts are still correct and improve code size due to giving more precise
    // typing to Closure.

    assertThrowsClassCastException(
        () -> {
          String unusedString = container.value = container.value;
        },
        String.class);
  }

  @Test
  public void testParenthesizedAssignmentReference() {
    assertThrowsClassCastException(
        () -> {
          Container<String> container = (Container<String>) new Container(1);
          String unusedString = (container.value = container.value);
        },
        String.class);
  }

  private static class Container<T> {
    Container(T value) {
      this.value = value;
    }

    T value;

    T getValue() {
      return value;
    }
  }

  private static void assertThrowsClassCastException(ThrowingRunnable runnable, Class<?> toClass) {
    assertThat(assertThrows(ClassCastException.class, runnable))
        .hasMessageThat()
        .endsWith("cannot be cast to " + toClass.getName());
  }
}
