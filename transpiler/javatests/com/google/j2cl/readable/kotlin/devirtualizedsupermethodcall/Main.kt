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
package devirtualizedsupermethodcall

internal class SubNumber : Number() {
  override fun toInt(): Int {
    return 0
  }

  override fun toLong(): Long {
    return 0L
  }

  override fun toFloat(): Float {
    return 0F
  }

  override fun toDouble(): Double {
    return 0.00
  }

  override fun toChar(): Char {
    return '0'
  }

  override fun toShort(): Short {
    return 0
  }

  override fun toByte(): Byte {
    return 0
  }
}

class FooCallsSuperObjectMethod {
  override fun hashCode(): Int {
    return super.hashCode()
  }
}

class Main {
  fun main() {
    val fooCallsSuperObjectMethods = FooCallsSuperObjectMethod()
    fooCallsSuperObjectMethods.hashCode()

    val sn = SubNumber()
    sn.toByte()
    sn.toDouble()
    sn.toFloat()
    sn.toInt()
    sn.toShort()
  }
}
