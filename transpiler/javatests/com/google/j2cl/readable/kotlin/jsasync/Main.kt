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
package jsasync

import jsinterop.annotations.JsAsync
import jsinterop.annotations.JsFunction
import jsinterop.annotations.JsMethod
import jsinterop.annotations.JsPackage
import jsinterop.annotations.JsType

class Main {

  private val x: IThenable<Int> = Promise.resolve(10)

  @JsAsync fun emptyAsyncMethod(): IThenable<Unit>? = null

  @JsAsync
  fun asyncMethod(): IThenable<Int> {
    val result: Int = await(Promise.resolve(7))
    return parametricAsyncMethod(result)
  }

  @JsAsync
  fun <T> parametricAsyncMethod(value: T): IThenable<T> {
    return Promise.resolve(await(Promise.resolve(value)))
  }

  @JsAsync @JsMethod fun asyncAndJsMethod(): IThenable<Int>? = null

  companion object {
    @JsAsync
    @JvmStatic
    fun staticAsyncMethod(): IThenable<Unit> = Promise.resolve(await(Promise.resolve(Unit)))
  }

  @SuppressWarnings("unused")
  fun testAsyncLambdas() {
    val i = AsyncInterface { Promise.resolve(await(Promise.resolve(5))) }

    val o =
      object : BaseInterface {
        @JsAsync override fun asyncCall(): IThenable<Int> = Promise.resolve(await(x))
      }

    val j = AsyncJsFunctionInterface { Promise.resolve(await(Promise.resolve(5))) }

    val optimizableJsFunction =
      object : JsFunctionInterface {
        @JsAsync override fun doSomething(): IThenable<Int> = Promise.resolve(await(x))
      }

    val unoptimizableJsFunction =
      object : JsFunctionInterface {
        @JsAsync
        override fun doSomething(): IThenable<Int> {
          val unused: JsFunctionInterface = this
          return Promise.resolve(await(x))
        }
      }
  }
}

interface BaseInterface {
  fun asyncCall(): IThenable<Int>
}

fun interface AsyncInterface : BaseInterface {
  @JsAsync override fun asyncCall(): IThenable<Int>
}

interface InterfaceWithAsyncDefaultMethod {
  @JsAsync fun asyncCall(): IThenable<Int> = Promise.resolve(await(Promise.resolve(5)))
}

@JsFunction
fun interface JsFunctionInterface {
  fun doSomething(): IThenable<Int>
}

@JsFunction
fun interface AsyncJsFunctionInterface {
  @JsAsync fun doSomething(): IThenable<Int>
}

@JsType(isNative = true, namespace = JsPackage.GLOBAL) interface IThenable<T> {}

@JsType(isNative = true, namespace = JsPackage.GLOBAL)
private class Promise<T> : IThenable<T> {
  companion object {
    @JvmStatic external fun <T> resolve(value: T): Promise<T>
  }
}

@JsMethod(namespace = JsPackage.GLOBAL) private external fun <T> await(thenable: IThenable<T>): T
