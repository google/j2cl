/*
 * Copyright 2026 Google Inc.
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
package subnativeinterface

import jsinterop.annotations.JsMethod
import jsinterop.annotations.JsProperty
import jsinterop.annotations.JsType

@JsType(isNative = true, namespace = "test.foo")
interface MyNativeInterface {
  fun getValue(): Int

  @JsMethod(name = "renamed") fun mightBeRenamed(): Int

  @JsProperty fun getProp(): Int

  @JsProperty fun setProp(value: Int)
}

@JsType(isNative = true, namespace = "test.foo")
fun interface MyNativeGenericInterface<T> {
  fun echo(t: T): T
}

open class SubNativeInterface : MyNativeInterface {
  private var prop: Int = 0

  override fun getValue(): Int = 1

  override fun mightBeRenamed(): Int = 2

  override fun getProp(): Int = prop

  override fun setProp(value: Int) {
    prop = value
  }

  companion object {
    fun test(): String {
      val nativeInterface: MyNativeInterface = SubNativeInterface()
      nativeInterface.setProp(nativeInterface.getValue() + nativeInterface.mightBeRenamed())
      val lambda = MyNativeGenericInterface<String> { s -> s + nativeInterface.getProp() }
      return lambda.echo("")
    }
  }
}

class SubSubNativeInterface : SubNativeInterface() {
  override fun getValue(): Int = 3
}

abstract class AbstractSubNativeInterface : MyNativeInterface {
  override fun mightBeRenamed(): Int = 4
}

class StringEcho : MyNativeGenericInterface<String> {
  override fun echo(t: String): String = t
}

abstract class AbstractStringEcho : MyNativeGenericInterface<String>

class ConcreteStringEcho : AbstractStringEcho() {
  override fun echo(t: String): String = t
}

class GenericEcho<T> : MyNativeGenericInterface<T> {
  override fun echo(t: T): T = t
}

open class NonJsBase {
  open fun getValue(): Int = 5

  fun mightBeRenamed(): Int = 6

  open fun getProp(): Int = 7

  open fun setProp(value: Int) {}
}

open class AccidentalSubNativeInterface : NonJsBase(), MyNativeInterface

// Repro b/588567907
class FinalAccidentalSubNativeInterface : NonJsBase(), MyNativeInterface

open class NonJsStringEcho {
  fun echo(t: String): String = t
}

class AccidentalStringEcho : NonJsStringEcho(), MyNativeGenericInterface<String>

open class NonJsGenericEcho<T> {
  fun echo(t: T): T = t
}

interface StringEchoInterface {
  fun echo(t: String): String
}

// Repro b/588568727
open class AccidentalGenericEchoWithOtherInterface :
  NonJsGenericEcho<String>(), MyNativeGenericInterface<String>, StringEchoInterface

interface KotlinSubNativeInterface : MyNativeInterface {
  override fun getValue(): Int = 8
}

class KotlinSubNativeInterfaceImpl : KotlinSubNativeInterface {
  override fun mightBeRenamed(): Int = 9

  override fun getProp(): Int = 10

  override fun setProp(value: Int) {}
}
