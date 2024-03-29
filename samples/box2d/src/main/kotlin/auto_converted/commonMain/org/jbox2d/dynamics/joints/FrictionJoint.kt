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
/** Created at 7:27:32 AM Jan 20, 2011 */
package org.jbox2d.dynamics.joints

import org.jbox2d.common.Mat22
import org.jbox2d.common.MathUtils
import org.jbox2d.common.Rot
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.SolverData
import org.jbox2d.pooling.IWorldPool

/** @author Daniel Murphy */
class FrictionJoint(argWorldPool: IWorldPool, def: FrictionJointDef) : Joint(argWorldPool, def) {
  private val m_localAnchorA: Vec2
  private val m_localAnchorB: Vec2

  // Solver shared
  private val m_linearImpulse: Vec2
  private var m_angularImpulse: Float
  private var m_maxForce: Float
  private var m_maxTorque: Float

  // Solver temp
  private var m_indexA = 0
  private var m_indexB = 0
  private val m_rA = Vec2()
  private val m_rB = Vec2()
  private val m_localCenterA = Vec2()
  private val m_localCenterB = Vec2()
  private var m_invMassA = 0f
  private var m_invMassB = 0f
  private var m_invIA = 0f
  private var m_invIB = 0f
  private val m_linearMass = Mat22()
  private var m_angularMass = 0f

  init {
    m_localAnchorA = Vec2(def.localAnchorA)
    m_localAnchorB = Vec2(def.localAnchorB)
    m_linearImpulse = Vec2()
    m_angularImpulse = 0.0f
    m_maxForce = def.maxForce
    m_maxTorque = def.maxTorque
  }

  fun getLocalAnchorA(): Vec2 {
    return m_localAnchorA
  }

  fun getLocalAnchorB(): Vec2 {
    return m_localAnchorB
  }

  override fun getAnchorA(out: Vec2) {
    m_bodyA!!.getWorldPointToOut(m_localAnchorA, out)
  }

  override fun getAnchorB(out: Vec2) {
    m_bodyB!!.getWorldPointToOut(m_localAnchorB, out)
  }

  override fun getReactionForce(inv_dt: Float, out: Vec2) {
    out.set(m_linearImpulse).mulLocal(inv_dt)
  }

  override fun getReactionTorque(inv_dt: Float): Float {
    return inv_dt * m_angularImpulse
  }

  fun setMaxForce(force: Float) {
    // assert is not supported in KMP.
    // assert(force >= 0.0f)
    m_maxForce = force
  }

  fun getMaxForce(): Float {
    return m_maxForce
  }

  fun setMaxTorque(torque: Float) {
    // assert is not supported in KMP.
    // assert(torque >= 0.0f)
    m_maxTorque = torque
  }

  fun getMaxTorque(): Float {
    return m_maxTorque
  }

  /** @see org.jbox2d.dynamics.joints.Joint.initVelocityConstraints */
  override fun initVelocityConstraints(data: SolverData) {
    m_indexA = m_bodyA!!.m_islandIndex
    m_indexB = m_bodyB!!.m_islandIndex
    m_localCenterA.set(m_bodyA!!.m_sweep.localCenter)
    m_localCenterB.set(m_bodyB!!.m_sweep.localCenter)
    m_invMassA = m_bodyA!!.m_invMass
    m_invMassB = m_bodyB!!.m_invMass
    m_invIA = m_bodyA!!.m_invI
    m_invIB = m_bodyB!!.m_invI
    val aA = data.positions!![m_indexA].a
    val vA = data.velocities!![m_indexA].v
    var wA = data.velocities!![m_indexA].w
    val aB = data.positions!![m_indexB].a
    val vB = data.velocities!![m_indexB].v
    var wB = data.velocities!![m_indexB].w
    val temp = pool.popVec2()
    val qA = pool.popRot()
    val qB = pool.popRot()
    qA.set(aA)
    qB.set(aB)

    // Compute the effective mass matrix.
    Rot.mulToOutUnsafe(qA, temp.set(m_localAnchorA).subLocal(m_localCenterA), m_rA)
    Rot.mulToOutUnsafe(qB, temp.set(m_localAnchorB).subLocal(m_localCenterB), m_rB)

    // J = [-I -r1_skew I r2_skew]
    // [ 0 -1 0 1]
    // r_skew = [-ry; rx]

    // Matlab
    // K = [ mA+r1y^2*iA+mB+r2y^2*iB, -r1y*iA*r1x-r2y*iB*r2x, -r1y*iA-r2y*iB]
    // [ -r1y*iA*r1x-r2y*iB*r2x, mA+r1x^2*iA+mB+r2x^2*iB, r1x*iA+r2x*iB]
    // [ -r1y*iA-r2y*iB, r1x*iA+r2x*iB, iA+iB]
    val mA = m_invMassA
    val mB = m_invMassB
    val iA = m_invIA
    val iB = m_invIB
    val K = pool.popMat22()
    K.ex.x = mA + mB + iA * m_rA.y * m_rA.y + iB * m_rB.y * m_rB.y
    K.ex.y = -iA * m_rA.x * m_rA.y - iB * m_rB.x * m_rB.y
    K.ey.x = K.ex.y
    K.ey.y = mA + mB + iA * m_rA.x * m_rA.x + iB * m_rB.x * m_rB.x
    K.invertToOut(m_linearMass)
    m_angularMass = iA + iB
    if (m_angularMass > 0.0f) {
      m_angularMass = 1.0f / m_angularMass
    }
    if (data.step!!.warmStarting) {
      // Scale impulses to support a variable time step.
      m_linearImpulse.mulLocal(data.step!!.dtRatio)
      m_angularImpulse *= data.step!!.dtRatio
      val P = pool.popVec2()
      P.set(m_linearImpulse)
      temp.set(P).mulLocal(mA)
      vA.subLocal(temp)
      wA -= iA * (Vec2.cross(m_rA, P) + m_angularImpulse)
      temp.set(P).mulLocal(mB)
      vB.addLocal(temp)
      wB += iB * (Vec2.cross(m_rB, P) + m_angularImpulse)
      pool.pushVec2(1)
    } else {
      m_linearImpulse.setZero()
      m_angularImpulse = 0.0f
    }
    //    data.velocities[m_indexA].v.set(vA);
    if (data.velocities!![m_indexA].w != wA) {
      // assert is not supported in KMP.
      // assert(data.velocities!![m_indexA].w != wA)
    }
    data.velocities!![m_indexA].w = wA
    //    data.velocities[m_indexB].v.set(vB);
    data.velocities!![m_indexB].w = wB
    pool.pushRot(2)
    pool.pushVec2(1)
    pool.pushMat22(1)
  }

  override fun solveVelocityConstraints(data: SolverData) {
    val vA = data.velocities!![m_indexA].v
    var wA = data.velocities!![m_indexA].w
    val vB = data.velocities!![m_indexB].v
    var wB = data.velocities!![m_indexB].w
    val mA = m_invMassA
    val mB = m_invMassB
    val iA = m_invIA
    val iB = m_invIB
    val h = data.step!!.dt

    // Solve angular friction
    run {
      val Cdot = wB - wA
      var impulse = -m_angularMass * Cdot
      val oldImpulse = m_angularImpulse
      val maxImpulse = h * m_maxTorque
      m_angularImpulse = MathUtils.clamp(m_angularImpulse + impulse, -maxImpulse, maxImpulse)
      impulse = m_angularImpulse - oldImpulse
      wA -= iA * impulse
      wB += iB * impulse
    }

    // Solve linear friction
    run {
      val Cdot = pool.popVec2()
      val temp = pool.popVec2()
      Vec2.crossToOutUnsafe(wA, m_rA, temp)
      Vec2.crossToOutUnsafe(wB, m_rB, Cdot)
      Cdot.addLocal(vB).subLocal(vA).subLocal(temp)
      val impulse = pool.popVec2()
      Mat22.mulToOutUnsafe(m_linearMass, Cdot, impulse)
      impulse.negateLocal()
      val oldImpulse = pool.popVec2()
      oldImpulse.set(m_linearImpulse)
      m_linearImpulse.addLocal(impulse)
      val maxImpulse = h * m_maxForce
      if (m_linearImpulse.lengthSquared() > maxImpulse * maxImpulse) {
        m_linearImpulse.normalize()
        m_linearImpulse.mulLocal(maxImpulse)
      }
      impulse.set(m_linearImpulse).subLocal(oldImpulse)
      temp.set(impulse).mulLocal(mA)
      vA.subLocal(temp)
      wA -= iA * Vec2.cross(m_rA, impulse)
      temp.set(impulse).mulLocal(mB)
      vB.addLocal(temp)
      wB += iB * Vec2.cross(m_rB, impulse)
    }

    //    data.velocities[m_indexA].v.set(vA);
    if (data.velocities!![m_indexA].w != wA) {
      // assert is not supported in KMP.
      // assert(data.velocities!![m_indexA].w != wA)
    }
    data.velocities!![m_indexA].w = wA

    //    data.velocities[m_indexB].v.set(vB);
    data.velocities!![m_indexB].w = wB
    pool.pushVec2(4)
  }

  override fun solvePositionConstraints(data: SolverData): Boolean {
    return true
  }
}
