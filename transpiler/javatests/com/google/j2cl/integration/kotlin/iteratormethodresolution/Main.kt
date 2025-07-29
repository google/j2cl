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
package iteratormethodresolution

import com.google.j2cl.integration.testing.Asserts.assertTrue

/** Test for iterator method resolution */
interface MyList : Iterable<String>

private open class Base : MyList {

  private val content = arrayOf("1", "2", "3")

  override fun iterator(): Iterator<String> {
    return object : Iterator<String> {
      var current = 0

      override fun hasNext(): Boolean {
        return current < content.size
      }

      override fun next(): String {
        val last = current
        current++
        return content[last]
      }
    }
  }
}

private class Concrete : Base() // does not have an implementation of iterator()

fun main(vararg unused: String) {
  var count = 1
  for (string in Concrete()) {
    assertTrue(string == count.toString())
    count++
  }

  // Refer to the type by interface
  val myList = Concrete()
  count = 1
  for (string in myList) {
    assertTrue(string == count.toString())
    count++
  }
}
