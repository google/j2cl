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
package staticfields

import com.google.j2cl.integration.testing.Asserts.assertTrue

/** Test static field initializer, and static field reference in field declaration. */
fun main(vararg unused: String) {
  testStaticFields_visibilities()
  // Superclass companion members cannot be referenced through the subclass.
  // testStaticFields_superInterface()
  testStaticFields_internal()
}

var a = 2
private var b = a * 3

// Fields and methods that start with $ do not get a prefix. This is a static field and will be
// rewritten into getter/setter plus backing field. The backing field name will also start with
// $, but in this case since it is synthetic it will be appropriately prefixed.
var `$internal` = 3

private fun testStaticFields_visibilities() {
  assertTrue(a == 2)
  assertTrue(b == 6)
}

private fun testStaticFields_internal() {
  assertTrue(`$internal` == 3)
  `$internal`++
  assertTrue(`$internal` == 4)
}
