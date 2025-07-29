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
package morebridgemethods

import com.google.j2cl.integration.testing.Asserts.assertTrue

object DefaultMethodsMain {

  val COLLECTION_ADD = 1
  val LIST_ADD = 2

  internal interface Collection<T> {
    fun add(elem: T): Int {
      assertTrue(this is Collection<*>)
      return COLLECTION_ADD
    }
  }

  internal interface List<T> : Collection<T> {
    override fun add(elem: T): Int {
      assertTrue(this is List<*>)
      return LIST_ADD
    }
  }

  internal open class SomeOtherCollection<T> : Collection<T>

  internal open class SomeOtherList<T> : SomeOtherCollection<T>(), List<T>

  // Should inherit List.add  even though the interface is not directly declared.
  internal class YetAnotherList<T> : SomeOtherList<T>(), Collection<T>

  fun test() {
    assertTrue(YetAnotherList<Any?>().add(null) == LIST_ADD)
  }
}
