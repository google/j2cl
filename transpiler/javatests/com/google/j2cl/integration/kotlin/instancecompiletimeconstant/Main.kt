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
package instancecompiletimeconstant

import com.google.j2cl.integration.testing.Asserts.assertTrue

fun main(vararg unused: String) {
  val child = Child()
}

private open class Parent {
  val parentCompileTimeConstantString: String? = "987"
  val parentCompileTimeConstantByte: Byte = 123
  val parentCompileTimeConstantShort: Short = 123
  val parentCompileTimeConstantInt = 123
  val parentCompileTimeConstantLong = 123L
  var parentRegularString: String? = "987"
  var parentRegularInt = 123

  init {
    checkFieldsInitialized()
  }

  /**
   * If this check() is called then the control flow was main -> Parent.constructor() ->
   * Parent.checkFieldsInitialized() and as a result all instance fields should already be
   * initialized.
   */
  open fun checkFieldsInitialized() {
    assertTrue(parentCompileTimeConstantString != null)
    assertTrue(parentCompileTimeConstantByte.toInt() != 0)
    assertTrue(parentCompileTimeConstantShort.toInt() != 0)
    assertTrue(parentCompileTimeConstantInt != 0)
    assertTrue(parentCompileTimeConstantLong != 0L)

    // Parent's non compile time constant fields *have* been initialized because of the time at
    // which check was called!
    assertTrue(parentRegularString != null)
    assertTrue(parentRegularInt != 0)
  }
}

private class Child : Parent() {
  val childCompileTimeConstantFloat = 123f
  val childCompileTimeConstantDouble = 123.0
  val childCompileTimeConstantChar = '{' // == ASCII dec 123
  val childCompileTimeConstantBoolean = true
  var childRegularString: String? = "987"
  var childRegularInt = 123

  /**
   * If this check() is called then the control flow was main -> Child.constructor()->
   * Parent.constructor() -> Child.checkFieldsInitialized() and as a result all of Parent instance
   * fields should have been initialized. Since instance fields cannot be a compile time constant in
   * Kotlin, none of the fields of Child should be initialized yet.
   */
  override fun checkFieldsInitialized() {
    super.checkFieldsInitialized()
    assertTrue(childCompileTimeConstantFloat == 0f)
    assertTrue(childCompileTimeConstantDouble == 0.0)
    assertTrue(childCompileTimeConstantChar == '\u0000')
    assertTrue(childCompileTimeConstantBoolean == false)
    assertTrue(childRegularString == null)
    assertTrue(childRegularInt == 0)
  }
}
