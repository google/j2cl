/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
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
package nativejstypes

fun testNativeJsTypeWithNamespace(): Int {
  val foo = Foo()
  return foo.sum()
}

fun testNativeJsTypeWithNamespaceJsProperties() {
  val foo = Foo()
  foo.x = 50
  foo.y = 5
}

fun testNativeJsTypeWithoutNamespace(): Int {
  val bar = Bar(6, 7)
  val unused = Bar.getStatic()
  return bar.product()
}

fun testNativeJsTypeWithoutNamespaceJsProperties() {
  val bar = Bar(6, 7)
  bar.x = 50
  bar.y = 5
  Bar.f = 10
}

fun testInnerNativeJsType() {
  val barInner = Bar.Inner(1)
  val barInnerWithDotInName = BarInnerWithDotInName(2)
  barInner.square()
  barInnerWithDotInName.square()
  Bar.Inner.getInnerStatic()
  BarInnerWithDotInName.getInnerStatic()
}

fun testGlobalNativeJsType() {
  val header = Headers()
  header.append("Content-Type", "text/xml")
}

fun testNativeTypeClassLiteral() {
  var o1: Any = Bar::class.java
  o1 = Array<Array<Bar>>::class.java
}

fun testNativeTypeObjectMethods() {
  val bar = Bar(6, 7)
  val unusedStr = bar.toString()
  val unusedHash = bar.hashCode()
  val unusedEq = bar == Any()
}

fun testTopLevel() {
  TopLevel.x = 2

  val nested = TopLevel.Nested()
  nested.x = 3

  val topLevelNestedReference = TopLevelNestedReference()
  topLevelNestedReference.x = 4
}
