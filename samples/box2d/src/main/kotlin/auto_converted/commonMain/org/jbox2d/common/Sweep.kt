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
 * This describes the motion of a body/shape for TOI computation. Shapes are defined with respect to
 * the body origin, which may no coincide with the center of mass. However, to support dynamics we
 * must interpolate the center of mass position.
 */
class Sweep : Serializable {
  /** Local center of mass position */
  val localCenter: Vec2

  /** Center world positions */
  val c0: Vec2
  val c: Vec2

  /** World angles */
  var a0 = 0f
  var a = 0f

  /** Fraction of the current time step in the range [0,1] c0 and a0 are the positions at alpha0. */
  var alpha0 = 0f
  override fun toString(): String {
    var s = "Sweep:\nlocalCenter: $localCenter\n"
    s += "c0: $c0, c: $c\n"
    s += "a0: $a0, a: $a\n"
    return s
  }

  init {
    localCenter = Vec2()
    c0 = Vec2()
    c = Vec2()
  }

  fun normalize() {
    val d = MathUtils.TWOPI * MathUtils.floor(a0 / MathUtils.TWOPI)
    a0 -= d
    a -= d
  }

  fun set(argCloneFrom: Sweep): Sweep {
    localCenter.set(argCloneFrom.localCenter)
    c0.set(argCloneFrom.c0)
    c.set(argCloneFrom.c)
    a0 = argCloneFrom.a0
    a = argCloneFrom.a
    return this
  }

  /**
   * Get the interpolated transform at a specific time.
   *
   * @param xf the result is placed here - must not be null
   * @param t the normalized time in [0,1].
   */
  fun getTransform(xf: Transform, beta: Float) {
    // assert is not supported in KMP.
    // assert(xf != null)
    // if (xf == null)
    // xf = new XForm();
    // center = p + R * localCenter
    /*
     * if (1.0f - t0 > Settings.EPSILON) { float alpha = (t - t0) / (1.0f - t0); xf.position.x =
     * (1.0f - alpha) * c0.x + alpha * c.x; xf.position.y = (1.0f - alpha) * c0.y + alpha * c.y;
     * float angle = (1.0f - alpha) * a0 + alpha * a; xf.R.set(angle); } else { xf.position.set(c);
     * xf.R.set(a); }
     */
    xf.p.x = (1.0f - beta) * c0.x + beta * c.x
    xf.p.y = (1.0f - beta) * c0.y + beta * c.y
    // float angle = (1.0f - alpha) * a0 + alpha * a;
    // xf.R.set(angle);
    xf.q.set((1.0f - beta) * a0 + beta * a)

    // Shift to origin
    // xf->p -= b2Mul(xf->q, localCenter);
    val q = xf.q
    xf.p.x -= q.cos * localCenter.x - q.sin * localCenter.y
    xf.p.y -= q.sin * localCenter.x + q.cos * localCenter.y
  }

  /**
   * Advance the sweep forward, yielding a new initial state.
   *
   * @param alpha the new initial time.
   */
  fun advance(alpha: Float) {
    //    assert (alpha0 < 1f);
    //    // c0 = (1.0f - t) * c0 + t*c;
    //    float beta = (alpha - alpha0) / (1.0f - alpha0);
    //    c0.x = (1.0f - beta) * c0.x + beta * c.x;
    //    c0.y = (1.0f - beta) * c0.y + beta * c.y;
    //    a0 = (1.0f - beta) * a0 + beta * a;
    //    alpha0 = alpha;
    c0.x = (1.0f - alpha) * c0.x + alpha * c.x
    c0.y = (1.0f - alpha) * c0.y + alpha * c.y
    a0 = (1.0f - alpha) * a0 + alpha * a
  }

  companion object {
    private const val serialVersionUID = 1L
  }
}
