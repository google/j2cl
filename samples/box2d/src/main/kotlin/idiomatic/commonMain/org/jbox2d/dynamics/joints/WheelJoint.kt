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

// Linear constraint (point-to-line)
// d = pB - pA = xB + rB - xA - rA
// C = dot(ay, d)
// Cdot = dot(d, cross(wA, ay)) + dot(ay, vB + cross(wB, rB) - vA - cross(wA, rA))
//   = -dot(ay, vA) - dot(cross(d + rA, ay), wA) + dot(ay, vB) + dot(cross(rB, ay), vB)
// J = [-ay, -cross(d + rA, ay), ay, cross(rB, ay)]
// Spring linear constraint
// C = dot(ax, d)
// Cdot = = -dot(ax, vA) - dot(cross(d + rA, ax), wA) + dot(ax, vB) + dot(cross(rB, ax), vB)
// J = [-ax -cross(d+rA, ax) ax cross(rB, ax)]
// Motor rotational constraint
// Cdot = wB - wA
// J = [0 0 -1 0 0 1]
/**
 * A wheel joint. This joint provides two degrees of freedom: translation along an axis fixed in
 * bodyA and rotation in the plane. You can use a joint limit to restrict the range of motion and a
 * joint motor to drive the rotation or to model rotational friction. This joint is designed for
 * vehicle suspensions.
 *
 * @author Daniel Murphy
 */
class WheelJoint(argPool: IWorldPool, def: WheelJointDef) : Joint(argPool, def) {
  var frequencyHz: Float = def.frequencyHz
  var dampingRatio: Float = def.dampingRatio

  // Solver shared
  val localAnchorA = Vec2().apply { set(def.localAnchorA) }
  val localAnchorB = Vec2().apply { set(def.localAnchorB) }
  val localXAxisA = Vec2().apply { set(def.localAxisA) }
  var maxMotorTorque: Float = def.maxMotorTorque
    private set
  var motorSpeed: Float = def.motorSpeed
    private set
  var enableMotor: Boolean = def.enableMotor
    private set

  private val localYAxisA = Vec2().apply { Vec2.crossToOutUnsafe(1.0f, localXAxisA, this) }
  private var impulse = 0f
  private var motorImpulse: Float = 0.0f
  private var springImpulse = 0f

  // Solver temp
  private var indexA = 0
  private var indexB = 0
  private val localCenterA = Vec2()
  private val localCenterB = Vec2()
  private var invMassA = 0f
  private var invMassB = 0f
  private var invIA = 0f
  private var invIB = 0f
  private val ax = Vec2()
  private val ay = Vec2()
  private var sAx = 0f
  private var sBx = 0f
  private var sAy = 0f
  private var sBy = 0f
  private var mass = 0f
  private var motorMass: Float = 0.0f
  private var springMass = 0f
  private var bias = 0f
  private var gamma = 0f

  // pooling
  private val rA = Vec2()
  private val rB = Vec2()
  private val d = Vec2()

  override fun getAnchorA(out: Vec2) {
    bodyA.getWorldPointToOut(localAnchorA, out)
  }

  override fun getAnchorB(out: Vec2) {
    bodyB.getWorldPointToOut(localAnchorB, out)
  }

  override fun getReactionForce(inv_dt: Float, out: Vec2) {
    val temp = pool.popVec2()
    temp.set(ay).mulLocal(impulse)
    out.set(ax).mulLocal(springImpulse).addLocal(temp).mulLocal(inv_dt)
    pool.pushVec2(1)
  }

  override fun getReactionTorque(inv_dt: Float): Float = inv_dt * motorImpulse

  fun getJointTranslation(): Float {
    val b1 = bodyA
    val b2 = bodyB
    val p1 = pool.popVec2()
    val p2 = pool.popVec2()
    val axis = pool.popVec2()
    b1.getWorldPointToOut(localAnchorA, p1)
    b2.getWorldPointToOut(localAnchorA, p2)
    p2.subLocal(p1)
    b1.getWorldVectorToOut(localXAxisA, axis)
    val translation = Vec2.dot(p2, axis)
    pool.pushVec2(3)
    return translation
  }

  fun getJointSpeed(): Float = bodyA.angularVelocity - bodyB.angularVelocity

  fun enableMotor(flag: Boolean) {
    bodyA.setAwake(true)
    bodyB.setAwake(true)
    enableMotor = flag
  }

  fun setMotorSpeed(speed: Float) {
    bodyA.setAwake(true)
    bodyB.setAwake(true)
    motorSpeed = speed
  }

  fun setMaxMotorTorque(torque: Float) {
    bodyA.setAwake(true)
    bodyB.setAwake(true)
    maxMotorTorque = torque
  }

  fun getMotorTorque(inv_dt: Float): Float = motorImpulse * inv_dt

  override fun initVelocityConstraints(data: SolverData) {
    indexA = bodyA.islandIndex
    indexB = bodyB.islandIndex
    localCenterA.set(bodyA.sweep.localCenter)
    localCenterB.set(bodyB.sweep.localCenter)
    invMassA = bodyA.invMass
    invMassB = bodyB.invMass
    invIA = bodyA.invI
    invIB = bodyB.invI
    val mA = invMassA
    val mB = invMassB
    val iA = invIA
    val iB = invIB
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
    d.set(cB).addLocal(rB).subLocal(cA).subLocal(rA)

    // Point to line constraint
    run {
      Rot.mulToOut(qA, localYAxisA, ay)
      sAy = Vec2.cross(temp.set(d).addLocal(rA), ay)
      sBy = Vec2.cross(rB, ay)
      mass = mA + mB + iA * sAy * sAy + iB * sBy * sBy
      if (mass > 0.0f) {
        mass = 1.0f / mass
      }
    }

    // Spring constraint
    springMass = 0.0f
    bias = 0.0f
    gamma = 0.0f
    if (frequencyHz > 0.0f) {
      Rot.mulToOut(qA, localXAxisA, ax)
      sAx = Vec2.cross(temp.set(d).addLocal(rA), ax)
      sBx = Vec2.cross(rB, ax)
      val invMass = mA + mB + iA * sAx * sAx + iB * sBx * sBx
      if (invMass > 0.0f) {
        springMass = 1.0f / invMass
        val C = Vec2.dot(d, ax)

        // Frequency
        val omega = 2.0f * MathUtils.PI * frequencyHz

        // Damping coefficient
        val d = 2.0f * springMass * dampingRatio * omega

        // Spring stiffness
        val k = springMass * omega * omega

        // magic formulas
        val h = data.step.dt
        gamma = h * (d + h * k)
        if (gamma > 0.0f) {
          gamma = 1.0f / gamma
        }
        bias = C * h * k * gamma
        springMass = invMass + gamma
        if (springMass > 0.0f) {
          springMass = 1.0f / springMass
        }
      }
    } else {
      springImpulse = 0.0f
    }

    // Rotational motor
    if (enableMotor) {
      motorMass = iA + iB
      if (motorMass > 0.0f) {
        motorMass = 1.0f / motorMass
      }
    } else {
      motorMass = 0.0f
      motorImpulse = 0.0f
    }
    if (data.step.warmStarting) {
      val P = pool.popVec2()
      // Account for variable time step.
      impulse *= data.step.dtRatio
      springImpulse *= data.step.dtRatio
      motorImpulse *= data.step.dtRatio
      P.x = impulse * ay.x + springImpulse * ax.x
      P.y = impulse * ay.y + springImpulse * ax.y
      val LA = impulse * sAy + springImpulse * sAx + motorImpulse
      val LB = impulse * sBy + springImpulse * sBx + motorImpulse
      vA.x -= invMassA * P.x
      vA.y -= invMassA * P.y
      wA -= invIA * LA
      vB.x += invMassB * P.x
      vB.y += invMassB * P.y
      wB += invIB * LB
      pool.pushVec2(1)
    } else {
      impulse = 0.0f
      springImpulse = 0.0f
      motorImpulse = 0.0f
    }
    pool.pushRot(2)
    pool.pushVec2(1)

    // data.velocities[m_indexA].v = vA;
    data.velocities[indexA].w = wA
    // data.velocities[m_indexB].v = vB;
    data.velocities[indexB].w = wB
  }

  override fun solveVelocityConstraints(data: SolverData) {
    val mA = invMassA
    val mB = invMassB
    val iA = invIA
    val iB = invIB
    val vA = data.velocities[indexA].v
    var wA = data.velocities[indexA].w
    val vB = data.velocities[indexB].v
    var wB = data.velocities[indexB].w
    val temp = pool.popVec2()
    val P = pool.popVec2()

    // Solve spring constraint
    run {
      val Cdot = Vec2.dot(ax, temp.set(vB).subLocal(vA)) + sBx * wB - sAx * wA
      val impulse = -springMass * (Cdot + bias + gamma * springImpulse)
      springImpulse += impulse
      P.x = impulse * ax.x
      P.y = impulse * ax.y
      val LA = impulse * sAx
      val LB = impulse * sBx
      vA.x -= mA * P.x
      vA.y -= mA * P.y
      wA -= iA * LA
      vB.x += mB * P.x
      vB.y += mB * P.y
      wB += iB * LB
    }

    // Solve rotational motor constraint
    run {
      val Cdot = wB - wA - motorSpeed
      var impulse = -motorMass * Cdot
      val oldImpulse = motorImpulse
      val maxImpulse = data.step.dt * maxMotorTorque
      motorImpulse = MathUtils.clamp(motorImpulse + impulse, -maxImpulse, maxImpulse)
      impulse = motorImpulse - oldImpulse
      wA -= iA * impulse
      wB += iB * impulse
    }

    // Solve point to line constraint
    run {
      val Cdot = Vec2.dot(ay, temp.set(vB).subLocal(vA)) + sBy * wB - sAy * wA
      val localImpulse = -mass * Cdot
      impulse += localImpulse
      P.x = localImpulse * ay.x
      P.y = localImpulse * ay.y
      val LA = localImpulse * sAy
      val LB = localImpulse * sBy
      vA.x -= mA * P.x
      vA.y -= mA * P.y
      wA -= iA * LA
      vB.x += mB * P.x
      vB.y += mB * P.y
      wB += iB * LB
    }
    pool.pushVec2(2)

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
    val temp = pool.popVec2()
    qA.set(aA)
    qB.set(aB)
    Rot.mulToOut(qA, temp.set(localAnchorA).subLocal(localCenterA), rA)
    Rot.mulToOut(qB, temp.set(localAnchorB).subLocal(localCenterB), rB)
    d.set(cB).subLocal(cA).addLocal(rB).subLocal(rA)
    val ay = pool.popVec2()
    Rot.mulToOut(qA, localYAxisA, ay)
    val localSAy = Vec2.cross(temp.set(d).addLocal(rA), ay)
    val localSBy = Vec2.cross(rB, ay)
    val C = Vec2.dot(d, ay)
    val k = invMassA + invMassB + invIA * sAy * sAy + invIB * sBy * sBy
    val impulse: Float =
      if (k != 0.0f) {
        -C / k
      } else {
        0.0f
      }
    val P = pool.popVec2()
    P.x = impulse * ay.x
    P.y = impulse * ay.y
    val LA = impulse * localSAy
    val LB = impulse * localSBy
    cA.x -= invMassA * P.x
    cA.y -= invMassA * P.y
    aA -= invIA * LA
    cB.x += invMassB * P.x
    cB.y += invMassB * P.y
    aB += invIB * LB
    pool.pushVec2(3)
    pool.pushRot(2)
    // data.positions[m_indexA].c = cA;
    data.positions[indexA].a = aA
    // data.positions[m_indexB].c = cB;
    data.positions[indexB].a = aB
    return MathUtils.abs(C) <= Settings.LINEAR_SLOP
  }
}
