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

import org.jbox2d.common.Mat33
import org.jbox2d.common.MathUtils
import org.jbox2d.common.Rot
import org.jbox2d.common.Settings
import org.jbox2d.common.Vec2
import org.jbox2d.common.Vec3
import org.jbox2d.dynamics.SolverData
import org.jbox2d.pooling.IWorldPool

// Linear constraint (point-to-line)
// d = p2 - p1 = x2 + r2 - x1 - r1
// C = dot(perp, d)
// Cdot = dot(d, cross(w1, perp)) + dot(perp, v2 + cross(w2, r2) - v1 - cross(w1, r1))
//   = -dot(perp, v1) - dot(cross(d + r1, perp), w1) + dot(perp, v2) + dot(cross(r2, perp), v2)
// J = [-perp, -cross(d + r1, perp), perp, cross(r2,perp)]
//
// Angular constraint
// C = a2 - a1 + a_initial
// Cdot = w2 - w1
// J = [0 0 -1 0 0 1]
//
// K = J * invM * JT
//
// J = [-a -s1 a s2]
//  [0  -1  0  1]
// a = perp
// s1 = cross(d + r1, a) = cross(p2 - x1, a)
// s2 = cross(r2, a) = cross(p2 - x2, a)
// Motor/Limit linear constraint
// C = dot(ax1, d)
// Cdot = = -dot(ax1, v1) - dot(cross(d + r1, ax1), w1) + dot(ax1, v2) + dot(cross(r2, ax1), v2)
// J = [-ax1 -cross(d+r1,ax1) ax1 cross(r2,ax1)]
// Block Solver
// We develop a block solver that includes the joint limit. This makes the limit stiff (inelastic)
// even
// when the mass has poor distribution (leading to large torques about the joint anchor points).
//
// The Jacobian has 3 rows:
// J = [-uT -s1 uT s2] // linear
//  [0   -1   0  1] // angular
//  [-vT -a1 vT a2] // limit
//
// u = perp
// v = axis
// s1 = cross(d + r1, u), s2 = cross(r2, u)
// a1 = cross(d + r1, v), a2 = cross(r2, v)
// M * (v2 - v1) = JT * df
// J * v2 = bias
//
// v2 = v1 + invM * JT * df
// J * (v1 + invM * JT * df) = bias
// K * df = bias - J * v1 = -Cdot
// K = J * invM * JT
// Cdot = J * v1 - bias
//
// Now solve for f2.
// df = f2 - f1
// K * (f2 - f1) = -Cdot
// f2 = invK * (-Cdot) + f1
//
// Clamp accumulated limit impulse.
// lower: f2(3) = max(f2(3), 0)
// upper: f2(3) = min(f2(3), 0)
//
// Solve for correct f2(1:2)
// K(1:2, 1:2) * f2(1:2) = -Cdot(1:2) - K(1:2,3) * f2(3) + K(1:2,1:3) * f1
//                    = -Cdot(1:2) - K(1:2,3) * f2(3) + K(1:2,1:2) * f1(1:2) + K(1:2,3) * f1(3)
// K(1:2, 1:2) * f2(1:2) = -Cdot(1:2) - K(1:2,3) * (f2(3) - f1(3)) + K(1:2,1:2) * f1(1:2)
// f2(1:2) = invK(1:2,1:2) * (-Cdot(1:2) - K(1:2,3) * (f2(3) - f1(3))) + f1(1:2)
//
// Now compute impulse to be applied:
// df = f2 - f1
/**
 * A prismatic joint. This joint provides one degree of freedom: translation along an axis fixed in
 * bodyA. Relative rotation is prevented. You can use a joint limit to restrict the range of motion
 * and a joint motor to drive the motion or to model joint friction.
 *
 * @author Daniel
 */
class PrismaticJoint(argWorld: IWorldPool, def: PrismaticJointDef) : Joint(argWorld, def) {
  // Solver shared
  val localAnchorA: Vec2 = Vec2(def.localAnchorA)
  val localAnchorB: Vec2 = Vec2(def.localAnchorB)
  val localXAxisA: Vec2 = Vec2(def.localAxisA).apply { normalize() }
  var referenceAngle: Float = def.referenceAngle
  var lowerTranslation: Float = def.lowerTranslation
    private set
  var upperTranslation: Float = def.upperTranslation
    private set
  var maxMotorForce: Float = def.maxMotorForce
    private set
  var motorSpeed: Float = def.motorSpeed
    private set
  var enableLimit: Boolean = def.enableLimit
    private set
  var enableMotor: Boolean = def.enableMotor
    private set
  protected val localYAxisA: Vec2 = Vec2().apply { Vec2.crossToOutUnsafe(1f, localXAxisA, this) }
  private val impulse: Vec3 = Vec3()
  private var motorImpulse: Float = 0.0f
  private var limitState: LimitState = LimitState.INACTIVE

  // Solver temp
  private var indexA = 0
  private var indexB = 0
  private val localCenterA = Vec2()
  private val localCenterB = Vec2()
  private var invMassA = 0f
  private var invMassB = 0f
  private var invIA = 0f
  private var invIB = 0f
  private val axis: Vec2 = Vec2()
  private val perp: Vec2 = Vec2()
  private var s1 = 0f
  private var s2 = 0f
  private var a1 = 0f
  private var a2 = 0f
  private val K: Mat33 = Mat33()
  private var motorMass: Float = 0.0f // effective mass for motor/limit translational constraint.

  override fun getAnchorA(out: Vec2) {
    bodyA.getWorldPointToOut(localAnchorA, out)
  }

  override fun getAnchorB(out: Vec2) {
    bodyB.getWorldPointToOut(localAnchorB, out)
  }

  override fun getReactionForce(inv_dt: Float, out: Vec2) {
    val temp = pool.popVec2()
    temp.set(axis).mulLocal(motorImpulse + impulse.z)
    out.set(perp).mulLocal(impulse.x).addLocal(temp).mulLocal(inv_dt)
    pool.pushVec2(1)
  }

  override fun getReactionTorque(inv_dt: Float): Float = inv_dt * impulse.y

  /** Get the current joint translation, usually in meters. */
  fun getJointSpeed(): Float {
    val bA = bodyA
    val bB = bodyB
    val temp = pool.popVec2()
    val rA = pool.popVec2()
    val rB = pool.popVec2()
    val p1 = pool.popVec2()
    val p2 = pool.popVec2()
    val d = pool.popVec2()
    val axis = pool.popVec2()
    val temp2 = pool.popVec2()
    val temp3 = pool.popVec2()
    temp.set(localAnchorA).subLocal(bA.sweep.localCenter)
    Rot.mulToOutUnsafe(bA.xf.q, temp, rA)
    temp.set(localAnchorB).subLocal(bB.sweep.localCenter)
    Rot.mulToOutUnsafe(bB.xf.q, temp, rB)
    p1.set(bA.sweep.c).addLocal(rA)
    p2.set(bB.sweep.c).addLocal(rB)
    d.set(p2).subLocal(p1)
    Rot.mulToOutUnsafe(bA.xf.q, localXAxisA, axis)
    val vA = bA.linearVelocity
    val vB = bB.linearVelocity
    val wA = bA.angularVelocity
    val wB = bB.angularVelocity
    Vec2.crossToOutUnsafe(wA, axis, temp)
    Vec2.crossToOutUnsafe(wB, rB, temp2)
    Vec2.crossToOutUnsafe(wA, rA, temp3)
    temp2.addLocal(vB).subLocal(vA).subLocal(temp3)
    val speed = Vec2.dot(d, temp) + Vec2.dot(axis, temp2)
    pool.pushVec2(9)
    return speed
  }

  fun getJointTranslation(): Float {
    val pA = pool.popVec2()
    val pB = pool.popVec2()
    val axis = pool.popVec2()
    bodyA.getWorldPointToOut(localAnchorA, pA)
    bodyB.getWorldPointToOut(localAnchorB, pB)
    bodyA.getWorldVectorToOutUnsafe(localXAxisA, axis)
    pB.subLocal(pA)
    val translation = Vec2.dot(pB, axis)
    pool.pushVec2(3)
    return translation
  }

  /**
   * Enable/disable the joint limit.
   *
   * @param flag
   */
  fun enableLimit(flag: Boolean) {
    if (flag != enableLimit) {
      bodyA.setAwake(true)
      bodyB.setAwake(true)
      enableLimit = flag
      impulse.z = 0.0f
    }
  }

  /**
   * Set the joint limits, usually in meters.
   *
   * @param lower
   * @param upper
   */
  fun setLimits(lower: Float, upper: Float) {
    // assert is not supported in KMP.
    // assert(lower <= upper)
    if (lower != lowerTranslation || upper != upperTranslation) {
      bodyA.setAwake(true)
      bodyB.setAwake(true)
      lowerTranslation = lower
      upperTranslation = upper
      impulse.z = 0.0f
    }
  }

  /**
   * Enable/disable the joint motor.
   *
   * @param flag
   */
  fun enableMotor(flag: Boolean) {
    bodyA.setAwake(true)
    bodyB.setAwake(true)
    enableMotor = flag
  }

  /**
   * Set the motor speed, usually in meters per second.
   *
   * @param speed
   */
  fun setMotorSpeed(speed: Float) {
    bodyA.setAwake(true)
    bodyB.setAwake(true)
    motorSpeed = speed
  }

  /**
   * Set the maximum motor force, usually in N.
   *
   * @param force
   */
  fun setMaxMotorForce(force: Float) {
    bodyA.setAwake(true)
    bodyB.setAwake(true)
    maxMotorForce = force
  }

  /**
   * Get the current motor force, usually in N.
   *
   * @param inv_dt
   * @return
   */
  fun getMotorForce(inv_dt: Float): Float = motorImpulse * inv_dt

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
    val d = pool.popVec2()
    val temp = pool.popVec2()
    val rA = pool.popVec2()
    val rB = pool.popVec2()
    qA.set(aA)
    qB.set(aB)

    // Compute the effective masses.
    Rot.mulToOutUnsafe(qA, d.set(localAnchorA).subLocal(localCenterA), rA)
    Rot.mulToOutUnsafe(qB, d.set(localAnchorB).subLocal(localCenterB), rB)
    d.set(cB).subLocal(cA).addLocal(rB).subLocal(rA)
    val mA = invMassA
    val mB = invMassB
    val iA = invIA
    val iB = invIB

    // Compute motor Jacobian and effective mass.
    run {
      Rot.mulToOutUnsafe(qA, localXAxisA, axis)
      temp.set(d).addLocal(rA)
      a1 = Vec2.cross(temp, axis)
      a2 = Vec2.cross(rB, axis)
      motorMass = mA + mB + iA * a1 * a1 + iB * a2 * a2
      if (motorMass > 0.0f) {
        motorMass = 1.0f / motorMass
      }
    }

    // Prismatic constraint.
    run {
      Rot.mulToOutUnsafe(qA, localYAxisA, perp)
      temp.set(d).addLocal(rA)
      s1 = Vec2.cross(temp, perp)
      s2 = Vec2.cross(rB, perp)
      val k11 = mA + mB + iA * s1 * s1 + iB * s2 * s2
      val k12 = iA * s1 + iB * s2
      val k13 = iA * s1 * a1 + iB * s2 * a2
      var k22 = iA + iB
      if (k22 == 0.0f) {
        // For bodies with fixed rotation.
        k22 = 1.0f
      }
      val k23 = iA * a1 + iB * a2
      val k33 = mA + mB + iA * a1 * a1 + iB * a2 * a2
      K.ex.set(k11, k12, k13)
      K.ey.set(k12, k22, k23)
      K.ez.set(k13, k23, k33)
    }

    // Compute motor and limit terms.
    if (enableLimit) {
      val jointTranslation = Vec2.dot(axis, d)
      if (MathUtils.abs(upperTranslation - lowerTranslation) < 2.0f * Settings.LINEAR_SLOP) {
        limitState = LimitState.EQUAL
      } else if (jointTranslation <= lowerTranslation) {
        if (limitState != LimitState.AT_LOWER) {
          limitState = LimitState.AT_LOWER
          impulse.z = 0.0f
        }
      } else if (jointTranslation >= upperTranslation) {
        if (limitState != LimitState.AT_UPPER) {
          limitState = LimitState.AT_UPPER
          impulse.z = 0.0f
        }
      } else {
        limitState = LimitState.INACTIVE
        impulse.z = 0.0f
      }
    } else {
      limitState = LimitState.INACTIVE
      impulse.z = 0.0f
    }
    if (!enableMotor) {
      motorImpulse = 0.0f
    }
    if (data.step.warmStarting) {
      // Account for variable time step.
      impulse.mulLocal(data.step.dtRatio)
      motorImpulse *= data.step.dtRatio
      val P = pool.popVec2()
      temp.set(axis).mulLocal(motorImpulse + impulse.z)
      P.set(perp).mulLocal(impulse.x).addLocal(temp)
      val LA = impulse.x * s1 + impulse.y + (motorImpulse + impulse.z) * a1
      val LB = impulse.x * s2 + impulse.y + (motorImpulse + impulse.z) * a2
      vA.x -= mA * P.x
      vA.y -= mA * P.y
      wA -= iA * LA
      vB.x += mB * P.x
      vB.y += mB * P.y
      wB += iB * LB
      pool.pushVec2(1)
    } else {
      impulse.setZero()
      motorImpulse = 0.0f
    }

    // data.velocities[m_indexA].v.set(vA);
    data.velocities[indexA].w = wA
    // data.velocities[m_indexB].v.set(vB);
    data.velocities[indexB].w = wB
    pool.pushRot(2)
    pool.pushVec2(4)
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
    val temp = pool.popVec2()

    // Solve linear motor constraint.
    if (enableMotor && limitState != LimitState.EQUAL) {
      temp.set(vB).subLocal(vA)
      val Cdot = Vec2.dot(axis, temp) + a2 * wB - a1 * wA
      var impulse = motorMass * (motorSpeed - Cdot)
      val oldImpulse = motorImpulse
      val maxImpulse = data.step.dt * maxMotorForce
      motorImpulse = MathUtils.clamp(motorImpulse + impulse, -maxImpulse, maxImpulse)
      impulse = motorImpulse - oldImpulse
      val P = pool.popVec2()
      P.set(axis).mulLocal(impulse)
      val LA = impulse * a1
      val LB = impulse * a2
      vA.x -= mA * P.x
      vA.y -= mA * P.y
      wA -= iA * LA
      vB.x += mB * P.x
      vB.y += mB * P.y
      wB += iB * LB
      pool.pushVec2(1)
    }
    val Cdot1 = pool.popVec2()
    temp.set(vB).subLocal(vA)
    Cdot1.x = Vec2.dot(perp, temp) + s2 * wB - s1 * wA
    Cdot1.y = wB - wA
    // System.out.println(Cdot1);
    if (enableLimit && limitState != LimitState.INACTIVE) {
      // Solve prismatic and limit constraint in block form.
      temp.set(vB).subLocal(vA)
      val Cdot2: Float = Vec2.dot(axis, temp) + a2 * wB - a1 * wA
      val Cdot = pool.popVec3()
      Cdot.set(Cdot1.x, Cdot1.y, Cdot2)
      val f1 = pool.popVec3()
      val df = pool.popVec3()
      f1.set(impulse)
      K.solve33ToOut(Cdot.negateLocal(), df)
      // Cdot.negateLocal(); not used anymore
      impulse.addLocal(df)
      if (limitState == LimitState.AT_LOWER) {
        impulse.z = MathUtils.max(impulse.z, 0.0f)
      } else if (limitState == LimitState.AT_UPPER) {
        impulse.z = MathUtils.min(impulse.z, 0.0f)
      }

      // f2(1:2) = invK(1:2,1:2) * (-Cdot(1:2) - K(1:2,3) * (f2(3) - f1(3))) +
      // f1(1:2)
      val b = pool.popVec2()
      val f2r = pool.popVec2()
      temp.set(K.ez.x, K.ez.y).mulLocal(impulse.z - f1.z)
      b.set(Cdot1).negateLocal().subLocal(temp)
      K.solve22ToOut(b, f2r)
      f2r.addLocal(f1.x, f1.y)
      impulse.x = f2r.x
      impulse.y = f2r.y
      df.set(impulse).subLocal(f1)
      val P = pool.popVec2()
      temp.set(axis).mulLocal(df.z)
      P.set(perp).mulLocal(df.x).addLocal(temp)
      val LA = df.x * s1 + df.y + df.z * a1
      val LB = df.x * s2 + df.y + df.z * a2
      vA.x -= mA * P.x
      vA.y -= mA * P.y
      wA -= iA * LA
      vB.x += mB * P.x
      vB.y += mB * P.y
      wB += iB * LB
      pool.pushVec2(3)
      pool.pushVec3(3)
    } else {
      // Limit is inactive, just solve the prismatic constraint in block form.
      val df = pool.popVec2()
      K.solve22ToOut(Cdot1.negateLocal(), df)
      Cdot1.negateLocal()
      impulse.x += df.x
      impulse.y += df.y
      val P = pool.popVec2()
      P.set(perp).mulLocal(df.x)
      val LA = df.x * s1 + df.y
      val LB = df.x * s2 + df.y
      vA.x -= mA * P.x
      vA.y -= mA * P.y
      wA -= iA * LA
      vB.x += mB * P.x
      vB.y += mB * P.y
      wB += iB * LB
      pool.pushVec2(2)
    }

    // data.velocities[m_indexA].v.set(vA);
    data.velocities[indexA].w = wA
    // data.velocities[m_indexB].v.set(vB);
    data.velocities[indexB].w = wB
    pool.pushVec2(2)
  }

  override fun solvePositionConstraints(data: SolverData): Boolean {
    val qA = pool.popRot()
    val qB = pool.popRot()
    val rA = pool.popVec2()
    val rB = pool.popVec2()
    val d = pool.popVec2()
    val axis = pool.popVec2()
    val perp = pool.popVec2()
    val temp = pool.popVec2()
    val C1 = pool.popVec2()
    val impulse = pool.popVec3()
    val cA = data.positions[indexA].c
    var aA = data.positions[indexA].a
    val cB = data.positions[indexB].c
    var aB = data.positions[indexB].a
    qA.set(aA)
    qB.set(aB)
    val mA = invMassA
    val mB = invMassB
    val iA = invIA
    val iB = invIB

    // Compute fresh Jacobians
    Rot.mulToOutUnsafe(qA, temp.set(localAnchorA).subLocal(localCenterA), rA)
    Rot.mulToOutUnsafe(qB, temp.set(localAnchorB).subLocal(localCenterB), rB)
    d.set(cB).addLocal(rB).subLocal(cA).subLocal(rA)
    Rot.mulToOutUnsafe(qA, localXAxisA, axis)
    val a1 = Vec2.cross(temp.set(d).addLocal(rA), axis)
    val a2 = Vec2.cross(rB, axis)
    Rot.mulToOutUnsafe(qA, localYAxisA, perp)
    val s1 = Vec2.cross(temp.set(d).addLocal(rA), perp)
    val s2 = Vec2.cross(rB, perp)
    C1.x = Vec2.dot(perp, d)
    C1.y = aB - aA - referenceAngle
    var linearError = MathUtils.abs(C1.x)
    val angularError = MathUtils.abs(C1.y)
    var active = false
    var C2 = 0.0f
    if (enableLimit) {
      val translation = Vec2.dot(axis, d)
      if (MathUtils.abs(upperTranslation - lowerTranslation) < 2.0f * Settings.LINEAR_SLOP) {
        // Prevent large angular corrections
        C2 =
          MathUtils.clamp(
            translation,
            -Settings.MAX_LINEAR_CORRECTION,
            Settings.MAX_LINEAR_CORRECTION
          )
        linearError = MathUtils.max(linearError, MathUtils.abs(translation))
        active = true
      } else if (translation <= lowerTranslation) {
        // Prevent large linear corrections and allow some slop.
        C2 =
          MathUtils.clamp(
            translation - lowerTranslation + Settings.LINEAR_SLOP,
            -Settings.MAX_LINEAR_CORRECTION,
            0.0f
          )
        linearError = MathUtils.max(linearError, lowerTranslation - translation)
        active = true
      } else if (translation >= upperTranslation) {
        // Prevent large linear corrections and allow some slop.
        C2 =
          MathUtils.clamp(
            translation - upperTranslation - Settings.LINEAR_SLOP,
            0.0f,
            Settings.MAX_LINEAR_CORRECTION
          )
        linearError = MathUtils.max(linearError, translation - upperTranslation)
        active = true
      }
    }
    if (active) {
      val k11 = mA + mB + iA * s1 * s1 + iB * s2 * s2
      val k12 = iA * s1 + iB * s2
      val k13 = iA * s1 * a1 + iB * s2 * a2
      var k22 = iA + iB
      if (k22 == 0.0f) {
        // For fixed rotation
        k22 = 1.0f
      }
      val k23 = iA * a1 + iB * a2
      val k33 = mA + mB + iA * a1 * a1 + iB * a2 * a2
      val K = pool.popMat33()
      K.ex.set(k11, k12, k13)
      K.ey.set(k12, k22, k23)
      K.ez.set(k13, k23, k33)
      val C = pool.popVec3()
      C.x = C1.x
      C.y = C1.y
      C.z = C2
      K.solve33ToOut(C.negateLocal(), impulse)
      pool.pushVec3(1)
      pool.pushMat33(1)
    } else {
      val k11 = mA + mB + iA * s1 * s1 + iB * s2 * s2
      val k12 = iA * s1 + iB * s2
      var k22 = iA + iB
      if (k22 == 0.0f) {
        k22 = 1.0f
      }
      val K = pool.popMat22()
      K.ex.set(k11, k12)
      K.ey.set(k12, k22)

      // temp is impulse1
      K.solveToOut(C1.negateLocal(), temp)
      C1.negateLocal()
      impulse.x = temp.x
      impulse.y = temp.y
      impulse.z = 0.0f
      pool.pushMat22(1)
    }
    val Px = impulse.x * perp.x + impulse.z * axis.x
    val Py = impulse.x * perp.y + impulse.z * axis.y
    val LA = impulse.x * s1 + impulse.y + impulse.z * a1
    val LB = impulse.x * s2 + impulse.y + impulse.z * a2
    cA.x -= mA * Px
    cA.y -= mA * Py
    aA -= iA * LA
    cB.x += mB * Px
    cB.y += mB * Py
    aB += iB * LB

    // data.positions[m_indexA].c.set(cA);
    data.positions[indexA].a = aA
    // data.positions[m_indexB].c.set(cB);
    data.positions[indexB].a = aB
    pool.pushVec2(7)
    pool.pushVec3(1)
    pool.pushRot(2)
    return linearError <= Settings.LINEAR_SLOP && angularError <= Settings.ANGULAR_SLOP
  }
}
