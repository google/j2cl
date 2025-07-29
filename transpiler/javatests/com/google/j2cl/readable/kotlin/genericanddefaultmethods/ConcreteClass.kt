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
package genericanddefaultmethods

interface InterfaceWithDefault {
  fun foo(value: String): Int {
    return 1
  }
}

open class GenericClass<T> {
  open fun foo(value: T): Int {
    return 2
  }
}

class ConcreteClass : GenericClass<String>(), InterfaceWithDefault {
  // In contrast to Java where the implementation in GenericClass is the one inherited,
  // in Kotlin it is ambiguous and needs to be declared explicitly.
  override fun foo(value: String): Int {
    return super<GenericClass<String>>.foo(value)
  }

  fun fooFromInterface(value: String): Int {
    return super<InterfaceWithDefault>.foo(value)
  }
}
