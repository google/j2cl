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
package castsdisabled

import com.google.j2cl.integration.testing.Asserts.fail

/**
 * Simple cast test for measuring the size cost of having casts disabled, and verifying that casts
 * checks are not performed.
 */
fun main(vararg unused: String) {
  class Foo
  val obj: Any = Foo()

  // This is fine.
  val f = obj as Foo
  try {
    // This should fail! But we've turned off cast checking in the BUILD file, so the exception
    // will not be thrown.
    val exception = obj as RuntimeException
  } catch (e: ClassCastException) {
    fail()
  }
  if (obj is Foo) {
    val foo = obj as Foo
  }
}
