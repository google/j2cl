/*
 * Copyright 2022 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package wasmimmutablefields;

import static com.google.j2cl.integration.testing.Asserts.assertEquals;
import static com.google.j2cl.integration.testing.Asserts.assertNull;

/**
 * Tests that the optimization that allows to mark fields immutable in the struct does not affect
 * Java semantics.
 */
public class Main {

  public static void main(String... args) throws Exception {
    testImmutableFieldsBackoff_thisEscapesAsQualifier();
    testImmutableFieldsBackoff_thisEscapesAsArgument();
    testImmutableFieldsBackoff_thisEscapesToField();
    testImmutableFieldsBackoff_superEscapesToMethodCall();
  }

  private static void testImmutableFieldsBackoff_thisEscapesAsQualifier() {
    class TestClass {
      public final String optimizeableString;
      public final String observedOptimizableString = observe();

      {
        optimizeableString = "Hello, World!";
      }

      private String observe() {
        return optimizeableString;
      }
    }
    TestClass testClass = new TestClass();
    assertNull(testClass.observedOptimizableString);
    assertEquals("Hello, World!", testClass.optimizeableString);
  }

  private static void testImmutableFieldsBackoff_thisEscapesAsArgument() {
    class TestClass implements Observer {
      public final String optimizeableString;
      // Use a static method to make sure "this" escapes from a parameter and not as the implicit
      // qualifier of a call.
      public final String observedOptimizableString = observeThroughStaticMethod(this);

      {
        optimizeableString = "Hello, World!";
      }

      @Override
      public String observe() {
        return optimizeableString;
      }
    }

    TestClass testClass = new TestClass();
    assertNull(testClass.observedOptimizableString);
    assertEquals("Hello, World!", testClass.optimizeableString);
  }

  interface Observer {
    String observe();
  }

  private static String observeThroughStaticMethod(Observer observer) {
    return observer.observe();
  }

  private static void testImmutableFieldsBackoff_thisEscapesToField() {
    class Parent {
      public final Parent escapingThis = this;
    }

    class TestClass extends Parent {

      public final String optimizeableString;
      public final String observedOptimizableString;

      TestClass() {
        // While you can not refer to a final field before its initialization directly, you
        // can if "this" has escaped.
        observedOptimizableString = ((TestClass) escapingThis).optimizeableString;
        optimizeableString = "Hello, World!";
      }
    }

    TestClass testClass = new TestClass();
    assertNull(testClass.observedOptimizableString);
    assertEquals("Hello, World!", testClass.optimizeableString);
  }

  private static void testImmutableFieldsBackoff_superEscapesToMethodCall() {
    class Parent {
      void callObserve() {
        observe();
      }

      void observe() {}
    }

    class TestClass extends Parent {

      public final String optimizeableString;
      public String observedOptimizableString;

      TestClass() {
        super.callObserve();
        optimizeableString = "Hello, World!";
      }

      void observe() {
        this.observedOptimizableString = optimizeableString;
      }
    }

    TestClass testClass = new TestClass();
    assertNull(testClass.observedOptimizableString);
    assertEquals("Hello, World!", testClass.optimizeableString);
  }
}
