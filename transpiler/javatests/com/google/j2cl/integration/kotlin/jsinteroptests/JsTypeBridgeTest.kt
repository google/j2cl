/*
 * Copyright 2017 Google Inc.
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
package jsinteroptests

import com.google.j2cl.integration.testing.Asserts.assertEquals
import jsinterop.annotations.JsType

object JsTypeBridgeTest {
  fun testAll() {
    testBridges()
  }

  @JsType
  private interface JsListInterface {
    fun add(o: Any?)
  }

  private interface Collection {
    fun add(o: Any?)
  }

  private open class CollectionBase : Collection {
    internal var x: Any? = null

    override fun add(o: Any?) {
      x = o.toString() + "CollectionBase"
    }
  }

  private interface List : Collection, JsListInterface {
    override fun add(o: Any?)
  }

  private class FooImpl : CollectionBase(), Collection {
    override fun add(o: Any?) {
      super.add(o)
      x = x.toString() + "FooImpl"
    }
  }

  private class ListImpl : CollectionBase(), List {
    override fun add(o: Any?) {
      x = o.toString() + "ListImpl"
    }
  }

  private fun testBridges() {
    // Exports .add().
    val listWithExport = ListImpl()
    // Does not export .add().
    val listNoExport = FooImpl()

    // Use a loose type reference to force polymorphic dispatch.
    val collectionWithExport: Collection = listWithExport
    // Calls through a bridge method.
    collectionWithExport.add("Loose")
    assertEquals("LooseListImpl", listWithExport.x)

    // Use a loose type reference to force polymorphic dispatch.
    val collectionNoExport: Collection = listNoExport
    collectionNoExport.add("Loose")
    assertEquals("LooseCollectionBaseFooImpl", listNoExport.x)

    // Calls directly.
    listNoExport.add("Tight")
    assertEquals("TightCollectionBaseFooImpl", listNoExport.x)

    listWithExport.add("Tight")
    assertEquals("TightListImpl", listWithExport.x)
  }
}
