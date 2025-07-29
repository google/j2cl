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
package interfacedelegation

interface I {
  val message: String
  val propertyOverridenInDelegatingClass: String

  fun retrieveString(): String

  fun methodOverridenInDelegatingClass(): String

  fun nonOveriddenfunctionUsingOverridenProperty(): String

  fun defaultMethod(): String {
    return "DefaultMethod from interface"
  }

  fun defaultMethodOverridenInD(): String {
    return "DefaultMethod from interface"
  }
}

class Implementor() : I {
  val m = "Implemented by Implementor"
  override val message = m
  override val propertyOverridenInDelegatingClass = m

  override fun retrieveString(): String {
    return m
  }

  override fun methodOverridenInDelegatingClass(): String {
    return m
  }

  override fun nonOveriddenfunctionUsingOverridenProperty(): String {
    return propertyOverridenInDelegatingClass
  }
}

class DelegatingClass() : I by Implementor() {
  override val propertyOverridenInDelegatingClass = "DelegatingClass"

  override fun methodOverridenInDelegatingClass(): String {
    return "Overriden in DelegatingClass"
  }
  override fun defaultMethodOverridenInD(): String {
    return "DefaultMethod overriden in DelegatingClass"
  }
}
