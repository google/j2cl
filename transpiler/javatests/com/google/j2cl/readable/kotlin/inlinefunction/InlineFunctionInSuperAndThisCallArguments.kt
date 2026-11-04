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

import jsinterop.annotations.JsConstructor

var sideEffect = 0

// When inlined in the arguments of a super() or this() call, the statements of the inlined body
// are emitted before that call.
inline fun computeWithSideEffect(x: Int): Int {
  val y = x + 1
  sideEffect = y
  return y * 2
}

inline fun computeLambdaWithSideEffect(f: () -> Int): Int {
  val r = f()
  sideEffect += r
  return r
}

open class ParentWithInlinedArgument(val a: Int)

class InlineFunctionInSuperCall : ParentWithInlinedArgument(computeWithSideEffect(1))

class InlineFunctionInSuperAndThisCalls : ParentWithInlinedArgument {
  constructor(x: Int) : super(computeLambdaWithSideEffect { x + 1 })

  constructor() : this(computeWithSideEffect(2))
}

open class JsParentWithInlinedArgument @JsConstructor constructor(val a: Int)

class JsInlineFunctionInSuperCall @JsConstructor constructor() :
  JsParentWithInlinedArgument(computeWithSideEffect(1))

class JsInlineFunctionInSuperAndThisCalls : JsParentWithInlinedArgument {
  @JsConstructor constructor(x: Int) : super(computeLambdaWithSideEffect { x + 1 })

  constructor() : this(computeWithSideEffect(2))
}

// The outer instance and the captured variables are read from the constructor parameters, since
// the fields holding them can't be accessed before super().
class OuterOfJsInnerClass(val outerValue: Int) {
  inner class JsInnerClass @JsConstructor constructor() :
    JsParentWithInlinedArgument(computeLambdaWithSideEffect { outerValue })
}

fun createJsLocalClass(capturedValue: Int): JsParentWithInlinedArgument {
  class JsLocalClass @JsConstructor constructor() :
    JsParentWithInlinedArgument(computeLambdaWithSideEffect { capturedValue })
  return JsLocalClass()
}
