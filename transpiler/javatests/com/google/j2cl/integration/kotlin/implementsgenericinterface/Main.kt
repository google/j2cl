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
package implementsgenericinterface

import com.google.j2cl.integration.testing.Asserts.assertTrue

interface GenericInterface<T> {
  fun foo(t: T): T
}

class InterfaceImpl : GenericInterface<InterfaceImpl> {
  override fun foo(t: InterfaceImpl): InterfaceImpl {
    return t
  }
}

internal class InterfaceGenericImpl<T> : GenericInterface<T> {
  override fun foo(t: T): T {
    return t
  }
}

fun main(vararg unused: String) {
  val i = InterfaceImpl()
  val o = i.foo(i) as Any
  assertTrue(o === i)
  val gi: InterfaceGenericImpl<InterfaceImpl> = InterfaceGenericImpl<InterfaceImpl>()
  val oo = gi.foo(i) as Any
  assertTrue(oo === i)
}
