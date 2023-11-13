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
/** Created at 12:12:02 PM Jan 23, 2011 */
package org.jbox2d.dynamics.joints

import org.jbox2d.common.MathUtils
import org.jbox2d.common.Rot
import org.jbox2d.common.Settings
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.SolverData
import org.jbox2d.pooling.IWorldPool

/**
 * The pulley joint is connected to two bodies and two fixed ground points. The pulley supports a
 * ratio such that: length1 + ratio * length2 <= constant Yes, the force transmitted is scaled by
 * the ratio. Warning: the pulley joint can get a bit squirrelly by itself. They often work better
 * when combined with prismatic joints. You should also cover the the anchor points with static
 * shapes to prevent one side from going to zero length.
 *
 * @author Daniel Murphy
 */
class PulleyJoint(argWorldPool: IWorldPool, def: PulleyJointDef) : Joint(argWorldPool, def) {
  val groundAnchorA = Vec2().apply { set(def.groundAnchorA) }
  val groundAnchorB = Vec2().apply { set(def.groundAnchorB) }
  val lengthA: Float = def.lengthA
  val lengthB: Float = def.lengthB

  // Solver shared
  val localAnchorA = Vec2().apply { set(def.localAnchorA) }
  val localAnchorB = Vec2().apply { set(def.localAnchorB) }
  val ratio: Float = def.ratio

  private val constant: Float = def.lengthA + ratio * def.lengthB
  private var impulse: Float = 0.0f

  // Solver temp
  private var indexA = 0
  private var indexB = 0
  private val uA = Vec2()
  private val uB = Vec2()
  private val rA = Vec2()
  private val rB = Vec2()
  private val localCenterA = Vec2()
  private val localCenterB = Vec2()
  private var invMassA = 0f
  private var invMassB = 0f
  private var invIA = 0f
  private var invIB = 0f
  private var mass = 0f

  fun getCurrentLengthA(): Float {
    val p = pool.popVec2()
    bodyA.getWorldPointToOut(localAnchorA, p)
    p.subLocal(groundAnchorA)
    val length = p.length()
    pool.pushVec2(1)
    return length
  }

  fun getCurrentLengthB(): Float {
    val p = pool.popVec2()
    bodyB.getWorldPointToOut(localAnchorB, p)
    p.subLocal(groundAnchorB)
    val length = p.length()
    pool.pushVec2(1)
    return length
  }

  override fun getAnchorA(out: Vec2) {
    bodyA.getWorldPointToOut(localAnchorA, out)
  }

  override fun getAnchorB(out: Vec2) {
    bodyB.getWorldPointToOut(localAnchorB, out)
  }

  override fun getReactionForce(inv_dt: Float, out: Vec2) {
    out.set(uB).mulLocal(impulse).mulLocal(inv_dt)
  }

  override fun getReactionTorque(inv_dt: Float): Float = 0f

  fun getLength1(): Float {
    val p = pool.popVec2()
    bodyA.getWorldPointToOut(localAnchorA, p)
    p.subLocal(groundAnchorA)
    val len = p.length()
    pool.pushVec2(1)
    return len
  }

  fun getLength2(): Float {
    val p = pool.popVec2()
    bodyB.getWorldPointToOut(localAnchorB, p)
    p.subLocal(groundAnchorB)
    val len = p.length()
    pool.pushVec2(1)
    return len
  }

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
    uA.set(cA).addLocal(rA).subLocal(groundAnchorA)
    uB.set(cB).addLocal(rB).subLocal(groundAnchorB)
    val lengthA = uA.length()
    val lengthB = uB.length()
    if (lengthA > 10f * Settings.LINEAR_SLOP) {
      uA.mulLocal(1.0f / lengthA)
    } else {
      uA.setZero()
    }
    if (lengthB > 10f * Settings.LINEAR_SLOP) {
      uB.mulLocal(1.0f / lengthB)
    } else {
      uB.setZero()
    }

    // Compute effective mass.
    val ruA = Vec2.cross(rA, uA)
    val ruB = Vec2.cross(rB, uB)
    val mA = invMassA + invIA * ruA * ruA
    val mB = invMassB + invIB * ruB * ruB
    mass = mA + ratio * ratio * mB
    if (mass > 0.0f) {
      mass = 1.0f / mass
    }
    if (data.step.warmStarting) {

      // Scale impulses to support variable time steps.
      impulse *= data.step.dtRatio

      // Warm starting.
      val PA = pool.popVec2()
      val PB = pool.popVec2()
      PA.set(uA).mulLocal(-impulse)
      PB.set(uB).mulLocal(-ratio * impulse)
      vA.x += invMassA * PA.x
      vA.y += invMassA * PA.y
      wA += invIA * Vec2.cross(rA, PA)
      vB.x += invMassB * PB.x
      vB.y += invMassB * PB.y
      wB += invIB * Vec2.cross(rB, PB)
      pool.pushVec2(2)
    } else {
      impulse = 0.0f
    }
    //    data.velocities[m_indexA].v.set(vA);
    data.velocities[indexA].w = wA
    //    data.velocities[m_indexB].v.set(vB);
    data.velocities[indexB].w = wB
    pool.pushVec2(1)
    pool.pushRot(2)
  }

  override fun solveVelocityConstraints(data: SolverData) {
    val vA = data.velocities[indexA].v
    var wA = data.velocities[indexA].w
    val vB = data.velocities[indexB].v
    var wB = data.velocities[indexB].w
    val vpA = pool.popVec2()
    val vpB = pool.popVec2()
    val PA = pool.popVec2()
    val PB = pool.popVec2()
    Vec2.crossToOutUnsafe(wA, rA, vpA)
    vpA.addLocal(vA)
    Vec2.crossToOutUnsafe(wB, rB, vpB)
    vpB.addLocal(vB)
    val Cdot = -Vec2.dot(uA, vpA) - ratio * Vec2.dot(uB, vpB)
    val localImpulse = -mass * Cdot
    impulse += localImpulse
    PA.set(uA).mulLocal(-localImpulse)
    PB.set(uB).mulLocal(-ratio * localImpulse)
    vA.x += invMassA * PA.x
    vA.y += invMassA * PA.y
    wA += invIA * Vec2.cross(rA, PA)
    vB.x += invMassB * PB.x
    vB.y += invMassB * PB.y
    wB += invIB * Vec2.cross(rB, PB)

    //    data.velocities[m_indexA].v.set(vA);
    data.velocities[indexA].w = wA
    //    data.velocities[m_indexB].v.set(vB);
    data.velocities[indexB].w = wB
    pool.pushVec2(4)
  }

  override fun solvePositionConstraints(data: SolverData): Boolean {
    val qA = pool.popRot()
    val qB = pool.popRot()
    val rA = pool.popVec2()
    val rB = pool.popVec2()
    val uA = pool.popVec2()
    val uB = pool.popVec2()
    val temp = pool.popVec2()
    val PA = pool.popVec2()
    val PB = pool.popVec2()
    val cA = data.positions[indexA].c
    var aA = data.positions[indexA].a
    val cB = data.positions[indexB].c
    var aB = data.positions[indexB].a
    qA.set(aA)
    qB.set(aB)
    Rot.mulToOutUnsafe(qA, temp.set(localAnchorA).subLocal(localCenterA), rA)
    Rot.mulToOutUnsafe(qB, temp.set(localAnchorB).subLocal(localCenterB), rB)
    uA.set(cA).addLocal(rA).subLocal(groundAnchorA)
    uB.set(cB).addLocal(rB).subLocal(groundAnchorB)
    val lengthA = uA.length()
    val lengthB = uB.length()
    if (lengthA > 10.0f * Settings.LINEAR_SLOP) {
      uA.mulLocal(1.0f / lengthA)
    } else {
      uA.setZero()
    }
    if (lengthB > 10.0f * Settings.LINEAR_SLOP) {
      uB.mulLocal(1.0f / lengthB)
    } else {
      uB.setZero()
    }

    // Compute effective mass.
    val ruA = Vec2.cross(rA, uA)
    val ruB = Vec2.cross(rB, uB)
    val mA = invMassA + invIA * ruA * ruA
    val mB = invMassB + invIB * ruB * ruB
    var mass = mA + ratio * ratio * mB
    if (mass > 0.0f) {
      mass = 1.0f / mass
    }
    val C = constant - lengthA - ratio * lengthB
    val linearError = MathUtils.abs(C)
    val impulse = -mass * C
    PA.set(uA).mulLocal(-impulse)
    PB.set(uB).mulLocal(-ratio * impulse)
    cA.x += invMassA * PA.x
    cA.y += invMassA * PA.y
    aA += invIA * Vec2.cross(rA, PA)
    cB.x += invMassB * PB.x
    cB.y += invMassB * PB.y
    aB += invIB * Vec2.cross(rB, PB)

    //    data.positions[m_indexA].c.set(cA);
    data.positions[indexA].a = aA
    //    data.positions[m_indexB].c.set(cB);
    data.positions[indexB].a = aB
    pool.pushRot(2)
    pool.pushVec2(7)
    return linearError < Settings.LINEAR_SLOP
  }

  companion object {
    const val MIN_PULLEY_LENGTH = 2.0f
  }
}
