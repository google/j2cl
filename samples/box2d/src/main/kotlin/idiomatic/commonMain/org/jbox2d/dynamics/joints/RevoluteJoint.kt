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

// Point-to-point constraint
// C = p2 - p1
// Cdot = v2 - v1
//   = v2 + cross(w2, r2) - v1 - cross(w1, r1)
// J = [-I -r1_skew I r2_skew ]
// Identity used:
// w k % (rx i + ry j) = w * (-ry i + rx j)
// Motor constraint
// Cdot = w2 - w1
// J = [0 0 -1 0 0 1]
// K = invI1 + invI2
/**
 * A revolute joint constrains two bodies to share a common point while they are free to rotate
 * about the point. The relative rotation about the shared point is the joint angle. You can limit
 * the relative rotation with a joint limit that specifies a lower and upper angle. You can use a
 * motor to drive the relative rotation about the shared point. A maximum motor torque is provided
 * so that infinite forces are not generated.
 *
 * @author Daniel Murphy
 */
class RevoluteJoint(argWorld: IWorldPool, def: RevoluteJointDef) : Joint(argWorld, def) {
  // Solver shared
  val localAnchorA = Vec2().apply { set(def.localAnchorA) }
  val localAnchorB = Vec2().apply { set(def.localAnchorB) }
  var referenceAngle: Float = def.referenceAngle
    private set
  var enableMotor: Boolean = def.enableMotor
    private set
  var maxMotorTorque: Float = def.maxMotorTorque
    private set
  var motorSpeed: Float = def.motorSpeed
    private set
  var enableLimit: Boolean = def.enableLimit
    private set
  var lowerAngle: Float = def.lowerAngle
    private set
  var upperAngle: Float = def.upperAngle
    private set
  val jointAngle: Float
    get() = bodyB.sweep.a - bodyA.sweep.a - referenceAngle
  val jointSpeed: Float
    get() = bodyB.angularVelocity - bodyA.angularVelocity

  private val impulse = Vec3()
  private var motorImpulse: Float = 0f
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
  private val mass = Mat33() // effective mass for point-to-point constraint.
  private var motorMass = 0f // effective mass for motor/limit angular constraint.
  private var limitState: LimitState = LimitState.INACTIVE

  override fun initVelocityConstraints(data: SolverData) {
    indexA = bodyA.islandIndex
    indexB = bodyB.islandIndex
    localCenterA.set(bodyA.sweep.localCenter)
    localCenterB.set(bodyB.sweep.localCenter)
    invMassA = bodyA.invMass
    invMassB = bodyB.invMass
    invIA = bodyA.invI
    invIB = bodyB.invI

    // Vec2 cA = data.positions[m_indexA].c;
    val aA = data.positions[indexA].a
    val vA = data.velocities[indexA].v
    var wA = data.velocities[indexA].w

    // Vec2 cB = data.positions[m_indexB].c;
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
    val fixedRotation = iA + iB == 0.0f
    mass.ex.x = mA + mB + rA.y * rA.y * iA + rB.y * rB.y * iB
    mass.ey.x = -rA.y * rA.x * iA - rB.y * rB.x * iB
    mass.ez.x = -rA.y * iA - rB.y * iB
    mass.ex.y = mass.ey.x
    mass.ey.y = mA + mB + rA.x * rA.x * iA + rB.x * rB.x * iB
    mass.ez.y = rA.x * iA + rB.x * iB
    mass.ex.z = mass.ez.x
    mass.ey.z = mass.ez.y
    mass.ez.z = iA + iB
    motorMass = iA + iB
    if (motorMass > 0.0f) {
      motorMass = 1.0f / motorMass
    }
    if (!enableMotor || fixedRotation) {
      motorImpulse = 0.0f
    }
    if (enableLimit && !fixedRotation) {
      val jointAngle = aB - aA - referenceAngle
      if (MathUtils.abs(upperAngle - lowerAngle) < 2.0f * Settings.ANGULAR_SLOP) {
        limitState = LimitState.EQUAL
      } else if (jointAngle <= lowerAngle) {
        if (limitState != LimitState.AT_LOWER) {
          impulse.z = 0.0f
        }
        limitState = LimitState.AT_LOWER
      } else if (jointAngle >= upperAngle) {
        if (limitState != LimitState.AT_UPPER) {
          impulse.z = 0.0f
        }
        limitState = LimitState.AT_UPPER
      } else {
        limitState = LimitState.INACTIVE
        impulse.z = 0.0f
      }
    } else {
      limitState = LimitState.INACTIVE
    }
    if (data.step.warmStarting) {
      val P = pool.popVec2()
      // Scale impulses to support a variable time step.
      impulse.x *= data.step.dtRatio
      impulse.y *= data.step.dtRatio
      motorImpulse *= data.step.dtRatio
      P.x = impulse.x
      P.y = impulse.y
      vA.x -= mA * P.x
      vA.y -= mA * P.y
      wA -= iA * (Vec2.cross(rA, P) + motorImpulse + impulse.z)
      vB.x += mB * P.x
      vB.y += mB * P.y
      wB += iB * (Vec2.cross(rB, P) + motorImpulse + impulse.z)
      pool.pushVec2(1)
    } else {
      impulse.setZero()
      motorImpulse = 0.0f
    }
    // data.velocities[m_indexA].v.set(vA);
    data.velocities[indexA].w = wA
    // data.velocities[m_indexB].v.set(vB);
    data.velocities[indexB].w = wB
    pool.pushVec2(1)
    pool.pushRot(2)
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
    val fixedRotation = iA + iB == 0.0f

    // Solve motor constraint.
    if (enableMotor && limitState != LimitState.EQUAL && !fixedRotation) {
      val Cdot = wB - wA - motorSpeed
      var localImpulse = -motorMass * Cdot
      val oldImpulse = motorImpulse
      val maxImpulse = data.step.dt * maxMotorTorque
      motorImpulse = MathUtils.clamp(motorImpulse + localImpulse, -maxImpulse, maxImpulse)
      localImpulse = motorImpulse - oldImpulse
      wA -= iA * localImpulse
      wB += iB * localImpulse
    }
    val temp = pool.popVec2()

    // Solve limit constraint.
    if (enableLimit && limitState != LimitState.INACTIVE && !fixedRotation) {
      val Cdot1 = pool.popVec2()
      val Cdot = pool.popVec3()

      // Solve point-to-point constraint
      Vec2.crossToOutUnsafe(wA, rA, temp)
      Vec2.crossToOutUnsafe(wB, rB, Cdot1)
      Cdot1.addLocal(vB).subLocal(vA).subLocal(temp)
      val Cdot2 = wB - wA
      Cdot.set(Cdot1.x, Cdot1.y, Cdot2)
      val localImpulse = pool.popVec3()
      mass.solve33ToOut(Cdot, localImpulse)
      localImpulse.negateLocal()
      if (limitState == LimitState.EQUAL) {
        impulse.addLocal(localImpulse)
      } else if (limitState == LimitState.AT_LOWER) {
        val newImpulse = impulse.z + localImpulse.z
        if (newImpulse < 0.0f) {
          val rhs = pool.popVec2()
          rhs.set(mass.ez.x, mass.ez.y).mulLocal(impulse.z).subLocal(Cdot1)
          mass.solve22ToOut(rhs, temp)
          localImpulse.x = temp.x
          localImpulse.y = temp.y
          localImpulse.z = -impulse.z
          impulse.x += temp.x
          impulse.y += temp.y
          impulse.z = 0.0f
          pool.pushVec2(1)
        } else {
          impulse.addLocal(localImpulse)
        }
      } else if (limitState == LimitState.AT_UPPER) {
        val newImpulse = impulse.z + localImpulse.z
        if (newImpulse > 0.0f) {
          val rhs = pool.popVec2()
          rhs.set(mass.ez.x, mass.ez.y).mulLocal(impulse.z).subLocal(Cdot1)
          mass.solve22ToOut(rhs, temp)
          localImpulse.x = temp.x
          localImpulse.y = temp.y
          localImpulse.z = -impulse.z
          impulse.x += temp.x
          impulse.y += temp.y
          impulse.z = 0.0f
          pool.pushVec2(1)
        } else {
          impulse.addLocal(localImpulse)
        }
      }
      val P = pool.popVec2()
      P.set(localImpulse.x, localImpulse.y)
      vA.x -= mA * P.x
      vA.y -= mA * P.y
      wA -= iA * (Vec2.cross(rA, P) + localImpulse.z)
      vB.x += mB * P.x
      vB.y += mB * P.y
      wB += iB * (Vec2.cross(rB, P) + localImpulse.z)
      pool.pushVec2(2)
      pool.pushVec3(2)
    } else {

      // Solve point-to-point constraint
      val Cdot = pool.popVec2()
      val impulse = pool.popVec2()
      Vec2.crossToOutUnsafe(wA, rA, temp)
      Vec2.crossToOutUnsafe(wB, rB, Cdot)
      Cdot.addLocal(vB).subLocal(vA).subLocal(temp)
      mass.solve22ToOut(Cdot.negateLocal(), impulse) // just leave negated
      impulse.x += impulse.x
      impulse.y += impulse.y
      vA.x -= mA * impulse.x
      vA.y -= mA * impulse.y
      wA -= iA * Vec2.cross(rA, impulse)
      vB.x += mB * impulse.x
      vB.y += mB * impulse.y
      wB += iB * Vec2.cross(rB, impulse)
      pool.pushVec2(2)
    }

    // data.velocities[m_indexA].v.set(vA);
    data.velocities[indexA].w = wA
    // data.velocities[m_indexB].v.set(vB);
    data.velocities[indexB].w = wB
    pool.pushVec2(1)
  }

  override fun solvePositionConstraints(data: SolverData): Boolean {
    val qA = pool.popRot()
    val qB = pool.popRot()
    val cA = data.positions[indexA].c
    var aA = data.positions[indexA].a
    val cB = data.positions[indexB].c
    var aB = data.positions[indexB].a
    qA.set(aA)
    qB.set(aB)
    var angularError = 0.0f
    var positionError: Float
    val fixedRotation = invIA + invIB == 0.0f

    // Solve angular limit constraint.
    if (enableLimit && limitState != LimitState.INACTIVE && !fixedRotation) {
      val angle = aB - aA - referenceAngle
      var limitImpulse = 0.0f
      if (limitState == LimitState.EQUAL) {
        // Prevent large angular corrections
        val C =
          MathUtils.clamp(
            angle - lowerAngle,
            -Settings.MAX_ANGULAR_CORRECTION,
            Settings.MAX_ANGULAR_CORRECTION
          )
        limitImpulse = -motorMass * C
        angularError = MathUtils.abs(C)
      } else if (limitState == LimitState.AT_LOWER) {
        var C = angle - lowerAngle
        angularError = -C

        // Prevent large angular corrections and allow some slop.
        C = MathUtils.clamp(C + Settings.ANGULAR_SLOP, -Settings.MAX_ANGULAR_CORRECTION, 0.0f)
        limitImpulse = -motorMass * C
      } else if (limitState == LimitState.AT_UPPER) {
        var C = angle - upperAngle
        angularError = C

        // Prevent large angular corrections and allow some slop.
        C = MathUtils.clamp(C - Settings.ANGULAR_SLOP, 0.0f, Settings.MAX_ANGULAR_CORRECTION)
        limitImpulse = -motorMass * C
      }
      aA -= invIA * limitImpulse
      aB += invIB * limitImpulse
    }
    // Solve point-to-point constraint.
    run {
      qA.set(aA)
      qB.set(aB)
      val rA = pool.popVec2()
      val rB = pool.popVec2()
      val C = pool.popVec2()
      val impulse = pool.popVec2()
      Rot.mulToOutUnsafe(qA, C.set(localAnchorA).subLocal(localCenterA), rA)
      Rot.mulToOutUnsafe(qB, C.set(localAnchorB).subLocal(localCenterB), rB)
      C.set(cB).addLocal(rB).subLocal(cA).subLocal(rA)
      positionError = C.length()
      val mA = invMassA
      val mB = invMassB
      val iA = invIA
      val iB = invIB
      val K = pool.popMat22()
      K.ex.x = mA + mB + iA * rA.y * rA.y + iB * rB.y * rB.y
      K.ex.y = -iA * rA.x * rA.y - iB * rB.x * rB.y
      K.ey.x = K.ex.y
      K.ey.y = mA + mB + iA * rA.x * rA.x + iB * rB.x * rB.x
      K.solveToOut(C, impulse)
      impulse.negateLocal()
      cA.x -= mA * impulse.x
      cA.y -= mA * impulse.y
      aA -= iA * Vec2.cross(rA, impulse)
      cB.x += mB * impulse.x
      cB.y += mB * impulse.y
      aB += iB * Vec2.cross(rB, impulse)
      pool.pushVec2(4)
      pool.pushMat22(1)
    }
    // data.positions[m_indexA].c.set(cA);
    data.positions[indexA].a = aA
    // data.positions[m_indexB].c.set(cB);
    data.positions[indexB].a = aB
    pool.pushRot(2)
    return positionError <= Settings.LINEAR_SLOP && angularError <= Settings.ANGULAR_SLOP
  }

  override fun getAnchorA(out: Vec2) {
    bodyA.getWorldPointToOut(localAnchorA, out)
  }

  override fun getAnchorB(out: Vec2) {
    bodyB.getWorldPointToOut(localAnchorB, out)
  }

  override fun getReactionForce(inv_dt: Float, out: Vec2) {
    out.set(impulse.x, impulse.y).mulLocal(inv_dt)
  }

  override fun getReactionTorque(inv_dt: Float): Float = inv_dt * impulse.z

  fun enableMotor(flag: Boolean) {
    bodyA.setAwake(true)
    bodyB.setAwake(true)
    enableMotor = flag
  }

  fun getMotorTorque(inv_dt: Float): Float = motorImpulse * inv_dt

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

  fun enableLimit(flag: Boolean) {
    if (flag != enableLimit) {
      bodyA.setAwake(true)
      bodyB.setAwake(true)
      enableLimit = flag
      impulse.z = 0.0f
    }
  }

  fun setLimits(lower: Float, upper: Float) {
    // assert is not supported in KMP.
    // assert(lower <= upper)
    if (lower != lowerAngle || upper != upperAngle) {
      bodyA.setAwake(true)
      bodyB.setAwake(true)
      impulse.z = 0.0f
      lowerAngle = lower
      upperAngle = upper
    }
  }
}
