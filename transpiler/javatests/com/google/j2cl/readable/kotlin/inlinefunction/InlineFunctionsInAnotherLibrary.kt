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
