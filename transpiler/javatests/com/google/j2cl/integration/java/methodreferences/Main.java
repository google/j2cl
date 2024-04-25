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
package methodreferences;

import static com.google.j2cl.integration.testing.Asserts.assertEquals;
import static com.google.j2cl.integration.testing.Asserts.assertFalse;
import static com.google.j2cl.integration.testing.Asserts.assertThrowsClassCastException;
import static com.google.j2cl.integration.testing.Asserts.assertThrowsNullPointerException;
import static com.google.j2cl.integration.testing.Asserts.assertTrue;

public class Main {
  public static void main(String... args) {
    testSuperMethodReferences();
    testConstructorReferences();
    testInstanceMethodReferences();
    testStaticMethodReferences();
    testVarargs();
    testQualifierEvaluation();
    testQualifierEvaluation_array();
    testQualifierEvaluation_observeUninitializedField();
    testQualifierEvaluation_implicitObjectMethods();
  }

  interface Function<I, O> {
    O apply(I a);
  }

  interface Producer<T> {
    T produce();
  }

  interface Predicate<T> {
    boolean apply(T parameter);
  }

  interface ArrayProducer<T> {
    T[] produce(int size);
  }

  static class Outer {
    static int instanceNumber;

    static class SomeObject {
      int someObjectInstanceNumber = Outer.instanceNumber++;

      public Boolean isInstanceNumber2() {
        return someObjectInstanceNumber == 2;
      }

      public Boolean isInstanceNumber3() {
        return someObjectInstanceNumber == 3;
      }
    }

    class ObjectCapturingOuter {
      Outer getOuter() {
        return Outer.this;
      }
    }

    Producer<ObjectCapturingOuter> objectCapturingOuterFactory() {
      // Constructor member reference with implicit outer instance capture.
      return ObjectCapturingOuter::new;
    }
  }

  private static void testConstructorReferences() {
    Producer<Outer.SomeObject> objectFactory = Outer.SomeObject::new;
    assertTrue(objectFactory.produce().someObjectInstanceNumber == 0);
    assertTrue(objectFactory.produce().someObjectInstanceNumber == 1);

    Outer outer = new Outer();
    Producer<Outer.ObjectCapturingOuter> objectCapturingOuterProducer =
        outer.objectCapturingOuterFactory();
    assertTrue(objectCapturingOuterProducer.produce().getOuter() == outer);

    ArrayProducer<Object> arrayProducer = Outer.SomeObject[]::new;
    assertTrue(arrayProducer.produce(10).length == 10);
  }

  private static void testInstanceMethodReferences() {
    // Qualified instance method, make sure that the evaluation of the qualifier only happens once.
    Producer<Boolean> booleanProducer = new Outer.SomeObject()::isInstanceNumber2;
    assertTrue(booleanProducer.produce());
    assertTrue(booleanProducer.produce());

    // Unqualified SomeObject method
    Predicate<Outer.SomeObject> objectPredicate = Outer.SomeObject::isInstanceNumber3;
    assertTrue(objectPredicate.apply(new Outer.SomeObject()));
    assertTrue(!objectPredicate.apply(new Outer.SomeObject()));
  }

  private static void testStaticMethodReferences() {
    Producer<Integer> integerFactory = Main::returnSequenceAsInteger;
    assertTrue(integerFactory.produce() == 0);
    assertTrue(integerFactory.produce() == 1);

    // Lambda with autoboxing.
    integerFactory = Main::returnSequenceAsInt;
    assertTrue(integerFactory.produce() == 2);
    assertTrue(integerFactory.produce() == 3);
  }

  private static int sequenceNumber = 0;

  private static Integer returnSequenceAsInteger() {
    return sequenceNumber++;
  }

  private static String returnSequenceAsString() {
    return "" + sequenceNumber++;
  }

  private static Integer returnSequenceAsInt() {
    return sequenceNumber++;
  }

  private static void testSuperMethodReferences() {
    class A {
      String introduce() {
        return "I am A.";
      }
    }

    class SubA extends A {
      @Override
      String introduce() {
        return "I am SubA.";
      }

      Producer<String> superIntroducer() {
        return super::introduce;
      }
    }

    assertTrue(new SubA().superIntroducer().produce().equals("I am A."));
  }

  /** Tests different qualifier shapes since J2CL optimizes some of these constructs. */
  private static void testQualifierEvaluation() {
    // Variable reference
    String variable = "Hello";
    Producer<String> stringProducer = variable::toLowerCase;
    variable = "GoodBye";
    // Check that the evaluation of variable happens at taking the reference not at the evaluation
    // of the lambda.
    assertEquals("Hello".toLowerCase(), stringProducer.produce());

    // Field reference.
    class Local {
      String field = "Hello";
    }
    Local local = new Local();
    stringProducer = local.field::toLowerCase;
    local.field = "GoodBye";
    // Check that the evaluation of variable happens at taking the reference not at the evaluation
    // of the lambda.
    assertEquals("Hello".toLowerCase(), stringProducer.produce());

    // Static field reference
    assertFalse(isInitialized);
    stringProducer = InnerClassWithStaticInitializer.field::toString;
    // Class initializer should be triggered by just creating the reference.
    assertTrue(isInitialized);

    // Instantiation
    stringProducer = new Local()::toString;
    assertEquals(stringProducer.produce(), stringProducer.produce());

    // Method call
    stringProducer = returnSequenceAsInteger()::toString;
    assertEquals(stringProducer.produce(), stringProducer.produce());

    // Binary expression
    stringProducer = (returnSequenceAsString() + returnSequenceAsString())::toLowerCase;
    assertEquals(stringProducer.produce(), stringProducer.produce());

    // Cast expression
    assertThrowsClassCastException(
        () -> {
          Producer<String> sp = ((String) (Object) returnSequenceAsInteger())::toLowerCase;
        });

    // Array access
    stringProducer = returnsArray()[0]::toLowerCase;
    assertEquals(stringProducer.produce(), stringProducer.produce());

    // Ternary conditional
    stringProducer =
        (returnSequenceAsInt() > 0 ? returnSequenceAsString() : returnSequenceAsString())
            ::toLowerCase;
    assertEquals(stringProducer.produce(), stringProducer.produce());

    // Object instantiation
    stringProducer = new Integer(returnSequenceAsInt())::toString;
    assertEquals(stringProducer.produce(), stringProducer.produce());
  }

  interface Interface {}

  interface InterfaceWithHashcode {
    int hashCode();
  }

  private static void testQualifierEvaluation_implicitObjectMethods() {
    Interface i = new Interface() {};
    Producer<String> stringProducer = i::toString;
    assertEquals(i.toString(), stringProducer.produce());

    Producer<Integer> integerProducer = i::hashCode;
    assertEquals(i.hashCode(), integerProducer.produce().intValue());

    Function<Object, Boolean> equalityTester = i::equals;
    assertTrue(equalityTester.apply(i));
    assertFalse(equalityTester.apply(new Object() {}));

    InterfaceWithHashcode interfaceWithHashcode = new InterfaceWithHashcode() {};
    integerProducer = interfaceWithHashcode::hashCode;
    assertEquals(interfaceWithHashcode.hashCode(), integerProducer.produce().intValue());
  }

  @SuppressWarnings({"ArrayToString", "ArrayHashCode"})
  private static void testQualifierEvaluation_array() {
    // Array methods.
    String[] array = {returnSequenceAsString()};
    Producer<String> stringProducer = array::toString;
    assertEquals(array.toString(), stringProducer.produce());

    Producer<Integer> integerProducer = array::hashCode;
    assertEquals(array.hashCode(), integerProducer.produce().intValue());

    Function<Object, Boolean> equalityTester = array::equals;
    assertTrue(equalityTester.apply(array));
    assertFalse(equalityTester.apply(new int[0]));
  }

  private static String[] returnsArray() {
    return new String[] {returnSequenceAsString()};
  }

  private static boolean isInitialized;

  private static class InnerClassWithStaticInitializer {
    static {
      isInitialized = true;
    }

    private static final Object field = new Object();
  }

  private static void testQualifierEvaluation_observeUninitializedField() {
    class Super {
      Super() {
        // Call polymorphic init to allow referencing final subclass fields before they are
        // initialized
        init();
      }

      void init() {}
    }
    class Sub extends Super {

      private Producer<String> stringProducer;
      private final Integer qualifier = new Integer(2);

      void init() {
        // Construct the method reference before qualifier is initialized.
        stringProducer = qualifier::toString;
      }
    }

    assertThrowsNullPointerException(() -> new Sub().stringProducer.produce());
  }

  private static int stringCount(String... strings) {
    return strings.length;
  }

  private static int numberCount(Number... numbers) {
    return numbers.length;
  }

  private static int intCount(Integer... integers) {
    return integers.length;
  }

  private interface StringStringToIntFunction {
    int toInt(String s1, String s2);
  }

  private interface StringArrayToIntFunction {
    int toInt(String[] strings);
  }

  private interface ToIntFunction<T> {
    int toInt(T strings);
  }

  private static void testVarargs() {
    StringStringToIntFunction fun = Main::stringCount;
    assertTrue(fun.toInt("foo", "bar") == 2);
    StringArrayToIntFunction fun2 = Main::stringCount;
    assertTrue(fun2.toInt(new String[] {"foo", "bar"}) == 2);

    ToIntFunction<? super Integer[]> fun3 = Main::numberCount;
    assertTrue(fun3.toInt(new Integer[] {1, 2}) == 2);
    fun3 = Main::intCount;
    assertTrue(fun3.toInt(new Integer[] {1, 2}) == 2);
  }
}
