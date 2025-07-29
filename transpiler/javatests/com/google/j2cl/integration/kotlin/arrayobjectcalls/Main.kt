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
package arrayobjectcalls

import com.google.j2cl.integration.testing.Asserts.assertTrue

@SuppressWarnings("ArrayEquals", "ArrayHashCode")
fun main(vararg unused: String) {
  val nulls = arrayOfNulls<Any>(1)
  val other = arrayOfNulls<Any>(1)
  assertTrue(nulls == nulls)
  assertTrue(nulls != other)
  assertTrue(nulls.hashCode() == nulls.hashCode())
  assertTrue(nulls.hashCode() != other.hashCode())
  assertTrue(nulls.javaClass == other.javaClass)
}
