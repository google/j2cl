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
package interfacedevirtualize

import com.google.j2cl.integration.testing.Asserts.assertTrue

/** Test CharSequence Interface on all devirtualized classes that implement it. */
fun testCharSequence() {
  val s = "string"
  val cs =
    object : CharSequence {
      override val length: Int
        get() = s.length

      override fun get(index: Int): Char {
        return s[index]
      }

      override fun subSequence(i: Int, j: Int): CharSequence {
        return s.subSequence(i, j)
      }

      override fun equals(c: Any?): Boolean {
        return s == c
      }

      override fun hashCode(): Int {
        return s.hashCode()
      }

      override fun toString(): String {
        return s.toString()
      }
    }

  assertTrue(s.length == 6)
  assertTrue(cs.length == 6)

  assertTrue(s[1] == 't')
  assertTrue(cs[1] == 't')

  assertTrue(s.subSequence(0, 2) == "st")
  assertTrue(cs.subSequence(0, 2) == "st")

  assertTrue(s == "string")
  assertTrue(cs.toString() == "string")

  assertTrue(s.hashCode() == "string".hashCode())
  assertTrue(cs.hashCode() == "string".hashCode())

  assertTrue(s.toString() == "string")
  assertTrue(cs.toString() == "string")

  assertTrue(s.javaClass == String::class.java)
  assertTrue(cs.javaClass != String::class.java)
}

fun testCharSequenceViaSuper() {
  val cs =
    object : CharSequence {
      override val length: Int
        get() = 0

      override fun get(index: Int): Char {
        return 0.toChar()
      }

      override fun subSequence(i: Int, j: Int): CharSequence {
        return this
      }

      override fun equals(c: Any?): Boolean {
        return super.equals(c)
      }

      override fun hashCode(): Int {
        return super.hashCode()
      }

      override fun toString(): String {
        return "sub" + super.toString()
      }
    }

  assertTrue(cs == cs)
  assertTrue(cs.toString() != "string")

  assertTrue(cs.hashCode() == System.identityHashCode(cs))
  assertTrue(cs.hashCode() != 0)

  assertTrue(cs.toString().startsWith("sub"))
}
