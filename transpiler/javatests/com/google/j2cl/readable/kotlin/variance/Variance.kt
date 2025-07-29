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
package variance

class Container<T>(var t: T)

interface In<in T> {
  fun m(t: T)

  fun mListIn(c: Container<out T>): Container<in T>
}

interface Out<out T> {
  fun m(): T

  fun mListOut(c: Container<in T>): Container<out T>
}

interface Bounded<T : CharSequence> {
  fun m(t: T): T

  fun mList(c: Container<T>): Container<T>
}

interface Constrained<T> where T : kotlin.Comparable<T>, T : CharSequence {
  fun m(t: T): T

  fun mList(c: Container<T>): Container<T>
}

fun <T> returnsT(): T {
  return null!!
}

fun testStarProjections() {
  var inStar: In<*> = Any() as In<*>
  inStar = returnsT()
  inStar =
    object : In<String> {
      override fun m(t: String) {}

      override fun mListIn(c: Container<out String>): Container<in String> {
        return Container<String>(c.t)
      }
    }
  var sIn: Any?
  sIn = inStar.mListIn(Container("Content")).t

  var outStar: Out<*> = Any() as Out<*>
  outStar = returnsT()
  outStar =
    object : Out<String> {
      override fun m(): String {
        return "Out"
      }

      override fun mListOut(c: Container<in String>): Container<out String> {
        c.t = "Hi"
        return Container(c.t as String)
      }
    }
  var sOut: String
  sOut = outStar.mListOut(Container("Initial")).t

  var boundedStar: Bounded<*> = Any() as Bounded<*>
  boundedStar = returnsT()
  boundedStar =
    object : Bounded<String> {
      override fun m(t: String): String = t

      override fun mList(c: Container<String>): Container<String> {
        return c
      }
    }
  var sBounded: String
  sBounded = boundedStar.mList(Container("Initial")).t

  var constrainedStar: Constrained<*> = Any() as Constrained<*>
  constrainedStar = returnsT()

  constrainedStar =
    object : Constrained<String> {
      override fun m(t: String): String = t

      override fun mList(c: Container<String>): Container<String> {
        return c
      }
    }
  var sConstrained: CharSequence
  sConstrained = constrainedStar.mList(Container("Initial")).t
}

class ImplementsIn : In<String?> {
  override fun m(t: String?) {}

  override fun mListIn(c: Container<out String?>): Container<in String?> {
    return null!!
  }
}

class ImplementsOut : Out<CharSequence?> {
  override fun m(): CharSequence? {
    return null!!
  }

  override fun mListOut(c: Container<in CharSequence?>): Container<out CharSequence?> {
    return null!!
  }
}

fun returnInCharSequence(): In<CharSequence?> {
  return null!!
}

fun returnOutString(): Out<String?> {
  return null!!
}

fun acceptsOutThrowable(unused: Iterable<Throwable>?) {}

fun getIterableOfException(): Iterable<Exception>? = null

fun testDeclarationVariancePropagation() {
  var inString: In<String?>?
  inString = returnInCharSequence()
  var outCharSequence: Out<CharSequence?>?
  outCharSequence = returnOutString()
  val s: CharSequence? = outCharSequence.m()
  val m = inString.mListIn(Container(""))

  // Iterable has declaration site variance in the Kotlin stdlib, but it's mapped to
  // java.lang.Iterable which does not.
  val outException: Iterable<Exception>? = getIterableOfException()
  acceptsOutThrowable(outException)
}
