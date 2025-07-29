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
package subnativejstype

import com.google.j2cl.integration.testing.Asserts.assertTrue

fun main(vararg args: String) {
  val sub = SubMyNativeType(10, 20)
  assertTrue(sub.executed)
  assertTrue(sub.x == 30)
  assertTrue(sub.y == 200)
  assertTrue(sub.f == 10)
  assertTrue(sub.foo(100) == 330)
}
