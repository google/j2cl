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
package finallyblock

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.assertTrue
import com.google.j2cl.integration.testing.Asserts.fail

fun main(vararg unused: String) {
  testFinally_basic()
  testFinally_basic_inConstructor()
  testFinally_basic_inFieldInitializer()
  testFinally_evaluationOrder_returnExpression()
  testFinally_fallThrough()
  testFinally_returnCancelledByOuterBreak()
  testFinally_returnCancelledByOuterContinue()
  testFinally_returnSupersededByOuterReturn()
  testFinally_returnCancelledByBreak_inLambda()
  testFinally_exceptionSupersededByOuterException()
  testFinally_exceptionCancelledByOuterBreak()
  testFinally_exceptionCancelledByOuterBreak_inConstructor()
  testFinally_exceptionCancelledByOuterBreak_inInstanceInitializerBlock()
  testFinally_exceptionCancelledByOuterBreak_inStaticInitializerBlock()
}

private fun testFinally_basic() {
  assertEquals(10, basicFinallyMethod())
  assertEquals(42, value)
}

private var value = 0

private fun basicFinallyMethod(): Int {
  return try {
    10
  } finally {
    value = 42
  }
}

private fun testFinally_basic_inConstructor() {
  value = 0
  class Local {
    init {
      OUT@ do {
        try {
          break@OUT
        } finally {
          value = 54
        }
      } while (false)
    }
  }
  assertTrue(Local() is Local)
  assertEquals(54, value)
}

private fun testFinally_basic_inFieldInitializer() {
  val steps = fieldWithTryFinally.get()
  assertEquals(arrayOf<Any>("inTry", "inFinally"), steps.toTypedArray())
}

private var fieldWithTryFinally =
  Supplier<List<String>> {
    val steps: MutableList<String> = java.util.ArrayList()
    try {
      steps.add("inTry")
      return@Supplier steps
    } finally {
      steps.add("inFinally")
    }
    steps.add("afterTry")
    return@Supplier null!!
  }

private fun testFinally_evaluationOrder_returnExpression() {
  val steps: MutableList<String> = java.util.ArrayList()
  testFinally_evaluationOrder_returnExpression(steps)
  assertEquals(
    arrayOf<Any>("innerTry", "evaluatedReturnExpression", "innerFinally"),
    steps.toTypedArray(),
  )
}

private fun testFinally_evaluationOrder_returnExpression(steps: MutableList<String>): Boolean {
  try {
    steps.add("innerTry")
    return steps.add("evaluatedReturnExpression")
  } finally {
    steps.add("innerFinally")
  }
  steps.add("atEnd")
  return false
}

private fun testFinally_fallThrough() {
  val steps: MutableList<String> = ArrayList()
  testFinally_fallThrough(steps)
  assertEquals(
    arrayOf("innerTry", "innerFinally", "endOuterTry", "outerFinally", "end"),
    steps.toTypedArray(),
  )
}

private fun testFinally_fallThrough(steps: MutableList<String>) {
  try {
    try {
      steps.add("innerTry")
    } finally {
      steps.add("innerFinally")
    }
    steps.add("endOuterTry")
  } finally {
    steps.add("outerFinally")
  }
  steps.add("end")
}

private fun testFinally_returnCancelledByOuterBreak() {
  val steps: MutableList<String> = ArrayList()
  assertEquals("end", testFinally_returnCancelledByOuterBreak(steps))
  assertEquals(
    arrayOf("innerTry", "innerFinally", "endOuterTry", "outerFinally", "end"),
    steps.toTypedArray(),
  )
}

private fun testFinally_returnCancelledByOuterBreak(steps: MutableList<String>): String {
  try {
    OUT@ do {
      try {
        steps.add("innerTry")
        return "innerTry"
      } finally {
        steps.add("innerFinally")
        break@OUT
      }
    } while (false)
    steps.add("endOuterTry")
  } finally {
    steps.add("outerFinally")
  }
  steps.add("end")
  return "end"
}

private fun testFinally_returnCancelledByOuterContinue() {
  val steps: MutableList<String> = ArrayList()
  assertEquals("topOfLoop", testFinally_returnCancelledByOuterContinue(steps))
  assertEquals(arrayOf("innerTry", "innerFinally", "outerFinally"), steps.toTypedArray())
}

private fun testFinally_returnCancelledByOuterContinue(steps: MutableList<String>): String {
  for (i in 0..1) {
    if (i == 1) {
      return "topOfLoop"
    }
    try {
      try {
        steps.add("innerTry")
        return "innerTry"
      } finally {
        steps.add("innerFinally")
      }
      steps.add("endOuterTry")
    } finally {
      steps.add("outerFinally")
      continue
    }
    steps.add("bottomOfLoop")
  }
  steps.add("end")
  return "end"
}

private fun testFinally_returnSupersededByOuterReturn() {
  val steps: MutableList<String> = ArrayList()
  assertEquals("outerFinally", testFinally_returnSupersededByOuterReturn(steps))
  assertEquals(arrayOf("innerTry", "innerFinally", "outerFinally"), steps.toTypedArray())
}

private fun testFinally_returnSupersededByOuterReturn(steps: MutableList<String>): String {
  try {
    try {
      steps.add("innerTry")
      return "innerTry"
    } finally {
      steps.add("innerFinally")
    }
    steps.add("endOuterTry")
  } finally {
    steps.add("outerFinally")
    return "outerFinally"
  }
  steps.add("end")
  return "end"
}

private fun testFinally_returnCancelledByBreak_inLambda() {
  val steps: MutableList<String> = java.util.ArrayList()
  val functionWithFinally = Supplier {
    OUT@ do {
      try {
        steps.add("inTry")
        return@Supplier "inTry"
      } finally {
        steps.add("inFinally")
        break@OUT
      }
    } while (false)

    steps.add("afterTry")
    "atEnd"
  }
  steps.clear()
  assertEquals("atEnd", functionWithFinally.get())
  assertEquals(arrayOf<Any>("inTry", "inFinally", "afterTry"), steps.toTypedArray())
}

private fun testFinally_exceptionSupersededByOuterException() {
  val steps: MutableList<String> = ArrayList()
  try {
    testFinally_exceptionSupersededByOuterException(steps)
    fail()
  } catch (e: RuntimeException) {
    assertEquals(arrayOf("innerTry", "innerFinally", "outerFinally"), steps.toTypedArray())
    assertEquals("outerFinally", e.message)
    // The original exceptions gets dropped and is not added to the suppressed ones.
    assertEquals(0, e.suppressed.size)
  }
}

private fun testFinally_exceptionSupersededByOuterException(steps: MutableList<String>): String {
  try {
    try {
      steps.add("innerTry")
      throw RuntimeException("innerTry")
    } finally {
      steps.add("innerFinally")
    }
    steps.add("endOuterTry")
  } finally {
    steps.add("outerFinally")
    throw RuntimeException("outerFinally")
  }
  steps.add("end")
  return "end"
}

private fun testFinally_exceptionCancelledByOuterBreak() {
  val steps: MutableList<String> = ArrayList()
  assertEquals("end", testFinally_exceptionCancelledByOuterBreak(steps))
  assertEquals(arrayOf("innerTry", "innerFinally", "outerFinally", "end"), steps.toTypedArray())
}

private fun testFinally_exceptionCancelledByOuterBreak(steps: MutableList<String>): String {
  OUT@ do {
    try {
      try {
        steps.add("innerTry")
        throw RuntimeException("innerTry")
      } finally {
        steps.add("innerFinally")
      }
      steps.add("endOuterTry")
    } finally {
      steps.add("outerFinally")
      break@OUT
    }
  } while (false)
  steps.add("end")
  return "end"
}

private fun testFinally_exceptionCancelledByOuterBreak_inConstructor() {
  val steps: MutableList<String> = java.util.ArrayList()

  class Local {
    init {
      OUT@ do {
        try {
          try {
            steps.add("beforeThrow")
            throw RuntimeException()
          } finally {
            steps.add("innerFinally")
          }
          steps.add("normalFlowAfterTry")
        } finally {
          steps.add("outerFinally")
          break@OUT
        }
      } while (false)
      steps.add("atNormalExit")
    }
  }

  assertTrue(Local() is Local)
  assertEquals(
    arrayOf<Any>("beforeThrow", "innerFinally", "outerFinally", "atNormalExit"),
    steps.toTypedArray(),
  )
}

private fun testFinally_exceptionCancelledByOuterBreak_inInstanceInitializerBlock() {
  val steps: MutableList<String> = ArrayList()

  class Local {
    init {
      OUT@ do {
        try {
          try {
            steps.add("beforeThrow")
            throw RuntimeException()
          } finally {
            steps.add("innerFinally")
          }
          steps.add("normalFlowAfterFinally")
        } finally {
          steps.add("outerFinally")
          break@OUT
        }
      } while (false)
      steps.add("atNormalExit")
    }
  }

  val l = Local()
  assertEquals(
    arrayOf<Any>("beforeThrow", "innerFinally", "outerFinally", "atNormalExit"),
    steps.toTypedArray(),
  )
}

private fun testFinally_exceptionCancelledByOuterBreak_inStaticInitializerBlock() {
  assertEquals(
    arrayOf<Any>("beforeThrow", "innerFinally", "outerFinally", "atNormalExit"),
    FinallyInStaticInitializer.steps.toTypedArray(),
  )
}

private class FinallyInStaticInitializer {
  val i = 0 // dummy field to prevent kotlinc complaining that the classes that just declared a

  // companion should be themselves object.
  companion object {
    var steps: MutableList<String> = java.util.ArrayList()

    init {
      OUT@ do {
        try {
          try {
            steps.add("beforeThrow")
            throw RuntimeException()
          } finally {
            steps.add("innerFinally")
          }
          // TODO(b/b/271358556): Delete this suppression
          @Suppress("UNINITIALIZED_VARIABLE") steps.add("normalFlowAfterFinally")
        } finally {
          steps.add("outerFinally")
          break@OUT
        }
      } while (false)
      steps.add("atNormalExit")
    }
  }
}

internal fun interface Supplier<T> {
  fun get(): T
}
