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
package mixednestings

fun interface MyInterface {
  fun func(a: Int): Int
}

/**
 * Test lambda having access to local variables and arguments when placed in mixed scopes.
 * Local class -> local class -> anonymous -> lambda -> lambda -> anonymous
 */
class MixedNestings {
  fun mm() {}

  internal inner class A {
    fun aa() {}

    fun a() {
      class B {
        fun bb() {}

        fun b(): Int {
          val i = object : MyInterface {

            override fun func(a: Int): Int {
              val ii = MyInterface { n ->
                mm()
                aa()
                bb()
                val iii = MyInterface { m ->
                  mm()
                  aa()
                  bb()
                  object : MyInterface {
                    override fun func(b: Int): Int {
                      return b
                    }
                  }.func(100)
                }
                iii.func(200)
              }
              return ii.func(300)
            }
          }
          return i.func(400)
        }
      }
      B().b()
    }
  }

  fun test() {
    A().a()
  }
}
