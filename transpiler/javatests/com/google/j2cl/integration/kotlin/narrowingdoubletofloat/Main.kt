/*
 * Copyright 2023 Google Inc.
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
package narrowingdoubletofloat

import com.google.j2cl.integration.testing.Asserts.assertTrue

fun main(vararg unused: String) {
  testCoercions()
}

private fun testCoercions() {
  val dd = 2415919103.7 // dd < Double.MAX_VALUE;
  val md = 1.7976931348623157E308 // Double.MAX_VALUE;

  assertTrue(dd.toFloat().toDouble() == dd) // we don't honor float-double precision differences
  assertTrue(md.toFloat().toDouble() == md) // we don't honor float-double precision differences
}
