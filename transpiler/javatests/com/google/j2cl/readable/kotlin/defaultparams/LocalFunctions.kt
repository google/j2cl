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
package defaultparams

import jsinterop.annotations.JsConstructor
import jsinterop.annotations.JsPackage
import jsinterop.annotations.JsType

// Enum entry arguments are not within any function when the default parameters are lowered.
enum class LocalFunInEnumEntryArgument(val value: Int) {
  LOCAL_FUN(
    run {
      fun localFun(a: Int = 1) = a
      localFun()
    }
  ),
  NESTED_LOCAL_FUN(
    run {
      fun localFun(a: Int = 1): Int {
        fun nestedLocalFun(b: Int = 2) = b
        return a + nestedLocalFun()
      }
      localFun()
    }
  ),
  LOCAL_FUN_IN_LAMBDA(
    evaluate {
      fun localFun(a: Int = 4) = a
      localFun()
    }
  ),
  LOCAL_FUN_IN_ANONYMOUS_OBJECT(
    object {
        // A member of a local class, which is called through a stub unlike local functions.
        fun compute(a: Int = 5): Int {
          fun localFun(b: Int = a) = b
          return localFun()
        }
      }
      .compute()
  ),
}

fun localClassWithDefaultParameters(): Int {
  // Members of local classes are called through a stub unlike local functions.
  class LocalClass(val a: Int) {
    fun memberFun(b: Int = 1): Int {
      fun localFun(c: Int = b) = a + c
      return localFun()
    }
  }
  return LocalClass(10).memberFun()
}

fun evaluate(f: () -> Int) = f()

@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
open class NativeObject {
  open external fun toLocaleString(): String
}

class NativeObjectSubclass @JsConstructor constructor() : NativeObject() {
  // Overrides of external functions don't have default argument stubs.
  override fun toLocaleString(): String {
    fun localFun(s: String = "defaulted") = s
    return localFun()
  }
}
