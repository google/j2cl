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
package localnamecollision

class LocalNameCollision(
  LocalNameCollision: Boolean,
  Asserts: Any,
  `$Asserts`: Any,
  l_Asserts: Int,
  A: Int,
) {
  fun testClassLocalVarCollision() {
    var LocalNameCollision = false
    val RuntimeException: Any? = null
    val Asserts = 1
    val `$Asserts` = 1
    val l_Asserts = 1
    val com_google_j2cl_readable_localnamecollision_Class = 1
    val com_google_j2cl_readable_localnamecollision_package1_A = 1
    val com_google_j2cl_readable_localnamecollision_package2_A = 1
    val A = 1
    LocalNameCollision =
      (RuntimeException is LocalNameCollision ||
        RuntimeException is RuntimeException ||
        RuntimeException is localnamecollision.package1.A ||
        RuntimeException is localnamecollision.package2.A ||
        RuntimeException is Class)

    assert(Asserts().n() == 5)
  }

  fun testClassParameterCollision(
    LocalNameCollision: Boolean,
    Asserts: Any,
    `$Asserts`: Any,
    l_Asserts: Int,
    A: Int,
  ): Boolean {
    return (LocalNameCollision &&
      Asserts is LocalNameCollision &&
      `$Asserts` is Asserts &&
      l_Asserts == A)
  }

  // test class and parameters collision in constructor.
  init {
    val result =
      (LocalNameCollision &&
        Asserts is LocalNameCollision &&
        `$Asserts` is Asserts &&
        l_Asserts == A)
  }
}

internal class A {
  companion object {
    var A: A? = null
    var B: B? = null

    fun test() {
      A = A
      B = B

      localnamecollision.A.A = localnamecollision.A.A
      localnamecollision.A.B = localnamecollision.A.B
    }
  }

  internal class B
}
