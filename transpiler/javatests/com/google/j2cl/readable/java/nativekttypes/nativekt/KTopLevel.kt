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
package nativekttypes.nativekt

import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

open class KTopLevel<O>(o: O) {
  open class KNested<N>(n: N) {
    @JvmField var instanceField: N? = null

    open fun instanceMethod(n: N): N {
      return n
    }

    open fun renamedMethod(): Int {
      return 0
    }

    companion object {
      @JvmStatic var staticField: Any? = null

      @JvmStatic
      fun <S> staticMethod(s: S): S {
        return s
      }
    }
  }

  open inner class KInner<I>(i: I)

  @JvmField var instanceField: O? = null

  @JvmField var renamedField = 0

  @JvmField internal var nonPublicField = 0

  open fun renamedMethod(): Int {
    return 0
  }

  open val methodAsProperty: Int
    get() = 0

  open val uppercaseprefixMethodAsProperty: Int
    get() = 0

  open val uppercasemethodasproperty: Int
    get() = 0

  open val nonGetMethodAsProperty: Int
    get() = 0

  open val renamedMethodAsProperty: Int
    get() = 0

  open val getRenamedMethodAsProperty: Int
    get() = 0

  @JvmField var isRenamedField = false

  open val isMethodAsProperty: Boolean
    get() = false

  open val getstartingmethodAsProperty: Int
    get() = 0

  open fun instanceMethod(o: O): O {
    return o
  }

  internal fun nonPublicMethod() {}

  companion object {
    @JvmStatic var staticField: Any? = null

    @JvmStatic
    fun <S> staticMethod(s: S): S {
      return s
    }
  }
}
