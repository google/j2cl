/*
 * Copyright 2022 Google Inc.
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
package parallelhierarchies

import com.google.j2cl.integration.testing.Asserts.assertTrue

/**
 * Tests behavior of nested inner classes with parallel class hierarchies. One set of inner classes
 * with explicit constructors.
 */
class ExplicitConstructors {
  open inner class Outer1 {
    constructor() {}

    constructor(i: Int) {}

    open inner class Inner1 {
      constructor() {}

      constructor(i: Int) {}
    }
  }

  inner class Outer2 : Outer1 {
    constructor() : super() {}

    constructor(i: Int) : super(i) {}

    inner class Inner2 : Inner1 {
      constructor() : super() {}

      constructor(i: Int) : super(i) {}
    }
  }
}

/** Another set of inner classes with implicit constructors. */
class ImplicitConstructors {
  open inner class Outer1 {
    open inner class Inner1
  }

  inner class Outer2 : Outer1() {
    inner class Inner2 : Inner1()
  }
}

/** Another set with qualified calls to outer class fields. */
class QualifiedCalls {
  var name = "QualifiedCalls"

  open inner class Outer1 {
    open var name = "Outer1"

    open inner class Inner1 {
      open var name = "Inner1"
      var superResult: String

      constructor() {
        this.superResult = this.name + this@Outer1.name + this@QualifiedCalls.name
      }
    }
  }

  inner class Outer2 : Outer1() {
    override var name = "Outer2"

    inner class Inner2 : Inner1 {
      override var name = "Inner2"
      var result: String

      constructor() : super() {
        result = this.name + this@Outer2.name + this@QualifiedCalls.name + superResult
      }
    }
  }
}

fun main(vararg unused: String) {
  val test = QualifiedCalls().Outer2().Inner2()
  // test.result includes 'nullOuter2' because:
  //   - the super ctor is invoked before the child ctor;
  //   - on field references, Kotlin invokes the getter for the field;
  //   - the field getter is overridden in the child class;
  //   - the field on the child class is not initialized yet when the gettter is called;
  //
  // We got the same result if the code is executed on the JVM.
  // In java test.result is "Inner2Outer2QualifiedCallsInner1Outer1QualifiedCalls"
  assertTrue("Inner2Outer2QualifiedCallsnullOuter2QualifiedCalls" == test.result)
}
