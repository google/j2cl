/*
 * Copyright 2026 Google Inc.
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
package localclassinfieldinitializer

class LocalClassInFieldInitializer {
  var r: (String) -> Unit = {
    class Local {
      var ss = it
    }
    Local()
  }

  var s =
    if (true) {
      class Local {}
      Local()
      1
    } else {
      class Local {}
      Local()
      2
    }

  val t = run {
    fun local() = 1
    local()
  }
}

interface Supplier {
  fun get(): Int
}

class CapturesInInitializers(ctorParam: Int) {
  val capturesCtorParam =
    object : Supplier {
      override fun get() = ctorParam
    }

  val capturesInitLocal: Supplier

  init {
    var local = ctorParam
    capturesInitLocal =
      object : Supplier {
        override fun get() = local++
      }
  }
}

// The initializers, including the anonymous class, are kept in an init block that J2CL runs from
// its `$init` method, called by both constructors.
class NoPrimaryConstructor {
  val supplier =
    object : Supplier {
      override fun get() = 1
    }

  constructor(i: Int)

  constructor(s: String)
}
