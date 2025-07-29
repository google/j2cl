/*
 * Copyright 2017 Google Inc.
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
package renamejsmethodsinnativejstype

import com.google.j2cl.integration.testing.Asserts.assertTrue
import jsinterop.annotations.JsMethod

fun main(vararg args: String) {
  testJsMethodInJava()
  testJsMethodInJs()
}

fun testJsMethodInJava() {
  val foo = Foo()
  assertTrue(foo.sum() == 42)
  foo.x = 50
  foo.y = 5
  assertTrue(foo.sum() == 55)
}

fun testJsMethodInJs() {
  val foo = Foo()
  assertTrue(callSum(foo) == 42)
}

@JsMethod external fun callSum(foo: Foo): Int
