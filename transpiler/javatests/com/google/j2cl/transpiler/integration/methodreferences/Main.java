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
  interface Producer<T> {
    T produce();
  }

  interface Predicate<T> {
    boolean apply(T parameter);
  }

  interface ArrayProducer<T> {
    T[] produce(int size);
  }

  static SomeObject m() {
    return new SomeObject();
  }

  static int instanceNumber;

  static class SomeObject {
    int someObjectInstanceNumber = Main.instanceNumber++;

    public Boolean is2() {
      return someObjectInstanceNumber == 2;
    }

    public Boolean is3() {
      return someObjectInstanceNumber == 3;
    }
  }

  class ObjectCapturingOuter {
    Main getMain() {
      return Main.this;
    }
  }

  void testConstructorReferences() {
    Producer<SomeObject> objectFactory = SomeObject::new;
    assertTrue(objectFactory.produce().someObjectInstanceNumber == 0);
    assertTrue(objectFactory.produce().someObjectInstanceNumber == 1);

    Producer<ObjectCapturingOuter> objectCapturingOuterProducer = ObjectCapturingOuter::new;
    assertTrue(objectCapturingOuterProducer.produce().getMain() == this);

    ArrayProducer<Object> arrayProducer = SomeObject[]::new;
    assertTrue(arrayProducer.produce(10).length == 10);
  }

  void testMethodReferences() {
    Producer<SomeObject> objectFactory = Main::m;
    assertTrue(objectFactory.produce().someObjectInstanceNumber == 0);
    assertTrue(objectFactory.produce().someObjectInstanceNumber == 1);

    // Qualified instance method, make sure that the evaluation of the qualifier only happens once.
    Producer<Boolean> booleanProducer = new SomeObject()::is2;
    assertTrue(booleanProducer.produce());
    assertTrue(booleanProducer.produce());

    // Unqualified SomeObject method
    Predicate<SomeObject> objectPredicate = SomeObject::is3;
    assertTrue(objectPredicate.apply(new SomeObject()));
    assertTrue(!objectPredicate.apply(new SomeObject()));
  }

  void testSuperMethodReferences() {
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

  public static void main(String[] args) {
    Main m = new Main();
    instanceNumber = 0;
    m.testSuperMethodReferences();
    instanceNumber = 0;
    m.testConstructorReferences();
    instanceNumber = 0;
    m.testMethodReferences();
  }
}
