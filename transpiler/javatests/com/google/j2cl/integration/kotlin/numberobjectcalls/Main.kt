/*
 * Copyright 2023 Google Inc.
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
package numberobjectcalls

import com.google.j2cl.integration.testing.Asserts.assertFalse
import com.google.j2cl.integration.testing.Asserts.assertTrue

fun main(vararg unused: String) {
  testEquals()
  testHashCode()
  testToString()
  testGetClass()
  testNumberSubclass()
}

private val B: Byte? = 1.toByte()
private val D: Double? = 1.0
private val F: Float? = 1.0f
private val I: Int? = 1
private val L: Long? = 1L
private val S: Short? = 1.toShort()
private val C: Char? = 'a'
private val BOOL: Boolean? = true

private fun testEquals() {
  // equals
  assertTrue(B == B)
  assertTrue(B == 1.toByte())
  assertTrue(D == D)
  assertTrue(D == 1.0)
  assertTrue(F == F)
  assertTrue(F == 1.0f)
  assertTrue(I == I)
  assertTrue(I == 1)
  assertTrue(L == L)
  assertTrue(L == 1L)
  assertTrue(S == S)
  assertTrue(S == 1.toShort())
  assertFalse(L as Any? == I)
  assertFalse(B as Any? == D)
  assertFalse(B as Any? == F)
  assertFalse(B as Any? == I)
  assertFalse(B as Any? == L)
  assertFalse(B as Any? == S)
  assertFalse(D as Any? == B)
  assertFalse(D as Any? == F)
  assertFalse(D as Any? == I)
  assertFalse(D as Any? == L)
  assertFalse(D as Any? == S)
  assertTrue(C == C)
  assertTrue(C == 'a')
  assertFalse(C == 'b')
  assertTrue(BOOL == BOOL)
  assertTrue(BOOL == true)
  assertFalse(BOOL == false)
}

private fun testHashCode() {
  // hashCode
  assertTrue(B.hashCode() == B.hashCode())
  assertTrue(D.hashCode() == D.hashCode())
  assertTrue(F.hashCode() == F.hashCode())
  assertTrue(I.hashCode() == I.hashCode())
  assertTrue(L.hashCode() == L.hashCode())
  assertTrue(S.hashCode() == S.hashCode())
  assertTrue(B.hashCode() == I.hashCode())
  assertTrue(L.hashCode() == I.hashCode())
  // GWT's JRE Long hashcode comes out different here than the JRE
  // TODO(dankurka): investigate
  // assertTrue((new Long(9223372036854775807L).hashCode() == -2147483648));
  assertTrue(C.hashCode() == C.hashCode())
  assertTrue(BOOL.hashCode() == BOOL.hashCode())
  assertTrue(BOOL.hashCode() != false.hashCode())
}

private fun testToString() {
  assertTrue(B.toString() == "1")
  // assertTrue((d.toString().equals("1.0"))); // d.toString().equals("1")
  // assertTrue((f.toString().equals("1.0"))); // f.toString().equals("1")
  assertTrue(I.toString() == "1")
  assertTrue(L.toString() == "1")
  assertTrue(S.toString() == "1")
  assertTrue(BOOL.toString() == "true")
}

private fun testGetClass() {
  assertTrue(B!!.javaClass is Class<*>)
  assertTrue(D!!.javaClass is Class<*>)
  assertTrue(F!!.javaClass is Class<*>)
  assertTrue(I!!.javaClass is Class<*>)
  assertTrue(L!!.javaClass is Class<*>)
  assertTrue(S!!.javaClass is Class<*>)
  assertTrue(B.javaClass.getName() == "java.lang.Byte")
  assertTrue(D.javaClass.getName() == "java.lang.Double")
  assertTrue(F.javaClass.getName() == "java.lang.Float")
  assertTrue(I.javaClass.getName() == "java.lang.Integer")
  assertTrue(L.javaClass.getName() == "java.lang.Long")
  assertTrue(S.javaClass.getName() == "java.lang.Short")
  assertTrue(C!!.javaClass.getName() == "java.lang.Character")
  assertTrue(BOOL!!.javaClass.getName() == "java.lang.Boolean")
}

private class SubNumber : Number() {
  override fun toInt(): Int {
    return 0
  }

  override fun toLong(): Long {
    return 0
  }

  override fun toShort(): Short {
    return 0
  }

  override fun toFloat(): Float {
    return 0f
  }

  override fun toDouble(): Double {
    return 0.0
  }

  override fun equals(o: Any?): Boolean {
    return o is SubNumber
  }

  override fun hashCode(): Int {
    return 100
  }

  override fun toByte(): Byte {
    return 0
  }

  override fun toChar(): Char {
    return 0.toChar()
  }

  override fun toString(): String {
    return "SubNumber"
  }
}

private fun testNumberSubclass() {
  val sn1 = SubNumber()
  val sn2 = SubNumber()
  assertTrue(sn1 == sn2)
  assertFalse(sn1 == Any())

  assertTrue(sn1.hashCode() == 100)
  assertTrue(sn2.hashCode() == 100)

  assertTrue(sn1.toString() == sn1.toString())

  assertTrue(sn1.javaClass is Class<*>)
  assertTrue(sn1.javaClass == sn2.javaClass)
}
