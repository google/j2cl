/*
 * Copyright 2025 Google Inc.
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
package inlinefunction

inline fun <reified T> reifiedInlineFunInAnotherLib(obj: Any?): T? {
  if (obj is T) return obj
  return null
}

@Suppress("just here to ensure that annotations properly deserialize")
inline fun inlineFun(value: Any?, message: String): Any {
  if (value == null) return message
  return value
}

inline fun inlineFunWithNestedInlineFun(value: Any?): Any {
  return inlineFun(value, "default")
}

inline fun funWithLambda(value: Any?, defaultValue: () -> Any): Any {
  if (value == null) return defaultValue()
  return value
}

inline fun funWithNestedInlineFunAndLambda(value: Any?): Any {
  return funWithLambda(value) { "someDefault" }
}

fun getDefaultValue(defaultValue: () -> Any): Any = defaultValue()

inline fun funWithNestedInlineFunAndLambdaWithInlineFunctionCall(
  value: Any?,
  crossinline defaultValue: () -> Any,
): Any {
  return funWithLambda(value) { getDefaultValue { defaultValue() } }
}

inline fun callsFunThatIsNeverDirectlyReferenced(): Any = dontCallMeDirectly()

inline fun callsAccessorThatIsNeverDirectlyReferenced(): Any = dontAccessMeDirectly

@PublishedApi internal fun dontCallMeDirectly(): Any = 1

@PublishedApi
internal val dontAccessMeDirectly: Any
  get() = 1

class WithInlineFunction() {
  inline fun callTopLevelNotReferencedFun() = notReferenceOutsideOfThisFile()
}

inline fun notReferenceOutsideOfThisFile(): Any = 1

class HolderInAnotherLibrary(val i: Int) {
  @Suppress("just here to ensure that annotations properly deserialize")
  inline fun getPlusOne() = i + 1
}

inline fun callTopLevelInlineFunctionOnlyReferencedButDeclaredInAnotherFile() =
  topLevelInlineFunctionOnlyReferencedInAnotherFile()

class A {
  val x: Int = 0

  inner class B {
    // capture A.this
    inline fun foo() = x
  }
}

private val privateTopLevelProperty: Int = 1

private fun privateTopLevelFunction() = 2

// Leaks the private top-level members above to the friend modules of this library through
// synthetic accessors.
internal inline fun internalInlineFunctionUsingPrivateTopLevelMembers() =
  privateTopLevelProperty + privateTopLevelFunction()

class ClassWithPrivateMembers {
  private val privateProperty: Int = 3

  private fun privateFunction() = 4

  // Leaks the private members above to the friend modules of this library through synthetic
  // accessors.
  internal inline fun internalInlineFunctionUsingPrivateMembers() =
    privateProperty + privateFunction()
}

inline fun inlineFunctionUsingJavaMembers(i: Int): String {
  // Constructor and overloaded instance methods.
  val builder = java.lang.StringBuilder().append("x").append(i)
  // Static field.
  java.lang.System.out.println(builder)
  // Static method and Java getter used as a property.
  return java.lang.Integer.toHexString(i) + java.util.Date(0L).time
}

open class ParentInAnotherLibrary {
  var parentProperty: Int = 5

  fun parentFunction() = 6
}

class ChildInAnotherLibrary : ParentInAnotherLibrary()

// Calls the members of the parent class through an instance of the child class, which inherits
// them without overriding them.
inline fun inlineFunctionUsingMembersInheritedByChild(child: ChildInAnotherLibrary): Int {
  child.parentProperty = 7
  return child.parentProperty + child.parentFunction()
}

class ChildOfJavaClass : java.util.Random()

// Calls a method of a Java class through an instance of a Kotlin child class, which inherits it
// without overriding it.
inline fun inlineFunctionUsingJavaMethodInheritedByChild(child: ChildOfJavaClass) = child.nextInt()
