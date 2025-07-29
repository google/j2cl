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
package multipleabstractparents

import com.google.j2cl.integration.testing.Asserts.assertEquals

interface List<T> {
  fun getFoo(t: T): String?
}

abstract class AbstractCollection<T> {
  fun getFoo(t: T): String? {
    return "AbstractCollection"
  }
}

abstract class AbstractList<T> : AbstractCollection<T>(), List<T>

class ArrayList<T> : AbstractList<T>()

interface IStringList : List<String?> {
  override fun getFoo(string: String?): String?
}

abstract class AbstractStringList : AbstractList<String?>(), IStringList

class StringList : AbstractStringList()

fun main(vararg unused: String) {
  assertEquals("AbstractCollection", ArrayList<String?>().getFoo(null))
  assertEquals("AbstractCollection", StringList().getFoo(null))
  assertEquals("AbstractCollection", (StringList() as IStringList).getFoo(null))
}
