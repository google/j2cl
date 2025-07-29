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
package unusedmultiexpressionreturn

/**
 * Unused multi expression return values.
 *
 * If we do not strip the return in this scenario then JSComp will flag the code as suspicious.
 */
private var field = 100

fun main(vararg unused: String) {
  var variable = 100
  val arrayVariable = intArrayOf(100)

  // If these are normalized to returning a value that is ignored then JSComp will reject.
  variable++
  variable--
  arrayVariable[0]++
  arrayVariable[0]--
  field++
  field--
}
