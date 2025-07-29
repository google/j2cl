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
package inlinefunction

import kotlin.experimental.ExperimentalTypeInference
import kotlin.jvm.JvmName

class ClassWithCompanion(v: Any) {
  inline fun inlineInstanceFun() = notReferencedOutsideOfThisFile()

  inline fun inlineInstanceFunCallingCompanionInlineFun() =
    ClassWithCompanion.anotherInlineCompanionFun()

  inline fun instanceInlineFunDirectlyInvoked() = yetYetAnotherNotReferencedOutsideOfThisFile()

  companion object {
    inline fun inlineCompanionFun() = ClassWithCompanion(notReferencedOutsideOfThisFile(Any()))

    inline fun anotherInlineCompanionFun() = yetAnotherNotReferencedOutsideOfThisFile()
  }
}

@PublishedApi internal fun notReferencedOutsideOfThisFile() = "foo"

@PublishedApi internal fun notReferencedOutsideOfThisFile(any: Any) = "foo"

@PublishedApi internal fun yetAnotherNotReferencedOutsideOfThisFile() = "foo"

fun yetYetAnotherNotReferencedOutsideOfThisFile() = "foo"

inline fun callAnotherCompanionInlineFunction() = ClassWithCompanion.inlineCompanionFun()

inline fun callAnotherInstanceInlineFunction() = ClassWithCompanion(Any()).inlineInstanceFun()

inline fun callNestedInlineFunctions() =
  ClassWithCompanion(Any()).inlineInstanceFunCallingCompanionInlineFun()

inline fun callInstanceInlineFunctionFromAnotherFile() =
  WithInlineFunction().callTopLevelNotReferencedFun()

inline fun callNotReferencedExtensionProperty() = ClassWithCompanion(Any()).notReferencedProperty

inline val ClassWithCompanion.notReferencedProperty
  get() = "RightExtensionProperty"

val notReferencedProperty = "foo"

class Foo

val Foo.notReferencedProperty
  get() = "WrongExtensionProperty"

inline fun callAmbiguousFunction() =
  funWithJvmSignatureConflict<String> { ArrayList<String>().asSequence() }

inline fun <T> funWithJvmSignatureConflict(f: (T) -> List<T>) = "funWithoutJvmName"

@OptIn(ExperimentalTypeInference::class)
@JvmName(name = "noConflict")
@OverloadResolutionByLambdaReturnType
inline fun <T> funWithJvmSignatureConflict(f: (T) -> Sequence<T>) = "funWithJvmName"
