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
package annotation

import kotlin.reflect.KClass

annotation class Foo(
  val booleanValue: Boolean = BOOLEAN_CONSTANT,
  val intValue: Int = INT_CONSTANT,
  val stringValue: String = STRING_CONSTANT,
  val classValue: KClass<*>,
  val stringClassValue: KClass<String>,
  val wildcardClassValue: KClass<out KClass<*>>,
  val enumValue: SomeEnum,
  val annotationValue: Zoo,
  val booleanArray: BooleanArray,
  val intArray: IntArray,
  val stringArray: Array<String>,
  val classArray: Array<KClass<*>>,
  val enumArray: Array<SomeEnum>,
  val annotationArray: Array<Zoo>,
) {
  companion object {
    const val BOOLEAN_CONSTANT = false
    const val INT_CONSTANT = 123
    const val STRING_CONSTANT = "foo"
  }
}

enum class SomeEnum {
  ZERO,
  ONE,
}

// Note that this inherits the single abstract method defined in its implicit parent
// java.lang.annotation.Annotation. Unlike Java, annotations are classes rather than interfaces,
// however, for Java interop behavior, and thus the J2CL model, they are interfaces. If this did not
// subtype Annotation then it would meet the requirements to be a functional interface.
annotation class Zoo

fun test(foo: Foo) {
  val booleanConstant: Boolean = Foo.BOOLEAN_CONSTANT
  val intConstant: Int = Foo.INT_CONSTANT
  val stringConstant: String = Foo.STRING_CONSTANT

  // TODO(b/393353894): rewrite call to `kotlin.Annotation#annotationClass()` to
  // `j.l.a.Annotation#annotationType()`
  // val annotationType = foo.annotationClass
  val annotationType = (foo as java.lang.annotation.Annotation).annotationType()

  val booleanValue: Boolean = foo.booleanValue
  val intValue: Int = foo.intValue
  val stringValue: String = foo.stringValue
  val classValue: KClass<*> = foo.classValue
  val stringClassValue: KClass<String> = foo.stringClassValue
  val wildcardClassValue: KClass<out KClass<*>> = foo.wildcardClassValue
  val enumValue: SomeEnum = foo.enumValue
  val annotationValue: Zoo = foo.annotationValue

  val booleanArray: BooleanArray = foo.booleanArray
  val intArray: IntArray = foo.intArray
  val stringArray: Array<String> = foo.stringArray
  val classArray: Array<KClass<*>> = foo.classArray
  val enumArray: Array<SomeEnum> = foo.enumArray
  val annotationArray: Array<Zoo> = foo.annotationArray
}

abstract class ClassImplementingAnnotation : Annotation

fun test(classImplementingAnnotation: ClassImplementingAnnotation): Annotation =
  classImplementingAnnotation

interface InterfaceExtendingAnnotation : Annotation

fun test(interfaceExtendingAnnotation: InterfaceExtendingAnnotation): Annotation =
  interfaceExtendingAnnotation
