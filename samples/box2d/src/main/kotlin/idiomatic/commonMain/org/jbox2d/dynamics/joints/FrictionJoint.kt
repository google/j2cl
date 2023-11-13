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
  val localAnchorA: Vec2 = Vec2(def.localAnchorA)
  val localAnchorB: Vec2 = Vec2(def.localAnchorB)
  var maxForce: Float = def.maxForce
  var maxTorque: Float = def.maxTorque

  // Solver shared
  private val linearImpulse: Vec2 = Vec2()
  private var angularImpulse: Float = 0.0f

  // Solver temp
  private var indexA = 0
  private var indexB = 0
  private val rA = Vec2()
  private val rB = Vec2()
  private val localCenterA = Vec2()
  private val localCenterB = Vec2()
  private var invMassA = 0f
  private var invMassB = 0f
  private var invIA = 0f
  private var invIB = 0f
  private val linearMass = Mat22()
  private var angularMass = 0f

  override fun getAnchorA(out: Vec2) {
    bodyA.getWorldPointToOut(localAnchorA, out)
  }

  override fun getAnchorB(out: Vec2) {
    bodyB.getWorldPointToOut(localAnchorB, out)
  }

  override fun getReactionForce(inv_dt: Float, out: Vec2) {
    out.set(linearImpulse).mulLocal(inv_dt)
  }

  override fun getReactionTorque(inv_dt: Float): Float = inv_dt * angularImpulse

  /** @see org.jbox2d.dynamics.joints.Joint.initVelocityConstraints */
  override fun initVelocityConstraints(data: SolverData) {
    indexA = bodyA.islandIndex
    indexB = bodyB.islandIndex
    localCenterA.set(bodyA.sweep.localCenter)
    localCenterB.set(bodyB.sweep.localCenter)
    invMassA = bodyA.invMass
    invMassB = bodyB.invMass
    invIA = bodyA.invI
    invIB = bodyB.invI
    val aA = data.positions[indexA].a
    val vA = data.velocities[indexA].v
    var wA = data.velocities[indexA].w
    val aB = data.positions[indexB].a
    val vB = data.velocities[indexB].v
    var wB = data.velocities[indexB].w
    val temp = pool.popVec2()
    val qA = pool.popRot()
    val qB = pool.popRot()
    qA.set(aA)
    qB.set(aB)

    // Compute the effective mass matrix.
    Rot.mulToOutUnsafe(qA, temp.set(localAnchorA).subLocal(localCenterA), rA)
    Rot.mulToOutUnsafe(qB, temp.set(localAnchorB).subLocal(localCenterB), rB)

    // J = [-I -r1_skew I r2_skew]
    // [ 0 -1 0 1]
    // r_skew = [-ry; rx]

    // Matlab
    // K = [ mA+r1y^2*iA+mB+r2y^2*iB, -r1y*iA*r1x-r2y*iB*r2x, -r1y*iA-r2y*iB]
    // [ -r1y*iA*r1x-r2y*iB*r2x, mA+r1x^2*iA+mB+r2x^2*iB, r1x*iA+r2x*iB]
    // [ -r1y*iA-r2y*iB, r1x*iA+r2x*iB, iA+iB]
    val mA = invMassA
    val mB = invMassB
    val iA = invIA
    val iB = invIB
    val K = pool.popMat22()
    K.ex.x = mA + mB + iA * rA.y * rA.y + iB * rB.y * rB.y
    K.ex.y = -iA * rA.x * rA.y - iB * rB.x * rB.y
    K.ey.x = K.ex.y
    K.ey.y = mA + mB + iA * rA.x * rA.x + iB * rB.x * rB.x
    K.invertToOut(linearMass)
    angularMass = iA + iB
    if (angularMass > 0.0f) {
      angularMass = 1.0f / angularMass
    }
    if (data.step.warmStarting) {
      // Scale impulses to support a variable time step.
      linearImpulse.mulLocal(data.step.dtRatio)
      angularImpulse *= data.step.dtRatio
      val P = pool.popVec2()
      P.set(linearImpulse)
      temp.set(P).mulLocal(mA)
      vA.subLocal(temp)
      wA -= iA * (Vec2.cross(rA, P) + angularImpulse)
      temp.set(P).mulLocal(mB)
      vB.addLocal(temp)
      wB += iB * (Vec2.cross(rB, P) + angularImpulse)
      pool.pushVec2(1)
    } else {
      linearImpulse.setZero()
      angularImpulse = 0.0f
    }
    //    data.velocities[m_indexA].v.set(vA);
    if (data.velocities[indexA].w != wA) {
      // assert is not supported in KMP.
      // assert(data.velocities!![m_indexA].w != wA)
    }
    data.velocities[indexA].w = wA
    //    data.velocities[m_indexB].v.set(vB);
    data.velocities[indexB].w = wB
    pool.pushRot(2)
    pool.pushVec2(1)
    pool.pushMat22(1)
  }

  override fun solveVelocityConstraints(data: SolverData) {
    val vA = data.velocities[indexA].v
    var wA = data.velocities[indexA].w
    val vB = data.velocities[indexB].v
    var wB = data.velocities[indexB].w
    val mA = invMassA
    val mB = invMassB
    val iA = invIA
    val iB = invIB
    val h = data.step.dt

    // Solve angular friction
    run {
      val Cdot = wB - wA
      var impulse = -angularMass * Cdot
      val oldImpulse = angularImpulse
      val maxImpulse = h * maxTorque
      angularImpulse = MathUtils.clamp(angularImpulse + impulse, -maxImpulse, maxImpulse)
      impulse = angularImpulse - oldImpulse
      wA -= iA * impulse
      wB += iB * impulse
    }

    // Solve linear friction
    run {
      val Cdot = pool.popVec2()
      val temp = pool.popVec2()
      Vec2.crossToOutUnsafe(wA, rA, temp)
      Vec2.crossToOutUnsafe(wB, rB, Cdot)
      Cdot.addLocal(vB).subLocal(vA).subLocal(temp)
      val impulse = pool.popVec2()
      Mat22.mulToOutUnsafe(linearMass, Cdot, impulse)
      impulse.negateLocal()
      val oldImpulse = pool.popVec2()
      oldImpulse.set(linearImpulse)
      linearImpulse.addLocal(impulse)
      val maxImpulse = h * maxForce
      if (linearImpulse.lengthSquared() > maxImpulse * maxImpulse) {
        linearImpulse.normalize()
        linearImpulse.mulLocal(maxImpulse)
      }
      impulse.set(linearImpulse).subLocal(oldImpulse)
      temp.set(impulse).mulLocal(mA)
      vA.subLocal(temp)
      wA -= iA * Vec2.cross(rA, impulse)
      temp.set(impulse).mulLocal(mB)
      vB.addLocal(temp)
      wB += iB * Vec2.cross(rB, impulse)
    }

    //    data.velocities[m_indexA].v.set(vA);
    if (data.velocities[indexA].w != wA) {
      // assert is not supported in KMP.
      // assert(data.velocities!![m_indexA].w != wA)
    }
    data.velocities[indexA].w = wA

    //    data.velocities[m_indexB].v.set(vB);
    data.velocities[indexB].w = wB
    pool.pushVec2(4)
  }

  override fun solvePositionConstraints(data: SolverData): Boolean = true
}
