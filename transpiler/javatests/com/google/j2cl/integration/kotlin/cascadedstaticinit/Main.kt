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
package cascadedstaticinit

import com.google.j2cl.integration.testing.Asserts.assertTrue

/** Test cascaded static initializers run only once. */
fun main(vararg unused: String) {
  // Bar's initializer defines it to be 5;
  assertTrue(getBar() == 5)
  // Foo's initializer defines it to be bar * 5;
  assertTrue(getFoo() == 25)

  // If you change Bar then Foo won't update because it's initializer only runs once.
  setBar(10)
  assertTrue(getBar() == 10)
  assertTrue(getFoo() == 25)
  assertTrue(getFoo() == 25)
  assertTrue(getFoo() == 25)
}

fun getBar(): Int {
  // Since this is a static function in Main it will attempt to run Main's $clinit and the getter
  // for the top level static bar property in ValueHolder will attempt to run it's own $clinit.
  return bar
}

fun getFoo(): Int {
  // Since this is a static function in Main it will attempt to run Main's $clinit and the getter
  // for the top level static foo property in ValueHolder will attempt to run it's own $clinit.
  return foo
}

fun setBar(value: Int) {
  // Since this is a static function in Main it will attempt to run Main's $clinit and the setter
  // for the top level static bar property in ValueHolder will attempt to run it's own $clinit.
  bar = value
}
