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
package jsfunction

import jsinterop.annotations.JsFunction

@JsFunction
internal fun interface F {
  fun m(s: String): String
}

fun main(r: Int) {
  object {
    fun m() {
      val x = ""
      object {
        fun m() {
          val `var` = 1
          val f: F =
            object : F {
              override fun m(s: String): String {
                return (r.toString() +
                  s +
                  x +
                  `var` +
                  object : F {
                      override fun m(s: String): String {
                        return s + r + x + `var`
                      }
                    }
                    .m("hello"))
              }
            }
          val f2: F =
            object : F {
              override fun m(s: String): String {
                val r1 = r
                val var1 = `var`
                val x1 = x
                return (r.toString() +
                  s +
                  x +
                  `var` +
                  object : Any() {
                      fun sayHey(): String {
                        return "Hey"
                      }
                    }
                    .sayHey())
              }
            }
        }
      }
    }
  }

  // Make sure that optimized types are not leaked if they are propagated through inference.
  // TODO(b/355664391): Make sure that more complex cases of inference are handled correctly.
  val f =
    object : F {
      override fun m(s: String): String {
        return ""
      }
    }

  val array: Array<Any?> =
    arrayOf(
      object : F {
        override fun m(s: String): String {
          return ""
        }
      }
    )

  val array2 =
    arrayOf(
      object : F {
        override fun m(s: String): String {
          return ""
        }
      }
    )

  val array3 =
    arrayOf<Any?>(
      object : F {
        override fun m(s: String): String {
          return ""
        }
      }
    )

  Holder(
    object : F {
      override fun m(s: String): String {
        return ""
      }
    }
  )

  Holder(
    1,
    object : F {
      override fun m(s: String): String {
        return ""
      }
    },
  )
}

internal class Holder<T> {
  constructor(value: T)

  constructor(i: Int, vararg value: T)
}
