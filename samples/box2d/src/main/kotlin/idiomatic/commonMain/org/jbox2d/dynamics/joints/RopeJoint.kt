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
package org.jbox2d.dynamics.joints

import org.jbox2d.common.MathUtils
import org.jbox2d.common.Rot
import org.jbox2d.common.Settings
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.SolverData
import org.jbox2d.pooling.IWorldPool

/**
 * A rope joint enforces a maximum distance between two points on two bodies. It has no other
 * effect. Warning: if you attempt to change the maximum length during the simulation you will get
 * some non-physical behavior. A model that would allow you to dynamically modify the length would
 * have some sponginess, so I chose not to implement it that way. See DistanceJoint if you want to
 * dynamically control length.
 *
 * @author Daniel Murphy
 */
class RopeJoint(worldPool: IWorldPool, def: RopeJointDef) : Joint(worldPool, def) {
  // Solver shared
  val localAnchorA = Vec2().apply { set(def.localAnchorA) }
  val localAnchorB = Vec2().apply { set(def.localAnchorB) }
  var maxLength: Float = def.maxLength
  var state: LimitState = LimitState.INACTIVE
    private set

  private var length: Float = 0.0f
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
  private var mass: Float = 0.0f

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
    val temp = pool.popVec2()
    qA.set(aA)
    qB.set(aB)

    // Compute the effective masses.
    Rot.mulToOutUnsafe(qA, temp.set(localAnchorA).subLocal(localCenterA), rA)
    Rot.mulToOutUnsafe(qB, temp.set(localAnchorB).subLocal(localCenterB), rB)
    u.set(cB).addLocal(rB).subLocal(cA).subLocal(rA)
    length = u.length()
    val C = length - maxLength
    state =
      if (C > 0.0f) {
        LimitState.AT_UPPER
      } else {
        LimitState.INACTIVE
      }
    if (length > Settings.LINEAR_SLOP) {
      u.mulLocal(1.0f / length)
    } else {
      u.setZero()
      mass = 0.0f
      impulse = 0.0f
      return
    }

    // Compute effective mass.
    val crA = Vec2.cross(rA, u)
    val crB = Vec2.cross(rB, u)
    val invMass = invMassA + invIA * crA * crA + invMassB + invIB * crB * crB
    mass = if (invMass != 0.0f) 1.0f / invMass else 0.0f
    if (data.step.warmStarting) {
      // Scale the impulse to support a variable time step.
      impulse *= data.step.dtRatio
      val Px = impulse * u.x
      val Py = impulse * u.y
      vA.x -= invMassA * Px
      vA.y -= invMassA * Py
      wA -= invIA * (rA.x * Py - rA.y * Px)
      vB.x += invMassB * Px
      vB.y += invMassB * Py
      wB += invIB * (rB.x * Py - rB.y * Px)
    } else {
      impulse = 0.0f
    }
    pool.pushRot(2)
    pool.pushVec2(1)

    // data.velocities[m_indexA].v = vA;
    data.velocities[indexA].w = wA
    // data.velocities[m_indexB].v = vB;
    data.velocities[indexB].w = wB
  }

  override fun solveVelocityConstraints(data: SolverData) {
    val vA = data.velocities[indexA].v
    var wA = data.velocities[indexA].w
    val vB = data.velocities[indexB].v
    var wB = data.velocities[indexB].w

    // Cdot = dot(u, v + cross(w, r))
    val vpA = pool.popVec2()
    val vpB = pool.popVec2()
    val temp = pool.popVec2()
    Vec2.crossToOutUnsafe(wA, rA, vpA)
    vpA.addLocal(vA)
    Vec2.crossToOutUnsafe(wB, rB, vpB)
    vpB.addLocal(vB)
    val C = length - maxLength
    var Cdot = Vec2.dot(u, temp.set(vpB).subLocal(vpA))

    // Predictive constraint.
    if (C < 0.0f) {
      Cdot += data.step.inv_dt * C
    }
    var localImpulse = -mass * Cdot
    val oldImpulse = impulse
    impulse = MathUtils.min(0.0f, impulse + localImpulse)
    localImpulse = impulse - oldImpulse
    val Px = localImpulse * u.x
    val Py = localImpulse * u.y
    vA.x -= invMassA * Px
    vA.y -= invMassA * Py
    wA -= invIA * (rA.x * Py - rA.y * Px)
    vB.x += invMassB * Px
    vB.y += invMassB * Py
    wB += invIB * (rB.x * Py - rB.y * Px)
    pool.pushVec2(3)

    // data.velocities[m_indexA].v = vA;
    data.velocities[indexA].w = wA
    // data.velocities[m_indexB].v = vB;
    data.velocities[indexB].w = wB
  }

  override fun solvePositionConstraints(data: SolverData): Boolean {
    val cA = data.positions[indexA].c
    var aA = data.positions[indexA].a
    val cB = data.positions[indexB].c
    var aB = data.positions[indexB].a
    val qA = pool.popRot()
    val qB = pool.popRot()
    val u = pool.popVec2()
    val rA = pool.popVec2()
    val rB = pool.popVec2()
    val temp = pool.popVec2()
    qA.set(aA)
    qB.set(aB)

    // Compute the effective masses.
    Rot.mulToOutUnsafe(qA, temp.set(localAnchorA).subLocal(localCenterA), rA)
    Rot.mulToOutUnsafe(qB, temp.set(localAnchorB).subLocal(localCenterB), rB)
    u.set(cB).addLocal(rB).subLocal(cA).subLocal(rA)
    val length = u.normalize()
    var C = length - maxLength
    C = MathUtils.clamp(C, 0.0f, Settings.MAX_LINEAR_CORRECTION)
    val impulse = -mass * C
    val Px = impulse * u.x
    val Py = impulse * u.y
    cA.x -= invMassA * Px
    cA.y -= invMassA * Py
    aA -= invIA * (rA.x * Py - rA.y * Px)
    cB.x += invMassB * Px
    cB.y += invMassB * Py
    aB += invIB * (rB.x * Py - rB.y * Px)
    pool.pushRot(2)
    pool.pushVec2(4)

    // data.positions[m_indexA].c = cA;
    data.positions[indexA].a = aA
    // data.positions[m_indexB].c = cB;
    data.positions[indexB].a = aB
    return length - maxLength < Settings.LINEAR_SLOP
  }

  override fun getAnchorA(out: Vec2) {
    bodyA.getWorldPointToOut(localAnchorA, out)
  }

  override fun getAnchorB(out: Vec2) {
    bodyB.getWorldPointToOut(localAnchorB, out)
  }

  override fun getReactionForce(inv_dt: Float, out: Vec2) {
    out.set(u).mulLocal(inv_dt).mulLocal(impulse)
  }

  override fun getReactionTorque(inv_dt: Float): Float = 0f
}
