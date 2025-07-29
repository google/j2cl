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
package jsasync

import com.google.j2cl.integration.testing.Asserts.assertTrue
import jsinterop.annotations.JsAsync
import jsinterop.annotations.JsFunction
import jsinterop.annotations.JsMethod

private fun five(): Promise<Int> {
  return Promise.resolve(5)
}

private fun ten(): Promise<Int> {
  return Promise.resolve(10)
}

@JsAsync
@JsMethod
private fun fifteen(): Promise<Int> {
  val a = await(five())
  assertTrue(a == 5)
  val asyncFunction = JsIntFunction {
    val ten = await(ten())
    Promise.resolve(ten)
  }
  val b = await(asyncFunction.get())
  assertTrue(b == 10)
  return Promise.resolve(a + b)
}

@JsAsync
fun main(@SuppressWarnings("unused") vararg args: String): Promise<*> {
  var result = await(fifteen())
  assertTrue(result == 15)
  result += await(InterfaceWithDefaultMethod.staticAsyncMethod())
  assertTrue(result == 20)
  result += await(object : InterfaceWithDefaultMethod {}.defaultAsyncMethod())
  assertTrue(result == 30)
  result += same(await(ten()) + await(ten()))
  assertTrue(result == 50)
  result += await(ClinitTest.X)
  assertTrue(result == 60)
  return Promise.resolve(result)
}

private fun same(i: Int): Int = i

@JsFunction
private fun interface JsIntFunction {
  @JsAsync fun get(): Promise<Int>
}

private interface InterfaceWithDefaultMethod {
  @JsAsync
  fun defaultAsyncMethod(): Promise<Int> {
    val result = await(ten())
    assertTrue(result == 10)
    return Promise.resolve(result)
  }

  companion object {
    @JsAsync
    @JvmStatic
    fun staticAsyncMethod(): Promise<Int> {
      val result = await(five())
      assertTrue(result == 5)
      return Promise.resolve(result)
    }
  }
}
