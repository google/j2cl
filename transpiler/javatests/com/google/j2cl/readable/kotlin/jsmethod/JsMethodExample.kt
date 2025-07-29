/*
 * Copyright 2017 Google Inc.
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
package jsmethod

import jsinterop.annotations.JsMethod
import jsinterop.annotations.JsType

class JsMethodExample {

  // Regression readable for b/70040143.
  // The following declaration indirectly triggers the code in MethodDescriptor that fails a
  // precondition check when building the constructor of a raw type.
  // To reproduce the failing state the method needs:
  //    1. be a @JsMethod
  //    2. return a type variable that is bounded by a generic class that has a constructor.
  // This would better be handled in a unit test.
  @JsMethod external fun <T : ArrayList<String>> testMethod(): T

  abstract class Base<T> {
    @JsMethod open internal fun m(t: T) {}
  }

  interface I {
    @JsMethod(name = "mString") fun m(s: String)
  }

  // Regression test for b/124227197
  class Sub internal constructor() : Base<String>(), I {
    // This should not be a JsMethod.
    override fun m(s: String) {}
  }

  @JsType
  class SubJsType internal constructor() : Base<String>() {
    // This should not be a JsMethod.
    override fun m(s: String) {}

    companion object {
      // Regression test for b/313803375
      // This ensures that the proxy function creates on the enclosing type is not promoted to
      // public and part of the JsInterop contract.
      @JvmStatic internal fun n() {}
    }
  }

  @JsType
  class SubGenericsJsType<T> internal constructor() : Base<T>() {
    override fun m(s: T) {}
  }

  @JsType
  class SubJsTypeWithRenamedJsMethod internal constructor() : Base<String>() {
    // This should not be a JsMethod.
    @JsMethod(name = "renamedM") override fun m(s: String) {}
  }

  interface InterfaceWithMethod {
    fun m()

    fun n()
  }

  interface InterfaceExposingJsMethods : InterfaceWithMethod {
    @JsMethod override fun m() {}

    @JsMethod override fun n()
  }

  open class SuperClassWithFinalMethod internal constructor() {
    fun n() {}
  }

  // This class inherits implementations from supertypes and needs to exposes both the Java
  // contracts and js contracts.
  class ExposesOverrideableJsMethod internal constructor() :
    SuperClassWithFinalMethod(), InterfaceExposingJsMethods {}
}
