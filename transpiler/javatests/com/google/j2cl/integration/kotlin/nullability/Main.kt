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
package nullability

fun main() {
  testExplicitInvocationTypeArguments()
  testExplicitConstructorTypeArguments()
  testImplicitInvocationTypeArguments()
  testImplicitConstructorTypeArguments()
  testImplicitInvocationTypeArguments_wildcards()
  testImplicitConstructorTypeArguments_wildcards()
  testImplicitConstructorTypeArguments_inference()
  testLambdaReturnTypeInference()
  testNullWildcardInLambda()
}

const val STRING: String = "foo"
val NULL_STRING: String? = null

private fun testArrayLiteral() {
  val unusedArray1 = arrayOf(STRING, NULL_STRING)
  val unusedArray2 = arrayOf(NULL_STRING, STRING)
}

private fun testNewArray() {
  val unusedArray1 = arrayOf(STRING, NULL_STRING)
  val unusedArray2 = arrayOf(NULL_STRING, STRING)
}

private fun testExplicitInvocationTypeArguments() {
  accept1<String?>(STRING)
  accept1<String?>(NULL_STRING)
  accept2<String?>(NULL_STRING, STRING)
  accept2<String?>(STRING, NULL_STRING)
  acceptVarargs<String?>()
  acceptVarargs<String?>(STRING)
  acceptVarargs<String?>(NULL_STRING)
  acceptVarargs<String?>(STRING, NULL_STRING)
  acceptVarargs<String?>(NULL_STRING, STRING)
}

private fun testExplicitConstructorTypeArguments() {
  Consumer<String?>(STRING)
  Consumer<String?>(NULL_STRING)
  Consumer<String?>(NULL_STRING, STRING)
  Consumer<String?>(STRING, NULL_STRING)
  VarargConsumer<String?>()
  VarargConsumer<String?>(STRING)
  VarargConsumer<String?>(NULL_STRING)
  VarargConsumer<String?>(STRING, NULL_STRING)
  VarargConsumer<String?>(NULL_STRING, STRING)
}

private fun testImplicitInvocationTypeArguments() {
  accept1(STRING)
  accept1(NULL_STRING)
  acceptVarargs<String?>()
  acceptVarargs(STRING)
  acceptVarargs(NULL_STRING)

  accept1(NULL_STRING)

  accept2(NULL_STRING, STRING)
  accept2(STRING, NULL_STRING)

  acceptVarargs(STRING, NULL_STRING)
  acceptVarargs(NULL_STRING, STRING)
}

private fun testImplicitConstructorTypeArguments() {
  Consumer(STRING)
  Consumer(NULL_STRING)
  VarargConsumer(STRING)
  VarargConsumer(NULL_STRING)

  Consumer(NULL_STRING)

  Consumer(NULL_STRING, STRING)
  Consumer(STRING, NULL_STRING)

  VarargConsumer(STRING, NULL_STRING)
  VarargConsumer(NULL_STRING, STRING)
}

private fun testImplicitInvocationTypeArguments_wildcards() {
  val supplier: Supplier<*> = Supplier.of<String?>(NULL_STRING)
  accept1(supplier.getValue())
  accept2(STRING, supplier.getValue())
  acceptVarargs(STRING, supplier.getValue())
}

private fun testImplicitConstructorTypeArguments_wildcards() {
  val supplier: Supplier<*> = Supplier.of<String?>(NULL_STRING)
  Consumer(supplier.getValue())
  Consumer(STRING, supplier.getValue())
  VarargConsumer(STRING, supplier.getValue())
}

private fun testImplicitConstructorTypeArguments_inference() {
  Consumer(STRING, NULL_STRING).accept(NULL_STRING)
  VarargConsumer(STRING, NULL_STRING).accept(NULL_STRING)
}

private fun testLambdaReturnTypeInference() {
  acceptSupplier(Supplier { null })
}

fun <C> testNullableAcceptNullable2Vararg(nonNull: C) {
  val localNonNull = nonNull
  acceptNullable2Varargs(localNonNull, nonNull, nonNull)
}

fun <C : Any> testNonNullAcceptNullable2Vararg(nonNull: C) {
  val localNonNull = nonNull
  acceptNullable2Varargs(localNonNull, nonNull, nonNull)
}

fun <C : Any> testNonNullAcceptNonNull2Vararg(nonNull: C) {
  val localNonNull = nonNull
  acceptNonNull2Varargs(localNonNull, nonNull, nonNull)
}

private fun <T> accept1(t: T) {}

private fun <T> accept2(t1: T, t2: T) {}

private fun <T> acceptVarargs(vararg t: T) {}

private fun <T> acceptNullable2Varargs(t1: T, t2: T, vararg t: T) {}

private fun <T : Any> acceptNonNull2Varargs(t1: T, t2: T, vararg t: T) {}

private fun <V> acceptSupplier(t: Supplier<V>) {}

private class Consumer<T> {
  constructor(t: T)

  constructor(t1: T, t2: T)

  fun accept(t: T) {}
}

private class VarargConsumer<T> {
  constructor(vararg ts: T)

  fun accept(t: T) {}
}

private fun interface Supplier<V> {
  fun getValue(): V

  companion object {
    fun <V> of(v: V): Supplier<V> = Supplier { v }
  }
}

private fun testNullWildcardInLambda() {
  val supplier: Supplier<*> = wrap { null }
  supplier.getValue()
}

private fun <T> wrap(supplier: Supplier<out T>): Supplier<T> {
  return Supplier { supplier.getValue() }
}
