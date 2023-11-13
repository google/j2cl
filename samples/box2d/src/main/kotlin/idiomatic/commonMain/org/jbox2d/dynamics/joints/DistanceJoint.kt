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
 * ****************************************************************************
 */
/*
 * JBox2D - A Java Port of Erin Catto's Box2D
 *
 * JBox2D homepage: http://jbox2d.sourceforge.net/
 * Box2D homepage: http://www.box2d.org
 *
 * This software is provided 'as-is', without any express or implied
 * warranty.  In no event will the authors be held liable for any damages
 * arising from the use of this software.
 *
 * Permission is granted to anyone to use this software for any purpose,
 * including commercial applications, and to alter it and redistribute it
 * freely, subject to the following restrictions:
 *
 * 1. The origin of this software must not be misrepresented; you must not
 * claim that you wrote the original software. If you use this software
 * in a product, an acknowledgment in the product documentation would be
 * appreciated but is not required.
 * 2. Altered source versions must be plainly marked as such, and must not be
 * misrepresented as being the original software.
 * 3. This notice may not be removed or altered from any source distribution.
 */
package org.jbox2d.dynamics.joints

import org.jbox2d.common.MathUtils
import org.jbox2d.common.Rot
import org.jbox2d.common.Settings
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.SolverData
import org.jbox2d.pooling.IWorldPool

// C = norm(p2 - p1) - L
// u = (p2 - p1) / norm(p2 - p1)
// Cdot = dot(u, v2 + cross(w2, r2) - v1 - cross(w1, r1))
// J = [-u -cross(r1, u) u cross(r2, u)]
// K = J * invM * JT
// = invMass1 + invI1 * cross(r1, u)^2 + invMass2 + invI2 * cross(r2, u)^2
/**
 * A distance joint constrains two points on two bodies to remain at a fixed distance from each
 * other. You can view this as a massless, rigid rod.
 */
class DistanceJoint(argWorld: IWorldPool, def: DistanceJointDef) : Joint(argWorld, def) {
  var frequencyHz: Float = def.frequencyHz
  var dampingRatio: Float = def.dampingRatio
  var length: Float = def.length
  val localAnchorA: Vec2 = def.localAnchorA.clone()
  val localAnchorB: Vec2 = def.localAnchorB.clone()

  private var bias: Float = 0.0f

  // Solver shared
  private var gamma: Float = 0.0f
  private var impulse: Float = 0.0f

  // Solver temp
  private var indexA = 0
  private var indexB = 0
  private val u = Vec2()
  private val rA = Vec2()
  private val rB = Vec2()
  private val localCenterA = Vec2()
  private val localCenterB = Vec2()
  private var invMassA = 0f
  private var invMassB = 0f
  private var invIA = 0f
  private var invIB = 0f
  private var mass = 0f

  override fun getAnchorA(out: Vec2) {
    bodyA.getWorldPointToOut(localAnchorA, out)
  }

  override fun getAnchorB(out: Vec2) {
    bodyB.getWorldPointToOut(localAnchorB, out)
  }

  /** Get the reaction force given the inverse time step. Unit is N. */
  override fun getReactionForce(inv_dt: Float, out: Vec2) {
    out.x = impulse * u.x * inv_dt
    out.y = impulse * u.y * inv_dt
  }

  /**
   * Get the reaction torque given the inverse time step. Unit is N*m. This is always zero for a
   * distance joint.
   */
  override fun getReactionTorque(inv_dt: Float): Float = 0.0f

  override fun initVelocityConstraints(data: SolverData) {
    indexA = bodyA.islandIndex
    indexB = bodyB.islandIndex
    localCenterA.set(bodyA.sweep.localCenter)
    localCenterB.set(bodyB.sweep.localCenter)
    invMassA = bodyA.invMass
    invMassB = bodyB.invMass
    invIA = bodyA.invI
    invIB = bodyB.invI
    val cA = data.positions[indexA].c
    val aA = data.positions[indexA].a
    val vA = data.velocities[indexA].v
    var wA = data.velocities[indexA].w
    val cB = data.positions[indexB].c
    val aB = data.positions[indexB].a
    val vB = data.velocities[indexB].v
    var wB = data.velocities[indexB].w
    val qA = pool.popRot()
    val qB = pool.popRot()
    qA.set(aA)
    qB.set(aB)

    // use m_u as temporary variable
    Rot.mulToOutUnsafe(qA, u.set(localAnchorA).subLocal(localCenterA), rA)
    Rot.mulToOutUnsafe(qB, u.set(localAnchorB).subLocal(localCenterB), rB)
    u.set(cB).addLocal(rB).subLocal(cA).subLocal(rA)
    pool.pushRot(2)

    // Handle singularity.
    val ULength = u.length()
    if (ULength > Settings.LINEAR_SLOP) {
      u.x *= 1.0f / ULength
      u.y *= 1.0f / ULength
    } else {
      u.set(0.0f, 0.0f)
    }
    val crAu = Vec2.cross(rA, u)
    val crBu = Vec2.cross(rB, u)
    var invMass = invMassA + invIA * crAu * crAu + invMassB + invIB * crBu * crBu

    // Compute the effective mass matrix.
    mass = if (invMass != 0.0f) 1.0f / invMass else 0.0f
    if (frequencyHz > 0.0f) {
      val C = ULength - length

      // Frequency
      val omega = 2.0f * MathUtils.PI * frequencyHz

      // Damping coefficient
      val d = 2.0f * mass * dampingRatio * omega

      // Spring stiffness
      val k = mass * omega * omega

      // magic formulas
      val h = data.step.dt
      gamma = h * (d + h * k)
      gamma = if (gamma != 0.0f) 1.0f / gamma else 0.0f
      bias = C * h * k * gamma
      invMass += gamma
      mass = if (invMass != 0.0f) 1.0f / invMass else 0.0f
    } else {
      gamma = 0.0f
      bias = 0.0f
    }
    if (data.step.warmStarting) {

      // Scale the impulse to support a variable time step.
      impulse *= data.step.dtRatio
      val P = pool.popVec2()
      P.set(u).mulLocal(impulse)
      vA.x -= invMassA * P.x
      vA.y -= invMassA * P.y
      wA -= invIA * Vec2.cross(rA, P)
      vB.x += invMassB * P.x
      vB.y += invMassB * P.y
      wB += invIB * Vec2.cross(rB, P)
      pool.pushVec2(1)
    } else {
      impulse = 0.0f
    }
    //    data.velocities[m_indexA].v.set(vA);
    data.velocities[indexA].w = wA
    //    data.velocities[m_indexB].v.set(vB);
    data.velocities[indexB].w = wB
  }

  override fun solveVelocityConstraints(data: SolverData) {
    val vA = data.velocities[indexA].v
    var wA = data.velocities[indexA].w
    val vB = data.velocities[indexB].v
    var wB = data.velocities[indexB].w
    val vpA = pool.popVec2()
    val vpB = pool.popVec2()

    // Cdot = dot(u, v + cross(w, r))
    Vec2.crossToOutUnsafe(wA, rA, vpA)
    vpA.addLocal(vA)
    Vec2.crossToOutUnsafe(wB, rB, vpB)
    vpB.addLocal(vB)
    val Cdot = Vec2.dot(u, vpB.subLocal(vpA))
    val localImpulse = -mass * (Cdot + bias + gamma * impulse)
    impulse += localImpulse
    val Px = localImpulse * u.x
    val Py = localImpulse * u.y
    vA.x -= invMassA * Px
    vA.y -= invMassA * Py
    wA -= invIA * (rA.x * Py - rA.y * Px)
    vB.x += invMassB * Px
    vB.y += invMassB * Py
    wB += invIB * (rB.x * Py - rB.y * Px)

    //    data.velocities[m_indexA].v.set(vA);
    data.velocities[indexA].w = wA
    //    data.velocities[m_indexB].v.set(vB);
    data.velocities[indexB].w = wB
    pool.pushVec2(2)
  }

  override fun solvePositionConstraints(data: SolverData): Boolean {
    if (frequencyHz > 0.0f) {
      return true
    }
    val qA = pool.popRot()
    val qB = pool.popRot()
    val rA = pool.popVec2()
    val rB = pool.popVec2()
    val u = pool.popVec2()
    val cA = data.positions[indexA].c
    var aA = data.positions[indexA].a
    val cB = data.positions[indexB].c
    var aB = data.positions[indexB].a
    qA.set(aA)
    qB.set(aB)
    Rot.mulToOutUnsafe(qA, u.set(localAnchorA).subLocal(localCenterA), rA)
    Rot.mulToOutUnsafe(qB, u.set(localAnchorB).subLocal(localCenterB), rB)
    u.set(cB).addLocal(rB).subLocal(cA).subLocal(rA)
    val ULength = u.normalize()
    var C = ULength - length
    C = MathUtils.clamp(C, -Settings.MAX_LINEAR_CORRECTION, Settings.MAX_LINEAR_CORRECTION)
    val impulse = -mass * C
    val Px = impulse * u.x
    val Py = impulse * u.y
    cA.x -= invMassA * Px
    cA.y -= invMassA * Py
    aA -= invIA * (rA.x * Py - rA.y * Px)
    cB.x += invMassB * Px
    cB.y += invMassB * Py
    aB += invIB * (rB.x * Py - rB.y * Px)

    //    data.positions[m_indexA].c.set(cA);
    data.positions[indexA].a = aA
    //    data.positions[m_indexB].c.set(cB);
    data.positions[indexB].a = aB
    pool.pushVec2(3)
    pool.pushRot(2)
    return MathUtils.abs(C) < Settings.LINEAR_SLOP
  }
}
