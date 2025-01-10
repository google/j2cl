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

/** @author Daniel Murphy */
data class Vec3(var x: Float = 0f, var y: Float = 0f, var z: Float = 0f) {
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

  operator fun plus(argVec: Vec3): Vec3 = Vec3(x + argVec.x, y + argVec.y, z + argVec.z)

  operator fun minus(argVec: Vec3): Vec3 = Vec3(x - argVec.x, y - argVec.y, z - argVec.z)

  operator fun times(argScalar: Float): Vec3 = Vec3(x * argScalar, y * argScalar, z * argScalar)

  operator fun unaryMinus(): Vec3 = Vec3(-x, -y, -z)

  infix fun dot(b: Vec3): Float = x * b.x + y * b.y + z * b.z

  infix fun cross(b: Vec3): Vec3 = Vec3(y * b.z - z * b.y, z * b.x - x * b.z, x * b.y - y * b.x)

  fun addLocal(argVec: Vec3): Vec3 {
    x += argVec.x
    y += argVec.y
    z += argVec.z
    return this
  }

  fun subLocal(argVec: Vec3): Vec3 {
    x -= argVec.x
    y -= argVec.y
    z -= argVec.z
    return this
  }

  fun mulLocal(argScalar: Float): Vec3 {
    x *= argScalar
    y *= argScalar
    z *= argScalar
    return this
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

  override fun toString(): String {
    return "($x,$y,$z)"
  }

  companion object {
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
