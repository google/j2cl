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
package numberchilddevirtualcalls

import com.google.j2cl.integration.testing.Asserts.assertTrue

internal class NumberChild(private val x: Double, private val y: Double) : Number() {

  override fun toInt(): Int {
    return (x + y).toInt()
  }

  override fun toLong(): Long {
    return (x + y).toLong()
  }

  override fun toFloat(): Float {
    return (x + y).toFloat()
  }

  override fun toDouble(): Double {
    return x + y
  }

  override fun toByte(): Byte {
    return x.toInt().toByte()
  }

  override fun toShort(): Short {
    return toInt().toShort()
  }

  override fun toChar(): Char {
    throw AssertionError("Not Used")
  }
}

fun main(vararg unused: String) {
  val nc = NumberChild(2147483647.6, 2.6)
  assertTrue(nc.toByte() == (-1).toByte())
  assertTrue(nc.toDouble() == 2.1474836502E9)
  // assertTrue((nc.floatValue() == 2.14748365E9f)); // does not distinguish float and double.
  assertTrue(nc.toInt() == 2147483647)
  assertTrue(nc.toLong() == 2147483650L)
  assertTrue(nc.toShort() == (-1).toShort())

  assertTrue(NumberChild(3.6, 4.6).toByte() == 3.toByte())
  assertTrue(NumberChild(3.6, 4.6).toShort() == 8.toShort())
}
