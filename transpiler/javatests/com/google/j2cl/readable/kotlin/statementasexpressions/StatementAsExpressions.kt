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
package statementasexpressions

// Use a mutable to level variable to avoid if/else branch pruning by the kotlin frontend.
var TRUE = true

fun testReturnsFromCondition() {
  // You can return from a condition.
  if (return) 1 else 2
}

fun testThrowsFromCondition() {
  // You can also throw from a condition.
  if (throw IllegalStateException()) 1 else 2
}

fun testConditions() {
  if (
    // This statement in the if condition needs to be hoisted.
    if (TRUE) {
      true
    } else {
      throw RuntimeException()
    }
  ) {
    return
  }

  while (
    // Same for conditions in while.
    if (TRUE) {
      true
    } else {
      throw RuntimeException()
    }
  ) {}

  do while (
    // Same for conditions in do while.
    if (TRUE) {
      true
    } else {
      throw RuntimeException()
    }
  )
}

fun m(a: Int, b: Int, c: Int) {
  return
}

fun testReturnVoid() {
  // A return can have an expression or statement even if it returns void.
  return m(1, 3, 4)
}

fun testReturnReturn() {
  return return m(1, 3, 4)
}

fun testMemberAccess() {
  class X(var field: Int) {
    fun m(a: Int, b: Int, c: Int) {}
  }

  val x = X(0)
  // Access a field with a qualifier that is a statement.
  val b =
    if (TRUE) {
        X(0)
      } else {
        x
      }
      .field

  // Call a method with a qualifier that is a statement. This forces the evaluation of the
  // qualifier and all the arguments to be extracted. These could be optimized, see b/228703278.
  if (TRUE) {
      X(0)
    } else {
      x
    }
    .m(3, 4, 5)

  // Call a method with a statement as one of its arguments. This forces the evaluation of the
  // qualifier and all the arguments to be extracted. These could be optimized, see b/228703278.
  x.m(3, if (true) 4 else 5, 5)
}

fun testVariableDeclaration() {
  val b =
    if (TRUE) {
      32
    } else {
      throw RuntimeException()
    }
}

val fieldAssignedAStatement: Int =
  if (TRUE) {
    1
  } else {
    throw RuntimeException()
  }

fun testUnaryExpression() {
  -if (TRUE) {
    1
  } else {
    throw RuntimeException()
  }
}

fun testBinaryExpression() {
  1 -
    if (TRUE) {
      1
    } else {
      throw RuntimeException()
    }
}

class ClassForFieldInitialization {
  var witnessField: Int = 1

  // Observes the value of witnessField before it gets overwritten by the statements in
  // fieldInitializedWithStatement
  val observerPreField = witnessField
  val fieldInitializedWithStatement =
    if (true) {
      // Mutate witnessField
      witnessField = 2
      // This is the value of the intializer.
      3
    } else {
      throw AssertionError()
    }

  // Observes the value of witnessField after it gets overwritten by the statements in
  // fieldInitializedWithStatement
  val observerPostField = witnessField
}

private fun testStatement_inConditionOfWhileStatement() {
  var a = 0
  var b = 0
  while (
    if (b < 4) {
      // Since this branch will return true it will execute once per loop iteration. There will be
      // four iterations hence this will execute four times
      b += 1
      true
    } else {
      // Since this branch will return false and for the loop exit, it will only execute once.
      a += 100
      false
    }
  ) {
    // Executes once per loop iteration. There will be 4 iterations, hence this will execute four
    // times
    a += 10
  }
}

private fun methodThatReturnsVoid() {}

private fun testReturn_withVoidExpression() {
  return methodThatReturnsVoid()
}

class ClassWithInitializer {
  var field: Int = 1

  init {
    field =
      when ('1') {
        '0',
        '1' -> 1
        '2' -> throw AssertionError()
        else -> 3
      }
  }
}

fun testStatement_inIsOperator() {
  var boolean = (if (TRUE) 1 else throw java.lang.AssertionError()) is Int
}

fun acceptsUnitFunction(x: (Any) -> Unit) {}

fun consumeAndReturnDouble(d: Double) = d

fun testDecompositionOfIfReturningUnit(data: Any) {
  // Block Decomposer should not create a temporary variable of type Unit and assign it the
  // returned value of consumeAndReturnDouble
  acceptsUnitFunction {
    if (data is Double) {
      consumeAndReturnDouble(data)
    }
  }

  // Ensure we do decompose if/else expression returning Unit correctly
  val foo: Unit =
    if (data is Double) {
      methodThatReturnsVoid()
    } else {
      throw AssertionError()
    }
}

fun testDecompositionOfTryReturningUnit() {
  // Block Decomposer should not create a temporary variable of type Unit and assign it the
  // returned value of consumeAndReturnDouble
  acceptsUnitFunction {
    try {
      consumeAndReturnDouble(1.0)
    } finally {}
  }

  // Ensure we do decompose try-expression returning Unit correctly
  val foo: Unit =
    try {
      methodThatReturnsVoid()
    } catch (e: RuntimeException) {
      methodThatReturnsVoid()
    } finally {}

  // Ensure we do decompose try-expression returning non-Unit correctly.
  var x = 10
  val bar: Double =
    try {
      consumeAndReturnDouble(1.0)
    } catch (e: RuntimeException) {
      consumeAndReturnDouble(2.0)
    } finally {
      // The expression in the finally block doesn't impact the result of the try.
      x++
    }
}

enum class EnumWithInitStatement(val s: String) {
  FOO(if (TRUE) "FOO" else throw java.lang.AssertionError()),
  BAR("BARTOOLONG".let { if (it.length > 3) it.substring(0, 3) else it }),
  BAZ("BAZ".run { if (TRUE) this else throw java.lang.AssertionError() }) {
    override fun foo(): String = "BAZ"
  };

  open fun foo(): String = "FOO"
}
