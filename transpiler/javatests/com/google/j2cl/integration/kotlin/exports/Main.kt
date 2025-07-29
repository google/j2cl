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
package exports

import com.google.j2cl.integration.testing.AssertsBase

fun main(vararg unused: String) {
  // Will fail if the library that provides Foo is not available.
  val foo: Any = Foo()
  AssertsBase.assertTrue(foo is Foo)

  // Will fail if the library that provides Bar is not available.
  val bar: Any = Bar()
  AssertsBase.assertTrue(bar is Bar)
}
