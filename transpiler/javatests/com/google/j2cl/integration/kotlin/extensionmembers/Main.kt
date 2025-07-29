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

import com.google.j2cl.integration.testing.Asserts.assertEquals
import com.google.j2cl.integration.testing.Asserts.assertTrue

fun main(vararg unused: String) {
  testBasicExtensionMembers()
  testLocalExtensionMembers()
}

class ReceiverClass(var instanceProperty: Int) {
  val conflictingProperty = 1

  fun instanceMethod(): Int = 1

  companion object // Ensure it will be called "Companion"
}

fun ReceiverClass.topLevelExtensionFunction(valueParameter: Int): Int {
  return instanceProperty + instanceMethod() + valueParameter
}

var ReceiverClass.topLevelExtensionProperty: Int
  get() = instanceProperty + instanceMethod()
  set(value) {
    instanceProperty = value
  }

fun ReceiverClass?.topLevelExtensionFunctionAcceptingNull(valueParameter: Int): Int {
  if (this == null) {
    return -1
  }
  return valueParameter
}

fun ReceiverClass.Companion.extensionOnCompanion(): Int = 100

class TemplatedReceiverClass<T>(var instanceProperty: T)

fun TemplatedReceiverClass<String>.topLevelExtensionFunction(): String {
  return "TemplatedReceiverClass: " + instanceProperty
}

fun TemplatedReceiverClass<Int>.topLevelExtensionFunction(): Int {
  return 10 + instanceProperty
}

class ExtensionEnclosingClass(var aProperty: Int) {
  val conflictingProperty = 2

  private fun ReceiverClass.instanceExtensionFunction(valueParameter: Int): Int {
    // test if implicit this qualifier is correctly resolved. In the event of a name conflict
    // between the members of a dispatch receiver and an extension receiver, the extension receiver
    // takes precedence. To refer to the member of the dispatch receiver, you can use the qualified
    // this syntax.
    return aProperty +
      valueParameter +
      topLevelExtensionProperty +
      instanceProperty +
      conflictingProperty +
      this@ExtensionEnclosingClass.conflictingProperty
  }

  private var ReceiverClass.instanceExtensionProperty: Int
    get() = aProperty + topLevelExtensionProperty + instanceProperty
    set(value) {
      aProperty = value
      instanceProperty = value
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
}

private fun testBasicExtensionMembers() {
  assertTrue(ReceiverClass(5).instanceProperty == 5)
  assertTrue(ReceiverClass(5).topLevelExtensionFunction(2) == 8)
  assertTrue(ReceiverClass(5).topLevelExtensionProperty == 6)

  assertTrue(ReceiverClass.Companion.extensionOnCompanion() == 100)
  assertTrue(ReceiverClass.extensionOnCompanion() == 100)

  var receiver = ReceiverClass(5)
  receiver.topLevelExtensionProperty = 10
  assertTrue(receiver.instanceProperty == 10)
  assertTrue(receiver.topLevelExtensionProperty == 11)

  assertTrue(receiver.topLevelExtensionFunctionAcceptingNull(2) == 2)
  val nullReceiver: ReceiverClass? = null
  assertTrue(nullReceiver.topLevelExtensionFunctionAcceptingNull(2) == -1)

  receiver = ReceiverClass(2)
  assertTrue(ExtensionEnclosingClass(1).callExtensionFunction(receiver, 3) == 12)
  assertTrue(ExtensionEnclosingClass(1).getExtensionProperty(receiver) == 6)

  val extensionEnclosingInstance = ExtensionEnclosingClass(1)
  assertTrue(extensionEnclosingInstance.aProperty == 1)
  extensionEnclosingInstance.setExtensionProperty(receiver, 3)
  assertTrue(extensionEnclosingInstance.aProperty == 3)
  assertTrue(receiver.instanceProperty == 3)
  assertTrue(receiver.topLevelExtensionProperty == 4)

  assertEquals(
    "TemplatedReceiverClass: foo",
    TemplatedReceiverClass("foo").topLevelExtensionFunction(),
  )
  assertEquals(11, TemplatedReceiverClass(1).topLevelExtensionFunction())
}

fun ReceiverClass.anotherTopLevelExtensionFunction(valueParameter: Int): Int {
  fun ReceiverClass.extensionFunctionInsideExtensionFunction(valueParameter2: Int): Int {
    return this@anotherTopLevelExtensionFunction.instanceProperty +
      instanceProperty +
      valueParameter +
      valueParameter2
  }
  return ReceiverClass(10).extensionFunctionInsideExtensionFunction(3)
}

private fun testLocalExtensionMembers() {
  var receiver = ReceiverClass(5)
  assertTrue(
    ExtensionEnclosingClass(1).InnerClass(2).callExtensionFunctionFromInnerClass(receiver, 3) == 11
  )
  assertTrue(ReceiverClass(5).anotherTopLevelExtensionFunction(1) == 19)
}

@JvmInline
value class InlineReceiverClass(val instanceProperty: Int) {
  fun instanceMethod(): Int = 1
}

fun InlineReceiverClass.topLevelExtensionFunction(valueParameter: Int): Int {
  return instanceProperty + instanceMethod() + valueParameter
}

fun InlineReceiverClass?.topLevelExtensionFunction(valueParameter: Int): Int {
  return (this?.instanceProperty ?: 0) + valueParameter
}

private fun testInlineClassExtensionMembers() {
  assertEquals(6, InlineReceiverClass(2).topLevelExtensionFunction(3))
  var nullableInlineClass: InlineReceiverClass? = null
  assertEquals(3, nullableInlineClass.topLevelExtensionFunction(3))
  nullableInlineClass = InlineReceiverClass(2)
  assertEquals(5, nullableInlineClass.topLevelExtensionFunction(3))
}
