/*
 * Copyright 2026 Google Inc.
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
package inlinefunction

import com.google.j2cl.integration.testing.Asserts.assertEquals
import jsinterop.annotations.JsConstructor

private var sideEffect = 0

// When inlined in the arguments of a super() or this() call, the statements of the inlined body
// run before that call.
private inline fun computeWithSideEffect(x: Int): Int {
  val y = x + 1
  sideEffect = y
  return y * 2
}

private inline fun computeLambdaWithSideEffect(f: () -> Int): Int {
  val r = f()
  sideEffect += r
  return r
}

private open class ParentWithInlinedArgument(val a: Int)

private class InlineFunctionInSuperAndThisCalls : ParentWithInlinedArgument {
  constructor(x: Int) : super(computeLambdaWithSideEffect { x + 1 })

  constructor() : this(computeWithSideEffect(2))
}

private open class JsParentWithInlinedArgument @JsConstructor constructor(val a: Int)

private class JsInlineFunctionInSuperAndThisCalls : JsParentWithInlinedArgument {
  @JsConstructor constructor(x: Int) : super(computeLambdaWithSideEffect { x + 1 })

  constructor() : this(computeWithSideEffect(2))
}

fun testInlineFunctionInSuperAndThisCallArguments() {
  sideEffect = 0
  // computeWithSideEffect(2) sets the side effect to 3 and returns 6, then
  // computeLambdaWithSideEffect { 6 + 1 } adds 7.
  assertEquals(7, InlineFunctionInSuperAndThisCalls().a)
  assertEquals(10, sideEffect)

  sideEffect = 0
  assertEquals(7, JsInlineFunctionInSuperAndThisCalls().a)
  assertEquals(10, sideEffect)
}
