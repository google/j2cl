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

/**
 * Represents a rotation
 *
 * @author Daniel
 */
class Rot : Serializable {
  // sin and cos
  var sin = 0f
  var cos = 0f
  val angle: Float
    get() = MathUtils.atan2(sin, cos)

  constructor() {
    setIdentity()
  }

  constructor(angle: Float) {
    set(angle)
  }

  override fun toString(): String = "Rot(s:$sin, c:$cos)"

  fun set(angle: Float): Rot {
    sin = MathUtils.sin(angle)
    cos = MathUtils.cos(angle)
    return this
  }

  fun set(other: Rot): Rot {
    sin = other.sin
    cos = other.cos
    return this
  }

  fun setIdentity(): Rot {
    sin = 0f
    cos = 1f
    return this
  }

  fun getXAxis(xAxis: Vec2) {
    xAxis.set(cos, sin)
  }

  fun getYAxis(yAxis: Vec2) {
    yAxis.set(-sin, cos)
  }

  // @Override // annotation omitted for GWT-compatibility
  fun clone(): Rot {
    val copy = Rot()
    copy.sin = sin
    copy.cos = cos
    return copy
  }

  companion object {

    fun mul(q: Rot, r: Rot, out: Rot) {
      val tempc = q.cos * r.cos - q.sin * r.sin
      out.sin = q.sin * r.cos + q.cos * r.sin
      out.cos = tempc
    }

    fun mulUnsafe(q: Rot, r: Rot, out: Rot) {
      // assert is not supported in KMP.
      // assert(r !== out)
      // assert(q !== out)
      // [qc -qs] * [rc -rs] = [qc*rc-qs*rs -qc*rs-qs*rc]
      // [qs qc] [rs rc] [qs*rc+qc*rs -qs*rs+qc*rc]
      // s = qs * rc + qc * rs
      // c = qc * rc - qs * rs
      out.sin = q.sin * r.cos + q.cos * r.sin
      out.cos = q.cos * r.cos - q.sin * r.sin
    }

    fun mulTrans(q: Rot, r: Rot, out: Rot) {
      val tempc = q.cos * r.cos + q.sin * r.sin
      out.sin = q.cos * r.sin - q.sin * r.cos
      out.cos = tempc
    }

    fun mulTransUnsafe(q: Rot, r: Rot, out: Rot) {
      // [ qc qs] * [rc -rs] = [qc*rc+qs*rs -qc*rs+qs*rc]
      // [-qs qc] [rs rc] [-qs*rc+qc*rs qs*rs+qc*rc]
      // s = qc * rs - qs * rc
      // c = qc * rc + qs * rs
      out.sin = q.cos * r.sin - q.sin * r.cos
      out.cos = q.cos * r.cos + q.sin * r.sin
    }

    fun mulToOut(q: Rot, v: Vec2, out: Vec2) {
      val tempy = q.sin * v.x + q.cos * v.y
      out.x = q.cos * v.x - q.sin * v.y
      out.y = tempy
    }

    fun mulToOutUnsafe(q: Rot, v: Vec2, out: Vec2) {
      out.x = q.cos * v.x - q.sin * v.y
      out.y = q.sin * v.x + q.cos * v.y
    }

    fun mulTrans(q: Rot, v: Vec2, out: Vec2) {
      val tempy = -q.sin * v.x + q.cos * v.y
      out.x = q.cos * v.x + q.sin * v.y
      out.y = tempy
    }

    fun mulTransUnsafe(q: Rot, v: Vec2, out: Vec2) {
      out.x = q.cos * v.x + q.sin * v.y
      out.y = -q.sin * v.x + q.cos * v.y
    }
  }
}
