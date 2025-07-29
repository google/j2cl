/*
 * Copyright 2023 Google Inc.
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
package hasnosideeffects

import com.google.j2cl.integration.testing.Asserts.assertEquals
import javaemul.internal.annotations.HasNoSideEffects

fun main(vararg unused: String) {
  testSideEffectInQualifier()
}

private class ClassWithMethodWithoutSideEffects {
  var unusedField = 0

  @HasNoSideEffects
  internal fun methodWithoutSideEffects(): ClassWithMethodWithoutSideEffects {
    return this
  }

  internal fun staticMethod() {}
}

private var sideEffects = 0

private val INSTANCE = ClassWithMethodWithoutSideEffects()

private fun getInstanceWithSideEffect(): ClassWithMethodWithoutSideEffects {
  sideEffects++
  return INSTANCE
}

private fun testSideEffectInQualifier() {
  assertEquals(0, sideEffects)
  getInstanceWithSideEffect().methodWithoutSideEffects()
  assertEquals(1, sideEffects)
  getInstanceWithSideEffect().methodWithoutSideEffects().staticMethod()
  assertEquals(2, sideEffects)
  getInstanceWithSideEffect().unusedField++
  assertEquals(3, sideEffects)
}
