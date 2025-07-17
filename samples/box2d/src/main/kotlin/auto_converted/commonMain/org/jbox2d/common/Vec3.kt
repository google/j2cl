/*
 * ****************************************************************************
 * Copyright (c) 2013, Daniel Murphy All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 * * Redistributions of source code must retain the above copyright notice, this list of conditions
 *   and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice, this list of
 *   conditions and the following disclaimer in the documentation and/or other materials provided
 *   with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.jbox2d.common

import java.io.Serializable

/** @author Daniel Murphy */
class Vec3 : Serializable {
  var x: Float
  var y: Float
  var z: Float

  constructor() {
    z = 0f
    y = z
    x = y
  }

  constructor(argX: Float, argY: Float, argZ: Float) {
    x = argX
    y = argY
    z = argZ
  }

  constructor(argCopy: Vec3) {
    x = argCopy.x
    y = argCopy.y
    z = argCopy.z
  }

  fun set(argVec: Vec3): Vec3 {
    x = argVec.x
    y = argVec.y
    z = argVec.z
    return this
  }

  fun set(argX: Float, argY: Float, argZ: Float): Vec3 {
    x = argX
    y = argY
    z = argZ
    return this
  }

  fun addLocal(argVec: Vec3): Vec3 {
    x += argVec.x
    y += argVec.y
    z += argVec.z
    return this
  }

  fun add(argVec: Vec3): Vec3 {
    return Vec3(x + argVec.x, y + argVec.y, z + argVec.z)
  }

  fun subLocal(argVec: Vec3): Vec3 {
    x -= argVec.x
    y -= argVec.y
    z -= argVec.z
    return this
  }

  fun sub(argVec: Vec3): Vec3 {
    return Vec3(x - argVec.x, y - argVec.y, z - argVec.z)
  }

  fun mulLocal(argScalar: Float): Vec3 {
    x *= argScalar
    y *= argScalar
    z *= argScalar
    return this
  }

  fun mul(argScalar: Float): Vec3 {
    return Vec3(x * argScalar, y * argScalar, z * argScalar)
  }

  fun negate(): Vec3 {
    return Vec3(-x, -y, -z)
  }

  fun negateLocal(): Vec3 {
    x = -x
    y = -y
    z = -z
    return this
  }

  fun setZero() {
    x = 0f
    y = 0f
    z = 0f
  }

  fun clone(): Vec3 {
    return Vec3(this)
  }

  override fun toString(): String {
    return "($x,$y,$z)"
  }

  override fun hashCode(): Int {
    val prime = 31
    var result = 1
    result = prime * result + x.toBits()
    result = prime * result + y.toBits()
    result = prime * result + z.toBits()
    return result
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null) return false
    if (this::class != other::class) return false
    val newOther = other as Vec3
    if (x.toBits() != newOther.x.toBits()) return false
    if (y.toBits() != newOther.y.toBits()) return false
    if (z.toBits() != newOther.z.toBits()) return false
    return true
  }

  companion object {
    private const val serialVersionUID = 1L

    fun dot(a: Vec3, b: Vec3): Float {
      return a.x * b.x + a.y * b.y + a.z * b.z
    }

    fun cross(a: Vec3, b: Vec3): Vec3 {
      return Vec3(a.y * b.z - a.z * b.y, a.z * b.x - a.x * b.z, a.x * b.y - a.y * b.x)
    }

    fun crossToOut(a: Vec3, b: Vec3, out: Vec3) {
      val tempy = a.z * b.x - a.x * b.z
      val tempz = a.x * b.y - a.y * b.x
      out.x = a.y * b.z - a.z * b.y
      out.y = tempy
      out.z = tempz
    }

    fun crossToOutUnsafe(a: Vec3, b: Vec3, out: Vec3) {
      // assert is not supported in KMP.
      // assert(out !== b)
      // assert(out !== a)
      out.x = a.y * b.z - a.z * b.y
      out.y = a.z * b.x - a.x * b.z
      out.z = a.x * b.y - a.y * b.x
    }
  }
}
