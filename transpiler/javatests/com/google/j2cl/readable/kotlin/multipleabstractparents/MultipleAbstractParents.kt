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

object MultipleAbstractParents {
  interface List<T> {
    fun getFoo(t: T?): String?
  }

  interface Collection<T> {
    fun getFoo(t: T?): String?
  }

  abstract class AbstractListCollection<T> : List<T>, Collection<T>

  abstract class AbstractCollection<T> {
    fun getFoo(t: T?): String? {
      return "AbstractCollection"
    }
  }

  abstract class AbstractList<T> : AbstractCollection<T>(), List<T>

  abstract class AbstractList2<T> : List<T>

  class ArrayList<T> : AbstractList<T>()

  interface IStringList : List<String?> {
    override fun getFoo(string: String?): String?
  }

  abstract class AbstractStringList : AbstractList<String?>(), IStringList

  abstract class AbstractStringList2 : AbstractList2<String?>(), IStringList

  class SubAbstractStringList2 : AbstractStringList2() {
    override fun getFoo(t: String?): String? {
      return null
    }
  }

  abstract class AbstractStringList3 : AbstractList2<String?>()

  open class StringList : AbstractStringList()

  class StringListChild : StringList()

  fun main(args: Array<String>) {
    val a = ArrayList<String>().getFoo(null)
    val b = StringList().getFoo(null)
    val c = (StringList() as IStringList).getFoo(null)
  }
}
