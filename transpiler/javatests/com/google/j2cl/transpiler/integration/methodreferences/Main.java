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
package com.google.j2cl.transpiler.integration.methodreferences;

import static com.google.j2cl.transpiler.utils.Asserts.assertTrue;

public class Main {
  public static void main(String[] args) {
    testSuperMethodReferences();
    testConstructorReferences();
    testMethodReferences();
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

  static Outer.SomeObject returnsSomeObject() {
    return new Outer.SomeObject();
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
    Outer.instanceNumber = 0;
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

  private static void testMethodReferences() {
    Outer.instanceNumber = 0;
    Producer<Outer.SomeObject> objectFactory = Main::returnsSomeObject;
    assertTrue(objectFactory.produce().someObjectInstanceNumber == 0);
    assertTrue(objectFactory.produce().someObjectInstanceNumber == 1);

    // Qualified instance method, make sure that the evaluation of the qualifier only happens once.
    Producer<Boolean> booleanProducer = new Outer.SomeObject()::isInstanceNumber2;
    assertTrue(booleanProducer.produce());
    assertTrue(booleanProducer.produce());

    // Unqualified SomeObject method
    Predicate<Outer.SomeObject> objectPredicate = Outer.SomeObject::isInstanceNumber3;
    assertTrue(objectPredicate.apply(new Outer.SomeObject()));
    assertTrue(!objectPredicate.apply(new Outer.SomeObject()));
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
}
