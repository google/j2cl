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
package com.google.j2cl.transpiler.integration.captures;

import static com.google.j2cl.transpiler.utils.Asserts.assertEquals;
import static com.google.j2cl.transpiler.utils.Asserts.assertNull;
import static com.google.j2cl.transpiler.utils.Asserts.assertTrue;

/** Test captures. */
public class Main {
  public static void main(String[] args) {
    testVariableCapture();
    testVariableCapture_nested();
    testVariableCapture_indirect();
    testParameterCapture();
    testOuterCapture();
    testOuterCapture_nested();
    testOuterCapture_indirect();
    testCaptures_constructor();
    testCaptures_parent();
    testCaptures_anonymous();
    testCaptures_uninitializedNotObservable();
    testCaptures_fieldReferences();
  }

  private static void testVariableCapture() {
    final int variable = 1;
    class Inner {
      public int variable() {
        return variable;
      }
    }
    assertEquals(variable, new Inner().variable());
  }

  private static void testVariableCapture_nested() {
    final int variable = 1;
    class Outer {
      class Inner {
        public int variable() {
          return variable;
        }
      }
    }
    assertEquals(variable, new Outer().new Inner().variable());
  }

  private static void testVariableCapture_indirect() {
    int local = 3;
    class ClassCapturingLocal {
      int returnLocal() {
        return local;
      }
    }

    class ClassIndirectlyCapturingLocal {
      int returnIndirectCapture() {
        return new ClassCapturingLocal().returnLocal();
      }
    }
    assertTrue(new ClassIndirectlyCapturingLocal().returnIndirectCapture() == 3);
  }

  private static void testCaptures_constructor() {
    final int variable = 1;
    // local class with non-default constructor and this() call
    class Local {

      public int field;

      public Local(int a, int b) {
        field = variable + a + b;
      }

      public Local(int a) {
        this(a, variable);
      }
    }
    int a = 10, b = 20;
    assertEquals(variable + a + b, new Local(a, b).field);
    assertEquals(variable + a + variable, new Local(a).field);
  }

  private static void testParameterCapture() {
    class Outer {
      int captureInInner(int parameter) {
        class Inner {
          public int parameter() {
            return parameter;
          }
        }
        return new Inner().parameter();
      }
    }

    int parameter = 10;
    assertEquals(parameter, new Outer().captureInInner(10));
  }

  private static void testOuterCapture() {
    class Outer {
      class Inner {
        public Outer captured() {
          return Outer.this;
        }
      }
    }
    Outer outer = new Outer();
    assertEquals(outer, outer.new Inner().captured());
  }

  private static int ten = 10;

  private static class Outer {
    int outerField = ten;

    class Intermediate {
      class Inner {
        public int captured() {
          return outerField;
        }
      }
    }
  }

  private static void testOuterCapture_nested() {
    Outer outer = new Outer();
    assertEquals(ten, outer.new Intermediate().new Inner().captured());
  }

  private static void testOuterCapture_indirect() {
    class Outer {
      class ClassCapturingOuter {
        Outer capturedOuter() {
          return Outer.this;
        }
      }

      class ClassIndirectlyCapturingOuter {
        Outer returnIndirectCapture() {
          return new ClassCapturingOuter().capturedOuter();
        }
      }
    }
    Outer outer = new Outer();
    assertEquals(outer, outer.new ClassIndirectlyCapturingOuter().returnIndirectCapture());
  }

  private static void testCaptures_parent() {
    final int variable = 10;
    class Parent {
      int variablePlusPar(int parameter) {
        return variable + parameter;
      }
    }

    class AChild extends Parent {}

    class AnotherChild extends Parent {
      public AnotherChild() {
        super();
      }
    }

    int parameter = 15;
    assertEquals(variable + parameter, new AChild().variablePlusPar(parameter));
    assertEquals(variable + parameter, new AnotherChild().variablePlusPar(parameter));
  }

  interface AnonymousInterface {
    void foo();
  }

  private abstract static class SomeClass {
    public int f;

    SomeClass(int f) {
      this.f = f;
    }

    public int foo() {
      return f;
    }
  }

  public static void testCaptures_anonymous() {
    final Object[] instances = new Object[3];
    class Outer {
      AnonymousInterface i =
          new AnonymousInterface() {
            Object cachedThis = this;

            @Override
            public void foo() {
              instances[0] = this;
              instances[1] = cachedThis;
              instances[2] = Outer.this;
            }
          };
    }

    Outer outer = new Outer();
    outer.i.foo();

    assertTrue(instances[0] == outer.i);
    assertTrue(instances[1] == outer.i);
    assertTrue(instances[2] == outer);

    assertTrue(new SomeClass(3) {}.foo() == 3);
  }

  public static void testCaptures_uninitializedNotObservable() {
    Object o = new Object();
    abstract class Parent {
      Parent(Object oValue, Object outerValue) {
        observe(oValue, outerValue);
      }

      abstract void observe(Object oValue, Object outerValue);
    }

    class Outer {
      class Capturer extends Parent {
        // Sentinel to make sure that observe() is called before getting the chance to initialize
        // the instance.
        Object initializedField = new Object();

        // Pass the values on construction to avoid relying on captures which is what we will test.
        Capturer(Object oValue, Object outerValue) {
          super(oValue, outerValue);
        }

        @Override
        void observe(Object oValue, Object outerValue) {
          assertEquals(oValue, o);
          assertEquals(outerValue, Outer.this);
          // Observed uninitialized. Make sure that instance initialization has not been run.
          assertNull(initializedField);
        }
      }
    }
    Outer outer = new Outer();
    outer.new Capturer(o, outer);
  }

  private static class FieldReferencesOuter {
    String text;

    FieldReferencesOuter() {
      this.text = "Outer";
    }

    FieldReferencesOuter(String text) {
      this.text = text;
    }

    class Inner extends FieldReferencesOuter {
      Inner() {
        super("InnerSuper");
      }

      class InnerInner {
        String getImplicitText() {
          // Refers to Inner.super.text implicitly.
          return text;
        }

        // Refers to FieldReferencesOuter.text explicitly.
        String getOuterText() {
          return FieldReferencesOuter.this.text;
        }
      }
    }
  }

  public static void testCaptures_fieldReferences() {

    assertEquals(
        "InnerSuper", new FieldReferencesOuter().new Inner().new InnerInner().getImplicitText());
    assertEquals("Outer", new FieldReferencesOuter().new Inner().new InnerInner().getOuterText());
  }
}
