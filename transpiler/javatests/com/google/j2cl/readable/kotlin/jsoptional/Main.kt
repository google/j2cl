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
package jsoptional

import jsinterop.annotations.JsConstructor
import jsinterop.annotations.JsFunction
import jsinterop.annotations.JsMethod
import jsinterop.annotations.JsOptional
import jsinterop.annotations.JsType
import jsoptional.Main.Function

class Main @JsConstructor constructor(@JsOptional a: String?) {
  @JsMethod fun method1(i1: Int?, @JsOptional d: Double?, @JsOptional i: Int?) {}

  @JsMethod fun method2(s1: String?, @JsOptional d: Double?, vararg i: Boolean?) {}

  companion object {
    @JvmStatic @JsMethod fun staticMethod1(i1: Int?, @JsOptional d: Double?, @JsOptional i: Int?) {}

    @JvmStatic
    @JsMethod
    fun staticMethod2(s1: String?, @JsOptional d: Double?, vararg i: Boolean?) {}
  }

  @JsFunction
  fun interface Function {
    fun f1(@JsOptional s: String?, vararg args: Any?): Any?
  }

  val f = Function { s, _ -> s }

  class AFunction : Function {
    override fun f1(@JsOptional i: String?, vararg args: Any?): Any? {
      return args[0]
    }
  }

  fun testFunction(f: Function?) {}

  @JsMethod fun testOptionalFunction(@JsOptional f: Function?) {}

  @JsType
  interface I<T> {
    fun m(t: T, @JsOptional o: Any?)

    fun n(t: T, @JsOptional o: Any?, vararg rest: Any?)
  }

  @JsType
  class TemplatedSubtype<T : String> : I<T> {
    override fun m(t: T, @JsOptional o: Any?) {}

    override fun n(t: T, @JsOptional o: Any?, vararg rest: Any?) {}
  }

  @JsType
  @SuppressWarnings("ClassCanBeStatic")
  inner class SpecializedSubtype constructor(@JsOptional a: Any?) : I<String> {
    override fun m(t: String, @JsOptional o: Any?) {}

    override fun n(t: String, @JsOptional o: Any?, vararg rest: Any?) {}
  }

  class NonJsTypeSubtype : I<String> {
    override fun m(t: String, @JsOptional o: Any?) {}

    override fun n(t: String, @JsOptional o: Any?, vararg rest: Any?) {}
  }
}
