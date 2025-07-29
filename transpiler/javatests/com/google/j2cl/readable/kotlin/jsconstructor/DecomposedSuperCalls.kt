/*
 * Copyright 2023 Google Inc.
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
package jsconstructor

import jsinterop.annotations.JsConstructor

open class BaseNonJsConstructor(obj: Any)

open class BaseJsConstructorType @JsConstructor constructor(obj: Any)

// Scope functions will be extracted out into multiple statements preceding the super()/this() call.
class ScopeFunctionsInSuperCall @JsConstructor constructor(defaults: List<String>) :
  BaseJsConstructorType(
    defaults +
      mutableListOf<String>().apply {
        add("a")
        add("b")
      }
  ) {
  constructor() :
    this(
      mutableListOf<String>().apply {
        add("c")
        add("d")
      }
    )
}

class ScopeFunctionsInSuperCallToNonJsConstructor
@JsConstructor
constructor(defaults: List<String>) :
  BaseNonJsConstructor(
    defaults +
      mutableListOf<String>().apply {
        add("a")
        add("b")
      }
  ) {
  constructor() :
    this(
      mutableListOf<String>().apply {
        add("c")
        add("d")
      }
    )
}

// Kotlin frontend always rewrite elvis operators into multiple statements.
class ElvisOperatorInSuperCall @JsConstructor constructor(obj: Any?) :
  BaseJsConstructorType(obj ?: Any()) {
  constructor(a: Any?, b: Any) : this(a ?: b)
}

class ElvisOperatorInSuperCallToNonJsConsturctor @JsConstructor constructor(obj: Any?) :
  BaseNonJsConstructor(obj ?: Any()) {
  constructor(a: Any?, b: Any) : this(a ?: b)
}
