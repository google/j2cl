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
package overwrittentypevariables

// Reuse the ParameterizedType class name to ensure that unique to the FQN of the enclosing type,
// not just the relative class name.
//
// Redefine T in a different position from the parent.
interface ParameterizedType<F, T> : overwrittentypevariables.otherpkg.ParameterizedType<F, T> {
  override fun accept(v: F): T
}

// We use primitive specialization to Int here to force creation of a bridge method that needs to
// introspect on the transitive type variables.
class ParameterizedTypeImpl : ParameterizedType<String, Int> {
  override fun accept(v: String): Int = 1
}
