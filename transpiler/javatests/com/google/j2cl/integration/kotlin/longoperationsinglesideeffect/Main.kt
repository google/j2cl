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
package longoperationsinglesideeffect

import com.google.j2cl.integration.testing.Asserts.assertTrue

fun main(vararg unused: String) {
  testIncrementDecrementSideEffects()
  testCompoundAssignmentSideEffects()
  testNestedIncrements()
}

object Holder {
  var longField = 0L
  var sideEffectCount = 0

  fun causeSideEffect(): Holder {
    sideEffectCount++
    return this
  }

  fun fluentAssert(expectedValue: Long, testValue: Long): Holder {
    assertTrue("expected $expectedValue but was $testValue", expectedValue == testValue)
    return this
  }
}

/**
 * Long emulation of increment and decrement operators forces multiple accesses of the subject field
 * but it's important that it be done in a way that doesn't invoke earlier parts of the qualifier
 * chain multiple times.
 */
fun testIncrementDecrementSideEffects() {
  Holder.longField = 0L
  Holder.sideEffectCount = 0

  assertTrue(Holder.sideEffectCount == 0)

  assertTrue(Holder.causeSideEffect().longField++ == 0L)
  assertTrue(Holder.longField == 1L)
  assertTrue(Holder.sideEffectCount == 1)

  assertTrue(Holder.causeSideEffect().longField-- == 1L)
  assertTrue(Holder.longField == 0L)
  assertTrue(Holder.sideEffectCount == 2)

  assertTrue(++Holder.causeSideEffect().longField == 1L)
  assertTrue(Holder.longField == 1L)
  assertTrue(Holder.sideEffectCount == 3)

  assertTrue(--Holder.causeSideEffect().longField == 0L)
  assertTrue(Holder.longField == 0L)
  assertTrue(Holder.sideEffectCount == 4)

  Holder.causeSideEffect().longField += 1L
  assertTrue(Holder.longField == 1L)
  assertTrue(Holder.sideEffectCount == 5)

  Holder.causeSideEffect().longField -= 1L
  assertTrue(Holder.longField == 0L)
  assertTrue(Holder.sideEffectCount == 6)
}

/**
 * Long emulation of increment and decrement operators forces multiple accesses of the subject field
 * but it's important that it be done in a way that doesn't invoke earlier parts of the qualifier
 * chain multiple times.
 */
fun testCompoundAssignmentSideEffects() {
  Holder.longField = 0L
  Holder.sideEffectCount = 0

  assertTrue(Holder.sideEffectCount == 0)

  Holder.causeSideEffect().longField += 1L
  assertTrue(Holder.longField == 1L)
  assertTrue(Holder.sideEffectCount == 1)

  Holder.causeSideEffect().longField -= 1L
  assertTrue(Holder.longField == 0L)
  assertTrue(Holder.sideEffectCount == 2)

  Holder.causeSideEffect().longField *= 1L
  assertTrue(Holder.longField == 0L)
  assertTrue(Holder.sideEffectCount == 3)

  Holder.causeSideEffect().longField /= 1L
  assertTrue(Holder.longField == 0L)
  assertTrue(Holder.sideEffectCount == 4)
}

/** Stresses the Long emulation increment rewriting. */
fun testNestedIncrements() {
  Holder.longField = 0L
  Holder.sideEffectCount = 0

  Holder.fluentAssert(
    2L,
    Holder.fluentAssert(1L, Holder.fluentAssert(0L, Holder.longField++).longField++).longField++,
  )

  Holder.longField = 0L
  Holder.fluentAssert(0L, Holder.longField++)
    .fluentAssert(1L, Holder.longField++)
    .fluentAssert(2L, Holder.longField++)

  Holder.longField = 0L
  Holder.fluentAssert(
      2L,
      Holder.fluentAssert(0L, Holder.longField++).fluentAssert(1L, Holder.longField++).longField++,
    )
    .fluentAssert(
      5L,
      Holder.fluentAssert(3L, Holder.longField++).fluentAssert(4L, Holder.longField++).longField++,
    )
}
