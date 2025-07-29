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
package bridgemethods

import jsinterop.annotations.JsMethod

class BridgeMethodDefault {
  internal interface I<T> {
    fun m(t: T)
  }

  internal interface II : I<String?> {
    override fun m(s: String?) {}
  }

  class A : II

  internal interface JJ : I<Any?> {
    @JsMethod override fun m(o: Any?) {}
  }

  open class B : JJ

  class C : B() {
    override fun m(o: Any?) {}
  }

  init {
    val jj: JJ = C()
    jj.m(Any())
    val i: I<Any?> = jj
    i.m(Any())
  }
}
