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

enum class Numbers {
  ONE,
  TWO,
  THREE,
}

class WhenStatement {
  private fun whenExpressionWithString(stringValue: String): Int {
    return when (stringValue) {
      "zero",
      "one" -> 1
      "two" -> 2
      else -> 3
    }
  }

  private fun whenStatementWithString(stringValue: String): Int {
    val value: Int
    when (stringValue) {
      "minus_one",
      "zero",
      "one" -> value = 1
      "two" -> value = 2
      else -> value = 3
    }

    return value
  }

  private fun whenStatementWithStringWithNull(stringValue: String?): Int {
    val value: Int
    when (stringValue) {
      "minus_one",
      "zero",
      "one" -> value = 1
      "two" -> value = 2
      null -> value = 4
      else -> value = 3
    }

    return value
  }

  fun whenExpressionWithChar(charValue: Char): Int {
    return when (charValue) {
      '0',
      '1' -> 1
      '2' -> 2
      else -> 3
    }
  }

  fun whenStatementWithChar(charValue: Char): Int {
    val value: Int
    when (charValue) {
      '0',
      '1' -> value = 1
      '2' -> value = 2
      else -> value = 3
    }
    return value
  }

  fun whenStatementWithByte(byteValue: Byte): Int {
    when (byteValue) {
      0.toByte(),
      1.toByte() -> return 1
      2.toByte() -> return 2
      else -> return 3
    }
  }

  fun whenStatementWithShort(shortValue: Short): Int {
    when (shortValue) {
      0.toShort(),
      1.toShort() -> return 1
      2.toShort() -> return 2
      else -> return 3
    }
  }

  fun whenExpressionWithInt(intValue: Int): Int {
    return when (intValue) {
      0,
      1 -> 1
      2 -> 2
      else -> 3
    }
  }

  fun whenStatementWithInt(intValue: Int): Int {
    when (val foo = intValue * 2) {
      0,
      1 -> return 1
      2 -> return 2
      else -> return foo
    }
  }

  fun whenWithBooleanValue(): Int {
    when (true) {
      true -> return 1
      else -> return 2
    }
  }

  fun whenExpressionWithEnumValue(number: Numbers): Int {
    return when (number) {
      Numbers.ONE -> 1
      Numbers.TWO -> 2
      Numbers.THREE -> 3
    }
  }

  fun whenStatementWithEnumValue(number: Numbers?): Int {
    when (number) {
      Numbers.ONE -> return 1
      Numbers.TWO -> return 2
      Numbers.THREE -> return 3
      else -> return 4
    }
  }

  fun whenStatementWithInnerBreakStatement(numberValue: Numbers): Int {
    var value = 0
    for (i in 0..2) {
      when (numberValue) {
        Numbers.ONE -> value += 1
        Numbers.TWO -> {
          value += 2
          break
        }
        Numbers.THREE -> {
          for (j in 0..2) {
            break
          }
          break
        }
      }
    }
    return value
  }

  // when can be used as a statement with or without else branch
  fun whenWithoutElse(): Int {
    var number = 0
    when ("one") {
      "minusTwo",
      "minusOne",
      "zero",
      "one" -> number = 1
      "two" -> number = 2
    }
    return number
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

  fun getValueWithOperatorIs(): Int {
    when ("1") {
      is String -> return 1
      else -> return 2
    }
  }

  fun whenWithNoExpressions(): Int {
    val a = 3
    when {
      a < 3 -> return 1
      a == 3 -> return 2
    }
    return 3
  }

  fun whenWithVariableDeclaration(): Int {
    var a = 2
    a =
      when (val b = a + 2) {
        4 -> b + 1
        else -> b - 1
      }
    return a
  }

  fun whenExpressionWithStatements(i: Int): Int {
    return when (i) {
      1 -> {
        sideEffect()
        10
      }
      2 -> throw Exception()
      else -> 20
    }
  }

  private fun sideEffect() {}

  fun whenWithImplicitEqualsComparison(): Int {
    var o: Any? = Any()
    var a = Any()
    var v = 3
    return when (o) {
      a -> 1
      v -> 2
      else -> 3
    }
  }
}
