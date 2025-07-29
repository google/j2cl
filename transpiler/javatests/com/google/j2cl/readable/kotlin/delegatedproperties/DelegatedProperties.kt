/*
 * Copyright 2023 Google Inc.
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
package delegatedproperties

import kotlin.reflect.KProperty

var topLevelDelegatedProperty by PropertyDelegate()

class WithDelegatedProperty {
  var delegatedProperty: String by PropertyDelegate()

  var withProvideDelegate: String by DelegateProvider()

  var aProperty: String = "foo"

  // Property references implement the delegation api through inline functions. We should see in the
  // generated code a direct call to the get() and set() functions of the property reference.
  var delegatedToAnotherProperty: String by ::aProperty

  fun testLocalDelegatedVariable() {
    var localDelegatedVariableProperty: String by PropertyDelegate()

    val foo = localDelegatedVariableProperty
    localDelegatedVariableProperty = "Foo"
  }
}

var WithDelegatedProperty.delegatedExtensionProperty by PropertyDelegate()

private class PropertyDelegate {
  operator fun getValue(thisRef: Any?, property: KProperty<*>): String {
    return ""
  }

  operator fun setValue(thisRef: Any?, property: KProperty<*>, s: String) {}
}

private class DelegateProvider {
  operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): PropertyDelegate =
    PropertyDelegate()
}
