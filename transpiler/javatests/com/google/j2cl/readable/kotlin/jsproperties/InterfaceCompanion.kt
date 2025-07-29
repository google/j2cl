/*
 * Copyright 2024 Google Inc.
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
package com.google.j2cl.readable.kotlin.jsproperties

import jsinterop.annotations.JsProperty
import jsinterop.annotations.JsType

@JsType
interface JsTypeInterfaceWithCompanion {
  companion object {
    internal val a = 1
    const val b = 2
    var c = 3
    val d = 4
    val e
      get() = 5
  }
}

@JsType
interface JsTypeInterfaceWithCompanionJvmField {
  companion object {
    @JvmField val a = 1
  }
}

interface InterfaceWithCompanion {
  companion object {
    val a = 1
    const val b = 2
    var c = 3
    @JsProperty val d = 4
    val e
      get() = 5
  }
}
