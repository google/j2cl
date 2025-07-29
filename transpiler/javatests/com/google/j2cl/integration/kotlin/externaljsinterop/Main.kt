/*
 * Copyright 2023 Google Inc.
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
package externaljsinterop

import com.google.j2cl.integration.testing.Asserts.assertEquals

fun main(vararg unused: String) {
  testJsOverlay()
}

fun testJsOverlay() {
  assertEquals(arrayOf("a", "b", "c"), JsOverlays.SOME_STRS)
  assertEquals("static overlay: abc", JsOverlays.overlayStaticFun("abc"))

  val o = JsOverlays()
  assertEquals("overlay: xyz", o.overlayFun("xyz"))
}
