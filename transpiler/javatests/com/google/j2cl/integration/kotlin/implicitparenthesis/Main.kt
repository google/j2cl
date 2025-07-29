/*
 * Copyright 2022 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package implicitparenthesis

import com.google.j2cl.integration.testing.Asserts.assertTrue

/** Inserting unboxing method calls will be mangled if implicit parenthesis are not inserted. */
fun main(vararg unused: String) {
  examples()
  nonExamples()
}

private fun examples() {
  var primitiveInt: Int
  var boxedInt: Int?

  // Ternary
  primitiveInt = 5
  boxedInt = 10
  primitiveInt = if (primitiveInt == 5) 15 else 30
  assertTrue(primitiveInt == 15)

  // Binary operator, compound assignment
  primitiveInt = 5
  boxedInt = 10
  boxedInt += primitiveInt
  primitiveInt = boxedInt
  assertTrue(primitiveInt == 15)

  // Binary operator, assignment
  primitiveInt = 5
  boxedInt = 10
  boxedInt = primitiveInt
  primitiveInt = boxedInt
  assertTrue(primitiveInt == 5)

  // Postfix operator
  primitiveInt = 5
  boxedInt = 10
  primitiveInt = boxedInt++
  assertTrue(primitiveInt == 10)

  // Prefix operator
  primitiveInt = 5
  boxedInt = 10
  primitiveInt = ++boxedInt
  assertTrue(primitiveInt == 11)
}

private fun nonExamples() {
  var primitiveInt: Int
  var boxedInt: Int?
  var boxedBoolean: Boolean?

  // Binary operator, arithmetic

  // The arithmetic input and output must already be in primitive form so no boxing needs to be
  // inserted before the assignment.
  primitiveInt = 5
  boxedInt = 10
  primitiveInt = boxedInt - primitiveInt
  assertTrue(primitiveInt == 5)

  // Binary operator, comparison.

  // Boolean is devirtualized so this can't really fail, but it's here for completeness.
  primitiveInt = 5
  boxedInt = 10
  boxedBoolean = true
  boxedBoolean = boxedInt == primitiveInt
  assertTrue(boxedBoolean == false)
}
