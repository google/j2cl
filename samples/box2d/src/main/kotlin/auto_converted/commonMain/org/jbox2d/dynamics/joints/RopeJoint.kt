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
  private val m_localAnchorA = Vec2()
  private val m_localAnchorB = Vec2()
  private var m_maxLength: Float
  private var m_length: Float
  private var m_impulse: Float

  // Solver temp
  private var m_indexA = 0
  private var m_indexB = 0
  private val m_u = Vec2()
  private val m_rA = Vec2()
  private val m_rB = Vec2()
  private val m_localCenterA = Vec2()
  private val m_localCenterB = Vec2()
  private var m_invMassA = 0f
  private var m_invMassB = 0f
  private var m_invIA = 0f
  private var m_invIB = 0f
  private var m_mass: Float
  private var m_state: LimitState

  init {
    m_localAnchorA.set(def.localAnchorA)
    m_localAnchorB.set(def.localAnchorB)
    m_maxLength = def.maxLength
    m_mass = 0.0f
    m_impulse = 0.0f
    m_state = LimitState.INACTIVE
    m_length = 0.0f
  }

  override fun initVelocityConstraints(data: SolverData) {
    m_indexA = m_bodyA!!.m_islandIndex
    m_indexB = m_bodyB!!.m_islandIndex
    m_localCenterA.set(m_bodyA!!.m_sweep.localCenter)
    m_localCenterB.set(m_bodyB!!.m_sweep.localCenter)
    m_invMassA = m_bodyA!!.m_invMass
    m_invMassB = m_bodyB!!.m_invMass
    m_invIA = m_bodyA!!.m_invI
    m_invIB = m_bodyB!!.m_invI
    val cA = data.positions!![m_indexA].c
    val aA = data.positions!![m_indexA].a
    val vA = data.velocities!![m_indexA].v
    var wA = data.velocities!![m_indexA].w
    val cB = data.positions!![m_indexB].c
    val aB = data.positions!![m_indexB].a
    val vB = data.velocities!![m_indexB].v
    var wB = data.velocities!![m_indexB].w
    val qA = pool.popRot()
    val qB = pool.popRot()
    val temp = pool.popVec2()
    qA.set(aA)
    qB.set(aB)

    // Compute the effective masses.
    Rot.mulToOutUnsafe(qA, temp.set(m_localAnchorA).subLocal(m_localCenterA), m_rA)
    Rot.mulToOutUnsafe(qB, temp.set(m_localAnchorB).subLocal(m_localCenterB), m_rB)
    m_u.set(cB).addLocal(m_rB).subLocal(cA).subLocal(m_rA)
    m_length = m_u.length()
    val C = m_length - m_maxLength
    m_state =
      if (C > 0.0f) {
        LimitState.AT_UPPER
      } else {
        LimitState.INACTIVE
      }
    if (m_length > Settings.linearSlop) {
      m_u.mulLocal(1.0f / m_length)
    } else {
      m_u.setZero()
      m_mass = 0.0f
      m_impulse = 0.0f
      return
    }

    // Compute effective mass.
    val crA = Vec2.cross(m_rA, m_u)
    val crB = Vec2.cross(m_rB, m_u)
    val invMass = m_invMassA + m_invIA * crA * crA + m_invMassB + m_invIB * crB * crB
    m_mass = if (invMass != 0.0f) 1.0f / invMass else 0.0f
    if (data.step!!.warmStarting) {
      // Scale the impulse to support a variable time step.
      m_impulse *= data.step!!.dtRatio
      val Px = m_impulse * m_u.x
      val Py = m_impulse * m_u.y
      vA.x -= m_invMassA * Px
      vA.y -= m_invMassA * Py
      wA -= m_invIA * (m_rA.x * Py - m_rA.y * Px)
      vB.x += m_invMassB * Px
      vB.y += m_invMassB * Py
      wB += m_invIB * (m_rB.x * Py - m_rB.y * Px)
    } else {
      m_impulse = 0.0f
    }
    pool.pushRot(2)
    pool.pushVec2(1)

    // data.velocities[m_indexA].v = vA;
    data.velocities!![m_indexA].w = wA
    // data.velocities[m_indexB].v = vB;
    data.velocities!![m_indexB].w = wB
  }

  override fun solveVelocityConstraints(data: SolverData) {
    val vA = data.velocities!![m_indexA].v
    var wA = data.velocities!![m_indexA].w
    val vB = data.velocities!![m_indexB].v
    var wB = data.velocities!![m_indexB].w

    // Cdot = dot(u, v + cross(w, r))
    val vpA = pool.popVec2()
    val vpB = pool.popVec2()
    val temp = pool.popVec2()
    Vec2.crossToOutUnsafe(wA, m_rA, vpA)
    vpA.addLocal(vA)
    Vec2.crossToOutUnsafe(wB, m_rB, vpB)
    vpB.addLocal(vB)
    val C = m_length - m_maxLength
    var Cdot = Vec2.dot(m_u, temp.set(vpB).subLocal(vpA))

    // Predictive constraint.
    if (C < 0.0f) {
      Cdot += data.step!!.inv_dt * C
    }
    var impulse = -m_mass * Cdot
    val oldImpulse = m_impulse
    m_impulse = MathUtils.min(0.0f, m_impulse + impulse)
    impulse = m_impulse - oldImpulse
    val Px = impulse * m_u.x
    val Py = impulse * m_u.y
    vA.x -= m_invMassA * Px
    vA.y -= m_invMassA * Py
    wA -= m_invIA * (m_rA.x * Py - m_rA.y * Px)
    vB.x += m_invMassB * Px
    vB.y += m_invMassB * Py
    wB += m_invIB * (m_rB.x * Py - m_rB.y * Px)
    pool.pushVec2(3)

    // data.velocities[m_indexA].v = vA;
    data.velocities!![m_indexA].w = wA
    // data.velocities[m_indexB].v = vB;
    data.velocities!![m_indexB].w = wB
  }

  override fun solvePositionConstraints(data: SolverData): Boolean {
    val cA = data.positions!![m_indexA].c
    var aA = data.positions!![m_indexA].a
    val cB = data.positions!![m_indexB].c
    var aB = data.positions!![m_indexB].a
    val qA = pool.popRot()
    val qB = pool.popRot()
    val u = pool.popVec2()
    val rA = pool.popVec2()
    val rB = pool.popVec2()
    val temp = pool.popVec2()
    qA.set(aA)
    qB.set(aB)

    // Compute the effective masses.
    Rot.mulToOutUnsafe(qA, temp.set(m_localAnchorA).subLocal(m_localCenterA), rA)
    Rot.mulToOutUnsafe(qB, temp.set(m_localAnchorB).subLocal(m_localCenterB), rB)
    u.set(cB).addLocal(rB).subLocal(cA).subLocal(rA)
    val length = u.normalize()
    var C = length - m_maxLength
    C = MathUtils.clamp(C, 0.0f, Settings.maxLinearCorrection)
    val impulse = -m_mass * C
    val Px = impulse * u.x
    val Py = impulse * u.y
    cA.x -= m_invMassA * Px
    cA.y -= m_invMassA * Py
    aA -= m_invIA * (rA.x * Py - rA.y * Px)
    cB.x += m_invMassB * Px
    cB.y += m_invMassB * Py
    aB += m_invIB * (rB.x * Py - rB.y * Px)
    pool.pushRot(2)
    pool.pushVec2(4)

    // data.positions[m_indexA].c = cA;
    data.positions!![m_indexA].a = aA
    // data.positions[m_indexB].c = cB;
    data.positions!![m_indexB].a = aB
    return length - m_maxLength < Settings.linearSlop
  }

  override fun getAnchorA(out: Vec2) {
    m_bodyA!!.getWorldPointToOut(m_localAnchorA, out)
  }

  override fun getAnchorB(out: Vec2) {
    m_bodyB!!.getWorldPointToOut(m_localAnchorB, out)
  }

  override fun getReactionForce(inv_dt: Float, out: Vec2) {
    out.set(m_u).mulLocal(inv_dt).mulLocal(m_impulse)
  }

  override fun getReactionTorque(inv_dt: Float): Float {
    return 0f
  }

  fun getLocalAnchorA(): Vec2 {
    return m_localAnchorA
  }

  fun getLocalAnchorB(): Vec2 {
    return m_localAnchorB
  }

  fun getMaxLength(): Float {
    return m_maxLength
  }

  fun setMaxLength(maxLength: Float) {
    m_maxLength = maxLength
  }

  fun getLimitState(): LimitState {
    return m_state
  }
}
