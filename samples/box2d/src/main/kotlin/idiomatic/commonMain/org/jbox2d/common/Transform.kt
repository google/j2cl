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
import org.jbox2d.common.Rot.Companion.mul
import org.jbox2d.common.Rot.Companion.mulToOut
import org.jbox2d.common.Rot.Companion.mulToOutUnsafe
import org.jbox2d.common.Rot.Companion.mulTrans
import org.jbox2d.common.Rot.Companion.mulTransUnsafe
import org.jbox2d.common.Rot.Companion.mulUnsafe

// updated to rev 100
/**
 * A transform contains translation and rotation. It is used to represent the position and
 * orientation of rigid frames.
 */
class Transform : Serializable {
  /** The translation caused by the transform */
  val p: Vec2

  /** A matrix representing a rotation */
  val q: Rot

  /** The default constructor. */
  constructor() {
    p = Vec2()
    q = Rot()
  }

  /** Initialize as a copy of another transform. */
  constructor(xf: Transform) {
    p = xf.p.clone()
    q = xf.q.clone()
  }

  /** Initialize using a position vector and a rotation matrix. */
  constructor(position: Vec2, rot: Rot) {
    p = position.clone()
    q = rot.clone()
  }

  /** Set this to equal another transform. */
  fun set(xf: Transform): Transform {
    p.set(xf.p)
    q.set(xf.q)
    return this
  }

  /**
   * Set this based on the position and angle.
   *
   * @param p
   * @param angle
   */
  fun set(p: Vec2, angle: Float) {
    this.p.set(p)
    q.set(angle)
  }

  /** Set this to the identity transform. */
  fun setIdentity() {
    p.setZero()
    q.setIdentity()
  }

  override fun toString(): String {
    var s = "XForm:\n"
    s += "Position: $p\n"
    s += "R: \n$q\n"
    return s
  }

  companion object {
    private val pool = Vec2()

    fun mul(transform: Transform, v: Vec2): Vec2 =
      Vec2(
        transform.q.cos * v.x - transform.q.sin * v.y + transform.p.x,
        transform.q.sin * v.x + transform.q.cos * v.y + transform.p.y
      )

    fun mulToOut(transform: Transform, v: Vec2, out: Vec2) {
      val tempy = transform.q.sin * v.x + transform.q.cos * v.y + transform.p.y
      out.x = transform.q.cos * v.x - transform.q.sin * v.y + transform.p.x
      out.y = tempy
    }

    fun mulToOutUnsafe(transform: Transform, v: Vec2, out: Vec2) {
      // assert is not supported in KMP.
      // assert(v !== out)
      out.x = transform.q.cos * v.x - transform.q.sin * v.y + transform.p.x
      out.y = transform.q.sin * v.x + transform.q.cos * v.y + transform.p.y
    }

    fun mulTrans(transform: Transform, v: Vec2): Vec2 {
      val px = v.x - transform.p.x
      val py = v.y - transform.p.y
      return Vec2(
        transform.q.cos * px + transform.q.sin * py,
        -transform.q.sin * px + transform.q.cos * py
      )
    }

    fun mulTransToOut(transform: Transform, v: Vec2, out: Vec2) {
      val px = v.x - transform.p.x
      val py = v.y - transform.p.y
      val tempy = -transform.q.sin * px + transform.q.cos * py
      out.x = transform.q.cos * px + transform.q.sin * py
      out.y = tempy
    }

    fun mulTransToOutUnsafe(transform: Transform, v: Vec2, out: Vec2) {
      // assert is not supported in KMP.
      // assert(v !== out)
      val px = v.x - transform.p.x
      val py = v.y - transform.p.y
      out.x = transform.q.cos * px + transform.q.sin * py
      out.y = -transform.q.sin * px + transform.q.cos * py
    }

    fun mul(transformA: Transform, transformB: Transform): Transform {
      val transformC = Transform()
      mulUnsafe(transformA.q, transformB.q, transformC.q)
      mulToOutUnsafe(transformA.q, transformB.p, transformC.p)
      transformC.p.addLocal(transformA.p)
      return transformC
    }

    fun mulToOut(transformA: Transform, transformB: Transform, out: Transform) {
      // assert is not supported in KMP.
      // assert(out !== A)
      mul(transformA.q, transformB.q, out.q)
      mulToOut(transformA.q, transformB.p, out.p)
      out.p.addLocal(transformA.p)
    }

    fun mulToOutUnsafe(transformA: Transform, transformB: Transform, out: Transform) {
      // assert is not supported in KMP.
      // assert(out !== B)
      // assert(out !== A)
      mulUnsafe(transformA.q, transformB.q, out.q)
      mulToOutUnsafe(transformA.q, transformB.p, out.p)
      out.p.addLocal(transformA.p)
    }

    fun mulTrans(transformA: Transform, transformB: Transform): Transform {
      val transformC = Transform()
      mulTransUnsafe(transformA.q, transformB.q, transformC.q)
      pool.set(transformB.p).subLocal(transformA.p)
      mulTransUnsafe(transformA.q, pool, transformC.p)
      return transformC
    }

    fun mulTransToOut(transformA: Transform, transformB: Transform, out: Transform) {
      // assert is not supported in KMP.
      // assert(out !== A)
      mulTrans(transformA.q, transformB.q, out.q)
      pool.set(transformB.p).subLocal(transformA.p)
      mulTrans(transformA.q, pool, out.p)
    }

    fun mulTransToOutUnsafe(transformA: Transform, transformB: Transform, out: Transform) {
      // assert is not supported in KMP.
      // assert(out !== A)
      // assert(out !== B)
      mulTransUnsafe(transformA.q, transformB.q, out.q)
      pool.set(transformB.p).subLocal(transformA.p)
      mulTransUnsafe(transformA.q, pool, out.p)
    }
  }
}
