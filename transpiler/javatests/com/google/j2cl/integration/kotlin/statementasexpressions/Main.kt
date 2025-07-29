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

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.assertThrowsClassCastException
import com.google.j2cl.integration.testing.Asserts.assertTrue
import com.google.j2cl.integration.testing.Asserts.fail
import java.lang.AssertionError
import java.lang.IllegalStateException

fun main(vararg unused: String) {
  testStatement_inUnaryExpression()
  testStatement_inBinaryExpression()
  testStatement_inAssignment()
  testStatement_inInitializerOfVariableDeclaration()
  testStatement_inTopLevelPropertyInitialization()
  testStatement_inInstancePropertyIntialization()
  testStatement_inFieldAssignmentInSetter()
  testStatement_inConditionOfIfStatement()
  testStatement_inConditionOfWhileStatement()
  testStatement_inConditionOfDoWhileStatement()
  testStatement_inForLoop_arrayExpression()
  testStatement_inForLoop_iterableExpression()
  testStatement_inAsOperator()
  testStatement_inIsOperator()
  testStatement_inWhenCondition()
  testStatement_inWhenCase()
  testStatement_inWhenExpression()
  testStatement_inElvis()
  testStatement_inInOperator()
  testStatement_inRangeOperator()

  testStatement_asQualifierInPropertyAccess()
  testStatement_asQualifierOfMethodCall()
  testStatement_asParameterToSuperCall()
  testStatement_asDefaultParameterValue()
  testStatement_asArrayExpression()
  testStatement_asArrayIndex()
  testStatement_asEnumEntryInitializer()

  testReturn_asExpression()
  testThrow_asExpression()
  testTryCatch_asExpression()
  testBreak_asExpression()
  testContinue_asExpression()
  testIf_asExpression()

  testReturn_withVoidExpression()

  testAnonymousInitializer()
  testStatement_asEnumEntryInitializer()
}

// Use a mutable to level variable to avoid if/else branch pruning by the kotlin frontend.
var TRUE = true

private fun testStatement_inBinaryExpression() {
  var a = 1
  val b =
    (1 +
      // Statement that will be extracted, that has side effects and makes the other statements
      // order dependent.
      if (TRUE) {
        // Rewrite "a" so that if this statement is executed in the wrong order, variable a
        // will have the wrong value
        a = 2
        // The return value of this branch is always 3.
        3
      } else {
        // This branch in never reach and by having a throw it needs to be hoisted
        throw AssertionError()
      }) +
      // Second statement to be extracted, that also has a side effect.
      if (!TRUE) {
        // This branch in never reach and by having a throw it needs to be hoisted
        throw AssertionError()
      } else {
        // Side effect on "a" to test the order of the extracted statements.
        a += 5
        // The return value of this branch is always 2.
        2
      }
  // Tests that the order of the extracted statements is correct by expecting
  // the sequence a = 2; a += 5 to be executed in that order.
  assertEquals(7, a)
  // Tests that the results of the expression is the expected one: 1 + 3 + 2
  assertEquals(6, b)
}

private fun testStatement_inAssignment() {
  var three: Int

  three =
    if (TRUE) {
      3
    } else {
      throw AssertionError()
    }
  assertEquals(3, three)
}

var property: Int = if (TRUE) 1 else throw AssertionError()

private fun testStatement_inTopLevelPropertyInitialization() {
  assertEquals(1, property)
}

var propertyWithSetter: Int = 0
  set(value) {
    field =
      if (TRUE) {
        value + 1
      } else {
        throw AssertionError()
      }
  }

private fun testStatement_inFieldAssignmentInSetter() {
  // Tests the assignment to the backing field in the setter of property, since that is the only
  // place where field accesses show.
  propertyWithSetter = 4 // The field setter will increment the value by one.
  assertEquals(5, propertyWithSetter)
}

private fun testStatement_inUnaryExpression() {
  val one =
    -if (TRUE) {
      1
    } else {
      throw AssertionError()
    }
  assertEquals(-1, one)
}

private fun returnFromCondition(): Int {
  // Since the evaluation of the condition happens first, the return in the condition is actually
  // effective.
  if (return 1) throw AssertionError()
}

private fun testReturn_asExpression() {
  assertEquals(1, returnFromCondition())
}

private fun testReturn_withVoidExpression() {
  class ReturnIncrementingField(var field: Int) {
    fun incField() {
      field++
    }

    fun returnsVoid() {
      // A method that returns void can have a return with an expression that will be evaluated
      // before the return.
      return incField()
    }
  }

  val o = ReturnIncrementingField(3)
  o.returnsVoid()
  assertEquals(4, o.field)
}

private fun testStatement_inConditionOfIfStatement() {
  var a = 0
  if (
    if (TRUE) {
      // This is the branch always taken and makes the outer if condition true.
      a = 2
      true
    } else {
      // Branch is not taken.
      throw AssertionError()
    }
  ) {
    a += 100 // this code executes => a becomes 102
  }

  assertEquals(102, a)
}

private fun testStatement_inConditionOfWhileStatement() {
  var a = 0
  var b = 0
  while (
    if (b < 4) {
      // Since this branch will return true it will execute once per loop iteration. There will be
      // three iterations hence this will execute three times
      b += 1
      true
    } else {
      // This won't be executed as the break will stop the while loop on the third iteration.
      a += 100
      false
    }
  ) {
    if (b == 0) continue
    if (b > 2) break
    // This will execute two times
    a += 10
  }

  assertEquals(20, a)
  assertEquals(3, b)
}

private fun testStatement_inConditionOfDoWhileStatement() {
  var a = 0
  var b = 0
  do {
    if (b == 0) continue
    if (b > 2) break
    // This will execute two times
    a += 10
  } while (
    if (b < 4) {
      // Since this branch will return true it will execute once per loop iteration. There will be
      // three iterations hence this will execute three times
      b += 1
      true
    } else {
      // This won't be executed as the break will stop the while loop on the third iteration.
      a += 100
      false
    }
  )

  assertEquals(20, a)
  assertEquals(3, b)
}

private fun testStatement_inForLoop_arrayExpression() {
  var sum = 0
  var executedTimes = 0
  for (o in
    if (TRUE) {
      executedTimes++
      arrayOf(1, 2, 3)
    } else throw AssertionError()) {
    sum += o
  }
  assertEquals(6, sum)
  assertEquals(1, executedTimes)
}

private fun testStatement_inForLoop_iterableExpression() {
  var sum = 0
  var executedTimes = 0
  // Iterables of different types that are Iterable<Int>, Array does not implement Iterable, so
  // they can not be mixed in this way.
  for (i in 0..1) {
    for (o in
      if (i == 0) {
        executedTimes++
        setOf(1, 2, 3)
      } else {
        executedTimes++
        4..6
      }) {
      sum += o
    }
  }
  assertEquals(21, sum)
  assertEquals(2, executedTimes)
}

fun testStatement_inAsOperator() {
  assertThrowsClassCastException({ (if (TRUE) 1 else throw AssertionError()) as String })
}

fun testStatement_inIsOperator() {
  assertTrue((if (TRUE) "" else throw AssertionError()) is String)
}

fun testStatement_inWhenCondition() {
  val result =
    when (if (TRUE) 1 else throw AssertionError()) {
      1 -> 2
      else -> 3
    }
  assertEquals(2, result)
}

var ONE: Int = 1

fun testStatement_inWhenCase() {
  var result =
    when {
      (if (TRUE) 1 else throw AssertionError()) == 1 -> 2
      else -> 3
    }
  assertEquals(2, result)

  result =
    when (ONE) {
      (if (TRUE) 1 else throw AssertionError()) -> 2
      else -> 3
    }
  assertEquals(2, result)
}

fun testStatement_inWhenExpression() {
  val result =
    when (ONE) {
      1 -> if (TRUE) 2 else throw AssertionError()
      else -> throw AssertionError()
    }
  assertEquals(2, result)
}

// Use mutable nullable variables to prevent the kotlinc frontend from optimizing the code.
var NULL: Int? = null
var nullableOne: Int? = ONE

fun testStatement_inElvis() {
  var result: Int? = (if (TRUE) nullableOne else throw AssertionError()) ?: throw AssertionError()
  assertEquals(nullableOne, result)

  result = NULL ?: (if (TRUE) ONE else throw AssertionError())

  assertEquals(nullableOne, result)
}

fun testStatement_inInOperator() {
  assertTrue((if (TRUE) 2 else throw AssertionError()) in 1..4)
}

fun testStatement_inRangeOperator() {
  assertTrue(ONE in (if (TRUE) 1 else throw AssertionError())..4)
}

private fun testStatement_asQualifierInPropertyAccess() {
  class ClassWithField(var field: Int)

  val obj = ClassWithField(0)

  //  This is equivalent to obj.field = 4
  if (TRUE) {
      obj
    } else {
      throw AssertionError()
    }
    .field = 4

  assertEquals(4, obj.field)
}

private fun testStatement_asQualifierOfMethodCall() {
  class ClassWithMethod(var field: Int) {
    fun inc() {
      field++
    }
  }

  val obj = ClassWithMethod(0)

  //  This is equivalent to obj.inc()
  if (TRUE) {
      obj
    } else {
      throw AssertionError()
    }
    .inc()

  assertEquals(1, obj.field)
}

private fun testStatement_inInitializerOfVariableDeclaration() {
  val b =
    if (TRUE) {
      3
    } else {
      throw AssertionError()
    }

  assertEquals(3, b)
}

var witnessField: Int = 1

// Observes the value of witnessField before it gets overwritten by the statements in
// fieldInitializedWithStatement
val observerPreField = witnessField

val fieldInitializedWithStatement =
  if (TRUE) {
    // Mutate witnessField
    witnessField = 2
    // This is the value of the intializer.
    3
  } else {
    throw AssertionError()
  }

// Observes the value of witnessField after it gets overwritten by the statements in
// filedIntializedWithStatement
val observerPostField = witnessField

private fun testStatement_inInstancePropertyIntialization() {
  assertEquals(3, fieldInitializedWithStatement)
  assertEquals(2, witnessField)
  assertEquals(1, observerPreField)
  assertEquals(2, observerPostField)

  class ClassForFieldInitialization {
    var witnessField: Int = 1

    // Observes the value of witnessField before it gets overwritten by the statements in
    // fieldInitializedWithStatement
    val observerPreField = witnessField
    val fieldInitializedWithStatement =
      if (TRUE) {
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

  val o = ClassForFieldInitialization()
  assertEquals(3, o.fieldInitializedWithStatement)
  assertEquals(2, o.witnessField)
  assertEquals(1, o.observerPreField)
  assertEquals(2, o.observerPostField)
}

private fun testTryCatch_asExpression() {
  val a =
    try {
      throw IllegalStateException()
    } catch (e: IllegalStateException) {
      4
    }
  assertEquals(4, a)

  val b =
    try {
      1
    } catch (e: RuntimeException) {
      2
    }
  assertEquals(1, b)

  val c =
    try {
      1
    } finally {
      2
    }
  assertEquals(1, c)

  val d: Unit = try {} finally {}
  assertEquals(Unit, d)

  var num = 0
  val e: Int =
    try {
      num++
    } finally {
      num++
    }
  assertEquals(0, e)
  assertEquals(2, num)
}

private fun testThrow_asExpression() {
  try {
    if (throw IllegalStateException()) {
      fail()
    } else {
      fail()
    }
  } catch (e: IllegalStateException) {
    // Expected.
  }
}

fun testBreak_asExpression() {
  var a = 0
  while (a < 5) {
    a++
    if (break) throw AssertionError()
    throw AssertionError()
  }
  assertEquals(1, a)

  a = 0
  loop@ while (a < 5) {
    a++
    if (break@loop) throw AssertionError()
    throw AssertionError()
  }
  assertEquals(1, a)
}

fun testContinue_asExpression() {
  var a = 0
  while (a < 5) {
    a++
    if (continue) throw AssertionError()
    throw AssertionError()
  }
  assertEquals(5, a)

  a = 0
  loop@ while (a < 5) {
    a++
    if (continue@loop) throw AssertionError()
    throw AssertionError()
  }
  assertEquals(5, a)
}

fun testIf_asExpression() {
  var a = 0
  val l: Long = 3
  val i: Int = 4

  var r = if (a < 5) l else i
  assertTrue(3L == r)

  r = if (a >= 5) l else i
  assertTrue(4 == r)
}

open class Super(val i: Int)

class Sub : Super(if (TRUE) 3 else throw AssertionError())

class OtherSub : Super {
  constructor() : super(if (TRUE) 4 else throw AssertionError())
  // This code does not make sense, yet it is accepted by kotlinc and generates a class file
  // that does not pass the bytecode verifier.
  // constructor(i: Int) : super(return)
}

private fun testStatement_asParameterToSuperCall() {
  assertEquals(3, Sub().i)
  assertEquals(4, OtherSub().i)
}

private fun methodWithDefault(i: Int = if (TRUE) 1 else throw AssertionError()): Int {
  return i
}

var someValue = 1

private fun methodWithComplicatedDefault(
  str: String =
    when (someValue % 2) {
      0 -> "even"
      1 -> "odd"
      else -> "impossible"
    }
) = str

private fun testStatement_asDefaultParameterValue() {
  assertEquals(1, methodWithDefault())
  assertEquals(2, methodWithDefault(2))
  assertEquals("odd", methodWithComplicatedDefault())
  someValue++
  assertEquals("even", methodWithComplicatedDefault())
  assertEquals("passthrough", methodWithComplicatedDefault("passthrough"))
}

fun testStatement_asArrayExpression() {
  var array = IntArray(3)
  (if (TRUE) array else throw AssertionError())[1] = 5
  assertEquals(5, array[1])
}

fun testStatement_asArrayIndex() {
  var array = IntArray(3)
  array[(if (TRUE) 1 else throw AssertionError())] = 5
  assertEquals(5, array[1])
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

fun testAnonymousInitializer() {
  assertEquals(1, ClassWithInitializer().field)
}

var enumInitOrder = ""

enum class EnumWithStatementInInitializer(val first: String, val second: String) {
  FOO(
    "FOO"
      .let {
        if (it.length > 3) throw AssertionError()
        enumInitOrder += it
        it
      },
    "FOO2"
      .run {
        enumInitOrder += "-$this"
        this
      },
  ),
  BAR(
    "BAR"
      .run {
        enumInitOrder += "-$this"
        this
      },
    if (enumInitOrder.isNotEmpty()) {
      val v = "BAR2"
      enumInitOrder += "-$v"
      v
    } else "unexpectedValue",
  ),
  BAZ(
    "BAZ"
      .run {
        enumInitOrder += "-$this"
        this
      },
    "BAZ2",
  ) {
    override fun third(): String = "fromBAZ"
  };

  open fun third(): String = "default"
}

fun testStatement_asEnumEntryInitializer() {
  assertEquals("FOO", EnumWithStatementInInitializer.FOO.first)
  assertEquals("FOO2", EnumWithStatementInInitializer.FOO.second)
  assertEquals("default", EnumWithStatementInInitializer.FOO.third())
  assertEquals("BAR", EnumWithStatementInInitializer.BAR.first)
  assertEquals("BAR2", EnumWithStatementInInitializer.BAR.second)
  assertEquals("default", EnumWithStatementInInitializer.BAR.third())
  assertEquals("BAZ", EnumWithStatementInInitializer.BAZ.first)
  assertEquals("BAZ2", EnumWithStatementInInitializer.BAZ.second)
  assertEquals("fromBAZ", EnumWithStatementInInitializer.BAZ.third())

  assertEquals("FOO-FOO2-BAR-BAR2-BAZ", enumInitOrder)
}
