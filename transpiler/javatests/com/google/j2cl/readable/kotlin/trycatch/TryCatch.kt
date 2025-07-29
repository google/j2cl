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
package trycatch

class TryCatch {

  fun testEmptyThrowableCatch() {
    try {
      throw ClassCastException()
    } catch (e: Throwable) {
      // expected empty body.
    }
    try {
      throw ClassCastException()
    } catch (e: Exception) {
      // expected empty body.
    } catch (e: Throwable) {
      // expected empty body.
    }
  }

  fun testEmptyThrowableRethrow() {
    try {
      throw ClassCastException()
    } catch (e: Throwable) {
      throw e
    }
  }

  fun testFinally() {
    try {
      val b = 2
    } finally {}
  }

  fun testNestedTryCatch() {
    try {
      throw Exception()
    } catch (ae: Exception) {
      try {
        throw Exception()
      } catch (ie: Exception) {
        // expected empty body.
      }
    }
  }

  fun testThrowGenerics() {
    throw getT(Exception())
  }

  private fun <T> getT(t: T): T {
    return t
  }

  fun testThrowBoundGenerics() {
    throw getThrowable()!!
  }

  private fun <T : Throwable> getThrowable(): T? {
    return null
  }

  fun <T : Throwable> testThrowsParameterized(throwable: T) {
    throw throwable
  }
}
