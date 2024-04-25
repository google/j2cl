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
package captures;

import static com.google.j2cl.integration.testing.Asserts.assertEquals;
import static com.google.j2cl.integration.testing.Asserts.assertNotEquals;
import static com.google.j2cl.integration.testing.Asserts.assertNull;
import static com.google.j2cl.integration.testing.Asserts.assertTrue;

/** Test captures. */
public class Main {
  public static void main(String... args) {
    testVariableCapture();
    testVariableCapture_nested();
    testVariableCapture_indirect();
    // TODO(b/165431807): Uncomment when bug is fixed
    // testVariableCapture_nameClashes_inheritance();
    // testVariableCapture_nameClashes_nested();
    testParameterCapture();
    testOuterCapture();
    testOuterCapture_nested();
    testOuterCapture_indirect();
    testOuterCapture_defaultMethodSuper();
    testOuterCapture_inLambda();
    testOuterCapture_viaSuper();
    testOuterCapture_viaSuper_inLambda();
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
    final int anotherVariable = 2;
    class Outer {
      public int getField() {
        return field;
      }

      // TODO(b/215282471): anonymous class needs to pass the capture anotherVariable to Outer, but
      // when computing the captures it has not seen that Outer captures anotherVariable yet.
      // public Outer anonymous() {
      //    return new Outer() {};
      // }

      class Inner extends Outer {
        public int variable() {
          return variable;
        }
      }

      // Keep the only reference to the captured variable AFTER the nested classes to test that
      // the AST traversal in the resolution of captures handles the case.
      public int field = anotherVariable;
    }
    assertEquals(variable, new Outer().new Inner().variable());
    assertEquals(anotherVariable, new Outer().new Inner().getField());
    // assertEquals(anotherVariable, new Outer().anonymous().getField());
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

  // private static void testVariableCapture_nameClashes_inheritance() {
  //   int local = 3;
  //   class ClassCapturingLocal {
  //
  //     int returnLocal() {
  //       return local;
  //     }
  //
  //     final int returnOriginalLocal() {
  //       return local;
  //     }
  //   }
  //   class UnrelatedClass {
  //
  //     ClassCapturingLocal getInnerClass() {
  //       int local = 4;
  //       return new ClassCapturingLocal() {
  //         @Override
  //         int returnLocal() {
  //           return local;
  //         }
  //       };
  //     }
  //   }
  //
  //   ClassCapturingLocal outer = new ClassCapturingLocal();
  //   ClassCapturingLocal inner = new UnrelatedClass().getInnerClass();
  //   assertEquals(3, outer.returnLocal());
  //   assertEquals(4, inner.returnLocal());
  //   assertEquals(3, inner.returnOriginalLocal());
  //
  // }

  // private static void testVariableCapture_nameClashes_nested() {
  //   int local = 3;
  //   class ClassCapturingLocal {
  //     int returnLocal() {
  //       return local;
  //     }
  //
  //     final int returnOriginalLocal() {
  //       return local;
  //     }
  //
  //     ClassCapturingLocal getInnerClass() {
  //       int local = 4;
  //       return new ClassCapturingLocal() {
  //         @Override
  //         int returnLocal() {
  //           return local;
  //         }
  //       };
  //     }
  //   }
  //   ClassCapturingLocal outer = new ClassCapturingLocal();
  //   ClassCapturingLocal inner = outer.getInnerClass();
  //   assertEquals(3, outer.returnLocal());
  //   assertEquals(4, inner.returnLocal());
  //   assertEquals(3, inner.returnOriginalLocal());
  // }

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

  private static int TEN = 10;
  private static int TWENTY = 20;

  private static class Outer {
    int outerField = TEN;

    public Outer() {}

    public Outer(int outherField) {
      this.outerField = outherField;
    }

    class Intermediate {
      class Inner {
        public int captured() {
          return outerField;
        }
      }
    }

    // Even though this class extends Intermediate that captures the enclosing class, this class
    // itself does not capture it.
    static class IntermediateChild extends Intermediate {
      public IntermediateChild() {
        // When extending a class that captures the outer instance, there is always the option to
        // pass it explicitly as a qualifier of the super call. The subclass (IntermediateChild in
        // in this case) could be either static, in which case you will be required to pass a
        // qualifier to the super call, or an inner (non-static) class in which the qualifier
        // could be inferred.
        // Note that the subclass could even be an inner class with a totally unrelated enclosing
        // class in which case, again the qualifier of the super would be required.
        new Outer(TWENTY).super();
      }
    }
  }

  private static void testOuterCapture_nested() {
    Outer outer = new Outer();
    assertEquals(TEN, outer.new Intermediate().new Inner().captured());
    assertEquals(TWENTY, new Outer.IntermediateChild().new Inner().captured());
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

      class ClassNotIndirectlyCapturingOuter {
        Outer returnIndirectCapture() {
          return new Outer().new ClassCapturingOuter().capturedOuter();
        }
      }
    }
    Outer outer = new Outer();
    assertEquals(outer, outer.new ClassIndirectlyCapturingOuter().returnIndirectCapture());
    assertNotEquals(outer, outer.new ClassNotIndirectlyCapturingOuter().returnIndirectCapture());
  }

  interface Supplier {
    String get();
  }

  private static void testOuterCapture_inLambda() {
    class Outer {
      String m() {
        assertTrue(this instanceof Outer);
        return "Outer";
      }

      String n() {
        Supplier s = () -> this.m();
        return s.get();
      }
    }
    assertEquals("Outer", new Outer().n());
  }

  private static void testOuterCapture_viaSuper() {
    class Super {
      String m() {
        assertTrue(this instanceof Super);
        return "Super";
      }
    }

    class Sub extends Super {
      @Override
      String m() {
        return "Sub";
      }

      String n() {
        Supplier s =
            new Supplier() {
              @Override
              public String get() {
                return Sub.super.m();
              }
            };
        return s.get();
      }
    }
    assertEquals("Super", new Sub().n());
  }

  private static void testOuterCapture_viaSuper_inLambda() {
    class Super {
      String m() {
        assertTrue(this instanceof Super);
        return "Super";
      }
    }

    class Sub extends Super {
      @Override
      String m() {
        return "Sub";
      }

      String n() {
        Supplier s = () -> super.m();
        return s.get();
      }
    }
    assertEquals("Super", new Sub().n());
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
    Object unused = outer.new Capturer(o, outer);
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

  interface InterfaceWithDefaultMethod {
    String realName();

    default String name() {
      return realName();
    }
  }

  public static void testOuterCapture_defaultMethodSuper() {
    abstract class SuperOuter implements InterfaceWithDefaultMethod {}

    class Outer extends SuperOuter {
      @Override
      public String realName() {
        return "Outer";
      }

      @Override
      public String name() {
        throw new AssertionError("Outer.name() should never be called.");
      }

      class Inner extends Outer {
        @Override
        public String realName() {
          return "Inner";
        }

        public String outerName() {
          return Outer.super.name();
        }
      }
    }

    assertEquals("Outer", new Outer().new Inner().outerName());
  }
}
