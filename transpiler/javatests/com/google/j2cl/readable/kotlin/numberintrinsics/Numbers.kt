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
package numberintrinsics

fun bytes() {
  val a = 0b0.toByte()
  val b = 0b0.toShort()
  val c = 0b0.toInt()
  val d = 0b0.toLong()
  val e = 0b0.toFloat()
  val f = 0b0.toDouble()
}

fun shorts() {
  val a: Short = 0
  val b = a.toByte()
  val c = a.toShort()
  val d = a.toInt()
  val e = a.toLong()
  val f = a.toFloat()
  val g = a.toDouble()
}

fun ints() {
  val a = 0.toByte()
  val b = 0.toShort()
  val c = 0.toInt()
  val d = 0.toLong()
  val e = 0.toFloat()
  val f = 0.toDouble()
}

fun longs() {
  val a = 0L.toByte()
  val b = 0L.toShort()
  val c = 0L.toInt()
  val d = 0L.toLong()
  val e = 0L.toFloat()
  val f = 0L.toDouble()
}

fun floats() {
  val a = 0F.toInt()
  val b = 0F.toLong()
  val c = 0F.toFloat()
  val d = 0F.toDouble()
}

fun double() {
  val a = 0.0.toInt()
  val b = 0.0.toLong()
  val c = 0.0.toFloat()
  val d = 0.0.toDouble()
}

fun numbers() {
  val a: Number = 0
  val b = a.toByte()
  val c = a.toShort()
  val d = a.toInt()
  val e = a.toLong()
  val f = a.toFloat()
  val g = a.toDouble()
}

fun compareTo() {
  val i: Int = 0
  val d: Double = 0.0
  val f: Float = 0.0f
  val b: Byte = i.toByte()
  val s: Short = i.toShort()
  val l: Long = 0L

  var x = 0

  x = i.compareTo(i)
  x = i.compareTo(d)
  x = i.compareTo(f)
  x = i.compareTo(b)
  x = i.compareTo(s)
  x = i.compareTo(l)

  x = d.compareTo(d)
  x = d.compareTo(i)
  x = d.compareTo(f)
  x = d.compareTo(b)
  x = d.compareTo(s)
  x = d.compareTo(l)

  x = f.compareTo(f)
  x = f.compareTo(i)
  x = f.compareTo(d)
  x = f.compareTo(b)
  x = f.compareTo(s)
  x = f.compareTo(l)

  x = b.compareTo(b)
  x = b.compareTo(i)
  x = b.compareTo(d)
  x = b.compareTo(f)
  x = b.compareTo(s)
  x = b.compareTo(l)

  x = s.compareTo(s)
  x = s.compareTo(i)
  x = s.compareTo(d)
  x = s.compareTo(f)
  x = s.compareTo(b)
  x = s.compareTo(l)

  x = l.compareTo(l)
  x = l.compareTo(i)
  x = l.compareTo(d)
  x = l.compareTo(f)
  x = l.compareTo(b)
  x = l.compareTo(s)

  // Char can only be compared to Char
  x = i.toChar().compareTo(i.toChar())

  // Boolean can only compare to Boolean.
  x = false.compareTo(true)
}
