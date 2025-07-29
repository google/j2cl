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
package compiletimeconstant

interface CompileTimeConstantInterface {
  companion object {
    const val CONSTANT = 1
    val OBJ: Any? = null
    val OBJ2 = markClinit()
  }
}

fun markClinit(): Any? {
  ranClinit = true
  return null
}

val dummy: Any? = markClinit()

// compile time constants
const val CONSTANT = 10
const val CONSTANT_PLUS_ONE = CONSTANT + 1
const val CONSTANT_COMPOSED = CONSTANT_PLUS_ONE + CONSTANT
const val CONSTANT_STRING = "constant"
const val CONSTANT_COMPOSED_STRING = "constant" + CONSTANT_STRING
val OBJ: Any? = null

// non-compile time constants
var nonConstant = 20
