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
package extensionmembers

fun <T : ReceiverClass> main(parametrizedReceiver: T) {
  ReceiverClass(5).topLevelExtensionFunction(2)
  ReceiverClass(5).topLevelExtensionProperty

  parametrizedReceiver.topLevelExtensionFunction(2)
  parametrizedReceiver.topLevelExtensionProperty

  InlineReceiverClass(2).topLevelExtensionFunction(3)
  var foo: InlineReceiverClass? = null
  foo.topLevelExtensionFunction(3)
}

class ReceiverClass(var instanceProperty: Int) {
  fun instanceMethod(): Int = 1
}

fun ReceiverClass.topLevelExtensionFunction(valueParameter: Int): Int {
  return instanceProperty + instanceMethod() + valueParameter
}

var ReceiverClass.topLevelExtensionProperty: Int
  get() = instanceProperty + instanceMethod()
  set(value) {
    instanceProperty = value
  }

class TemplatedReceiverClass<T>(var instanceProperty: T)

fun TemplatedReceiverClass<String>.topLevelExtensionFunction(): String {
  return "TemplatedReceiverClass<String>: " + instanceProperty
}

fun TemplatedReceiverClass<Int>.topLevelExtensionFunction(): Int {
  return 10 + instanceProperty
}

class ExtensionEnclosingClass(var aProperty: Int) {
  fun ReceiverClass.instanceExtensionFunction(valueParameter: Int): Int {
    return aProperty + valueParameter + topLevelExtensionProperty + instanceProperty
  }

  var ReceiverClass.instanceExtensionProperty: Int
    get() = aProperty + topLevelExtensionProperty + instanceProperty
    set(value) {
      aProperty = value
      instanceProperty = value
    }

  inner class InnerClass(val innerClassProperty: Int) {
    fun ReceiverClass.extensionFunctionFromInnerClass(valueParameter: Int): Int {
      return this@ExtensionEnclosingClass.aProperty +
        this@InnerClass.innerClassProperty +
        this.instanceProperty +
        valueParameter
    }

    fun callExtensionFunctionFromInnerClass(
      receiver: ReceiverClass,
      extensionFunctionParam: Int,
    ): Int {
      return receiver.extensionFunctionFromInnerClass(extensionFunctionParam)
    }
  }

  fun callExtensionFunction(receiver: ReceiverClass, extensionFunctionParam: Int): Int {
    return receiver.instanceExtensionFunction(extensionFunctionParam)
  }

  fun setExtensionProperty(receiver: ReceiverClass, extensionPropertyValue: Int) {
    receiver.instanceExtensionProperty = extensionPropertyValue
  }

  fun getExtensionProperty(receiver: ReceiverClass): Int {
    return receiver.instanceExtensionProperty
  }
}

@JvmInline
value class InlineReceiverClass(val instanceProperty: Int) {
  fun instanceMethod(): Int = 1
}

fun InlineReceiverClass.topLevelExtensionFunction(valueParameter: Int): Int {
  return instanceProperty + instanceMethod() + valueParameter
}

fun InlineReceiverClass?.topLevelExtensionFunction(valueParameter: Int): Int {
  return (this?.instanceProperty ?: 0) + (this?.instanceMethod() ?: 0) + valueParameter
}
