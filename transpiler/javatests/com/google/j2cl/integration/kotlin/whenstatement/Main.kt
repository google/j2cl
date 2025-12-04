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
package whenstatement

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.assertThrows
import com.google.j2cl.integration.testing.Asserts.fail

/** Test unary operations. */
fun main(vararg unused: String) {
  testSwitchValues()
  testWhenWithoutElse()
  testWhenWithOperatorIs()
  testWhenNoExpression()
  testWhenVariableDeclaration()
  testEmptyWhen()
  testOnlyElse()
  testWhenWithOneBranch()
  testWhenExpressionWithStatements()
}

private fun testSwitchValues() {
  assertEquals(1, getStringValue("zero"))
  assertEquals(1, getStringValue("one"))
  assertEquals(2, getStringValue("two"))
  assertEquals(3, getStringValue("three"))
  assertEquals(3, getStringValue(null))
  assertEquals(4, getStringValueWithNull(null))

  assertEquals(1, getCharValue('0'))
  assertEquals(1, getCharValue('1'))
  assertEquals(2, getCharValue('2'))
  assertEquals(3, getCharValue('3'))
  assertEquals(3, getCharValue(null))

  assertEquals(1, getIntValue(0))
  assertEquals(1, getIntValue(1))
  assertEquals(2, getIntValue(2))
  assertEquals(3, getIntValue(3))
  assertEquals(3, getIntValue(null))

  assertEquals(1, getBooleanValue(true))
  assertEquals(2, getBooleanValue(false))
  assertEquals(2, getBooleanValue(null))

  assertEquals(1, getEnumValueExhaustive(Numbers.ONE))
  assertEquals(2, getEnumValueExhaustive(Numbers.TWO))
  assertEquals(3, getEnumValueExhaustive(Numbers.THREE))

  assertEquals(1, getEnumValue(Numbers.ONE))
  assertEquals(2, getEnumValue(Numbers.TWO))
  assertEquals(3, getEnumValue(Numbers.THREE))
  assertEquals(4, getEnumValue(null))
  assertEquals(4, getEnumValueWithNull(null))

  assertEquals(10, testBreakStatementInSwitch(Numbers.ONE))
  assertEquals(2, testBreakStatementInSwitch(Numbers.TWO))
  assertEquals(0, testBreakStatementInSwitch(Numbers.THREE))
}

private fun getStringValue(stringValue: String?): Int {
  return when (stringValue) {
    "zero",
    "one" -> 1
    "two" -> 2
    else -> 3
  }
}

private fun getStringValueWithNull(stringValue: String?): Int {
  when (stringValue) {
    "zero",
    "one" -> return 1
    "two" -> return 2
    null -> return 4
    else -> return 3
  }
}

private fun getCharValue(charValue: Char?): Int {
  return when (charValue) {
    '0',
    '1' -> 1
    '2' -> 2
    else -> 3
  }
}

private fun getIntValue(intValue: Int?): Int {
  when (intValue) {
    0,
    1 -> return 1
    2 -> return 2
    else -> return 3
  }
}

private fun getBooleanValue(booleanValue: Boolean?): Int {
  return when (booleanValue) {
    true -> 1
    else -> 2
  }
}

enum class Numbers {
  ONE,
  TWO,
  THREE,
}

private fun getEnumValueExhaustive(numberValue: Numbers): Int {
  when (numberValue) {
    Numbers.ONE -> return 1
    Numbers.TWO -> return 2
    Numbers.THREE -> return 3
  }
}

private fun getEnumValue(numberValue: Numbers?): Int {
  when (numberValue) {
    Numbers.ONE -> return 1
    Numbers.TWO -> return 2
    Numbers.THREE -> return 3
    else -> return 4
  }
}

private fun getEnumValueWithNull(numberValue: Numbers?): Int {
  when (numberValue) {
    Numbers.ONE -> return 1
    Numbers.TWO -> return 2
    Numbers.THREE -> return 3
    null -> return 4
  }
}

private fun testBreakStatementInSwitch(numberValue: Numbers): Int {
  var value = 0
  for (i in 0..2) {
    when (numberValue) {
      Numbers.ONE -> value += 5
      Numbers.TWO -> {
        value += 2
        break
      }
      Numbers.THREE -> break
    }
    if (i == 1) {
      break
    }
  }
  return value
}

private fun testWhenWithoutElse() {
  assertEquals(1, getStringValueWithoutElse("minusOne"))
  assertEquals(1, getStringValueWithoutElse("zero"))
  assertEquals(1, getStringValueWithoutElse("one"))
  assertEquals(2, getStringValueWithoutElse("two"))
  assertEquals(3, getStringValueWithoutElse("three"))
  assertEquals(1, num)
}

private fun getStringValueWithoutElse(stringValue: String): Int {
  when (stringValue) {
    "minusOne",
    "zero",
    "one" -> return 1
    "two" -> return 2
  }
  return 3
}

// when can be used as an expression, the else branch is mandatory, unless the compiler can prove
// that all possible cases are covered with branch conditions
val num =
  when (getEnumNumber()) {
    Numbers.ONE -> 1
    Numbers.TWO -> 2
    Numbers.THREE -> 3
  }

private fun getEnumNumber(): Numbers {
  return Numbers.ONE
}

private fun testWhenWithOperatorIs() {
  assertEquals(1, getValueWithOperatorIs("1"))
  assertEquals(2, getValueWithOperatorIs(1))
}

fun getValueWithOperatorIs(a: Any): Int {
  return when (a) {
    is String -> 1
    else -> 2
  }
}

private fun testWhenNoExpression() {
  var a = 3
  when {
    a < 3 -> fail()
    a == 3 -> a++
  }
  assertEquals(4, a)
}

private fun testWhenVariableDeclaration() {
  var a = 2
  when (val b = a + 2) {
    4 -> a = b + 1
    else -> a = b - 1
  }
  assertEquals(5, a)
}

private fun testEmptyWhen() {
  var a = 2
  when {}
  assertEquals(2, a)

  // TODO(b/465183958): Uncomment once this bug is fixed.
  // when (a++) {}
  // assertEquals(3, a)
}

private fun testOnlyElse() {
  var a = 2
  when {
    else -> a = 3
  }
  assertEquals(3, a)

  a =
    when {
      else -> 4
    }
  assertEquals(4, a)
}

private fun testWhenExpressionWithStatements() {
  sideEffectCtr = 0

  assertEquals(10, whenExpressionWithStatements(1))
  assertEquals(1, sideEffectCtr)

  assertThrows(Exception::class.java) { whenExpressionWithStatements(2) }
  assertEquals(1, sideEffectCtr)

  assertEquals(20, whenExpressionWithStatements(3))
  assertEquals(1, sideEffectCtr)
}

private fun whenExpressionWithStatements(i: Int): Int =
  when (i) {
    1 -> {
      sideEffect()
      10
    }
    2 -> throw Exception()
    else -> 20
  }

private var sideEffectCtr = 0

private fun sideEffect() {
  sideEffectCtr++
}

enum class OneEntry {
  THE_ONE
}

var oneEntry = OneEntry.THE_ONE

private fun testWhenWithOneBranch() {
  var a = 2
  when {
    a < 3 -> a = 3
  }
  assertEquals(3, a)

  a =
    when (oneEntry) {
      OneEntry.THE_ONE -> 4
    }
  assertEquals(4, a)
}
