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
package org.jbox2d.dynamics.contacts

import org.jbox2d.collision.Manifold
import org.jbox2d.collision.WorldManifold
import org.jbox2d.common.Mat22
import org.jbox2d.common.MathUtils
import org.jbox2d.common.Rot
import org.jbox2d.common.Settings
import org.jbox2d.common.Transform
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.TimeStep

/** @author Daniel */
class ContactSolver {
  lateinit var step: TimeStep
  lateinit var positions: Array<Position>
  lateinit var velocities: Array<Velocity>
  var positionConstraints: Array<ContactPositionConstraint> =
    Array<ContactPositionConstraint>(INITIAL_NUM_CONSTRAINTS) { ContactPositionConstraint() }
  var velocityConstraints: Array<ContactVelocityConstraint> =
    Array<ContactVelocityConstraint>(INITIAL_NUM_CONSTRAINTS) { ContactVelocityConstraint() }
  lateinit var contacts: Array<Contact?>
  var count = 0

  // djm pooling
  private val tangent = Vec2()
  private val temp1 = Vec2()
  private val temp2 = Vec2()

  // djm pooling, and from above
  private val P = Vec2()
  private val temp = Vec2()

  // djm pooling, and from above
  private val xfA = Transform()
  private val xfB = Transform()
  private val worldManifold = WorldManifold()

  // djm pooling from above
  private val a = Vec2()
  private val b = Vec2()
  private val dv1 = Vec2()
  private val dv2 = Vec2()
  private val x = Vec2()
  private val d = Vec2()
  private val P1 = Vec2()
  private val P2 = Vec2()

  // djm pooling, and from above
  private val psolver = PositionSolverManifold()
  private val rA = Vec2()
  private val rB = Vec2()

  fun init(def: ContactSolverDef) {
    // System.out.println("Initializing contact solver");
    step = def.step
    count = def.count
    if (positionConstraints.size < count) {
      val old = positionConstraints
      positionConstraints =
        Array<ContactPositionConstraint>(MathUtils.max(old.size * 2, count)) {
          ContactPositionConstraint()
        }
      old.copyInto(positionConstraints)
    }
    if (velocityConstraints.size < count) {
      val old = velocityConstraints
      velocityConstraints =
        Array<ContactVelocityConstraint>(MathUtils.max(old.size * 2, count)) {
          ContactVelocityConstraint()
        }
      old.copyInto(velocityConstraints)
    }
    positions = def.positions
    velocities = def.velocities
    contacts = def.contacts
    for (i in 0 until count) {
      // System.out.println("contacts: " + m_count);
      val contact = contacts[i]!!
      val fixtureA = contact.fixtureA
      val fixtureB = contact.fixtureB
      val shapeA = fixtureA.shape!!
      val shapeB = fixtureB.shape!!
      val radiusA = shapeA.radius
      val radiusB = shapeB.radius
      val bodyA = fixtureA.body!!
      val bodyB = fixtureB.body!!
      val manifold = contact.manifold
      val pointCount = manifold.pointCount
      // assert is not supported in KMP.
      // assert(pointCount > 0)
      val vc = velocityConstraints[i]
      vc.friction = contact.friction
      vc.restitution = contact.restitution
      vc.tangentSpeed = contact.tangentSpeed
      vc.indexA = bodyA.islandIndex
      vc.indexB = bodyB.islandIndex
      vc.invMassA = bodyA.invMass
      vc.invMassB = bodyB.invMass
      vc.invIA = bodyA.invI
      vc.invIB = bodyB.invI
      vc.contactIndex = i
      vc.pointCount = pointCount
      vc.K.setZero()
      vc.normalMass.setZero()
      val pc = positionConstraints[i]
      pc.indexA = bodyA.islandIndex
      pc.indexB = bodyB.islandIndex
      pc.invMassA = bodyA.invMass
      pc.invMassB = bodyB.invMass
      pc.localCenterA.set(bodyA.sweep.localCenter)
      pc.localCenterB.set(bodyB.sweep.localCenter)
      pc.invIA = bodyA.invI
      pc.invIB = bodyB.invI
      pc.localNormal.set(manifold.localNormal)
      pc.localPoint.set(manifold.localPoint)
      pc.pointCount = pointCount
      pc.radiusA = radiusA
      pc.radiusB = radiusB
      pc.type = manifold.type!!

      // System.out.println("contact point count: " + pointCount);
      for (j in 0 until pointCount) {
        val cp = manifold.points[j]
        val vcp = vc.points[j]
        if (step.warmStarting) {
          // assert(cp.normalImpulse == 0);
          // System.out.println("contact normal impulse: " + cp.normalImpulse);
          vcp.normalImpulse = step.dtRatio * cp.normalImpulse
          vcp.tangentImpulse = step.dtRatio * cp.tangentImpulse
        } else {
          vcp.normalImpulse = 0f
          vcp.tangentImpulse = 0f
        }
        vcp.rA.setZero()
        vcp.rB.setZero()
        vcp.normalMass = 0f
        vcp.tangentMass = 0f
        vcp.velocityBias = 0f
        pc.localPoints[j].x = cp.localPoint.x
        pc.localPoints[j].y = cp.localPoint.y
      }
    }
  }

  fun warmStart() {
    // Warm start.
    for (i in 0 until count) {
      val vc = velocityConstraints[i]
      val indexA = vc.indexA
      val indexB = vc.indexB
      val mA = vc.invMassA
      val iA = vc.invIA
      val mB = vc.invMassB
      val iB = vc.invIB
      val pointCount = vc.pointCount
      val vA = velocities[indexA].v
      var wA = velocities[indexA].w
      val vB = velocities[indexB].v
      var wB = velocities[indexB].w
      val normal = vc.normal
      val tangentx = 1.0f * normal.y
      val tangenty = -1.0f * normal.x
      for (j in 0 until pointCount) {
        val vcp = vc.points[j]
        val Px = tangentx * vcp.tangentImpulse + normal.x * vcp.normalImpulse
        val Py = tangenty * vcp.tangentImpulse + normal.y * vcp.normalImpulse
        wA -= iA * (vcp.rA.x * Py - vcp.rA.y * Px)
        vA.x -= Px * mA
        vA.y -= Py * mA
        wB += iB * (vcp.rB.x * Py - vcp.rB.y * Px)
        vB.x += Px * mB
        vB.y += Py * mB
      }
      velocities[indexA].w = wA
      velocities[indexB].w = wB
    }
  }

  fun initializeVelocityConstraints() {
    // Warm start.
    for (i in 0 until count) {
      val vc = velocityConstraints[i]
      val pc = positionConstraints[i]
      val radiusA = pc.radiusA
      val radiusB = pc.radiusB
      val manifold = contacts[vc.contactIndex]!!.manifold
      val indexA = vc.indexA
      val indexB = vc.indexB
      val mA = vc.invMassA
      val mB = vc.invMassB
      val iA = vc.invIA
      val iB = vc.invIB
      val localCenterA = pc.localCenterA
      val localCenterB = pc.localCenterB
      val cA = positions[indexA].c
      val aA = positions[indexA].a
      val vA = velocities[indexA].v
      val wA = velocities[indexA].w
      val cB = positions[indexB].c
      val aB = positions[indexB].a
      val vB = velocities[indexB].v
      val wB = velocities[indexB].w
      // assert is not supported in KMP.
      // assert(manifold!!.pointCount > 0)
      xfA.q.set(aA)
      xfB.q.set(aB)
      xfA.p.x = cA.x - (xfA.q.cos * localCenterA.x - xfA.q.sin * localCenterA.y)
      xfA.p.y = cA.y - (xfA.q.sin * localCenterA.x + xfA.q.cos * localCenterA.y)
      xfB.p.x = cB.x - (xfB.q.cos * localCenterB.x - xfB.q.sin * localCenterB.y)
      xfB.p.y = cB.y - (xfB.q.sin * localCenterB.x + xfB.q.cos * localCenterB.y)
      worldManifold.initialize(manifold, xfA, radiusA, xfB, radiusB)
      vc.normal.set(worldManifold.normal)
      val pointCount = vc.pointCount
      for (j in 0 until pointCount) {
        val vcp = vc.points[j]
        vcp.rA.set(worldManifold.points[j]).subLocal(cA)
        vcp.rB.set(worldManifold.points[j]).subLocal(cB)
        val rnA = vcp.rA.x * vc.normal.y - vcp.rA.y * vc.normal.x
        val rnB = vcp.rB.x * vc.normal.y - vcp.rB.y * vc.normal.x
        val kNormal = mA + mB + iA * rnA * rnA + iB * rnB * rnB
        vcp.normalMass = if (kNormal > 0.0f) 1.0f / kNormal else 0.0f
        val tangentx = 1.0f * vc.normal.y
        val tangenty = -1.0f * vc.normal.x
        val rtA = vcp.rA.x * tangenty - vcp.rA.y * tangentx
        val rtB = vcp.rB.x * tangenty - vcp.rB.y * tangentx
        val kTangent = mA + mB + iA * rtA * rtA + iB * rtB * rtB
        vcp.tangentMass = if (kTangent > 0.0f) 1.0f / kTangent else 0.0f

        // Setup a velocity bias for restitution.
        vcp.velocityBias = 0.0f
        val tempx = vB.x + -wB * vcp.rB.y - vA.x - -wA * vcp.rA.y
        val tempy = vB.y + wB * vcp.rB.x - vA.y - wA * vcp.rA.x
        val vRel = vc.normal.x * tempx + vc.normal.y * tempy
        if (vRel < -Settings.VELOCITY_THRESHOLD) {
          vcp.velocityBias = -vc.restitution * vRel
        }
      }

      // If we have two points, then prepare the block solver.
      if (vc.pointCount == 2) {
        val vcp1 = vc.points[0]
        val vcp2 = vc.points[1]
        val rn1A = Vec2.cross(vcp1.rA, vc.normal)
        val rn1B = Vec2.cross(vcp1.rB, vc.normal)
        val rn2A = Vec2.cross(vcp2.rA, vc.normal)
        val rn2B = Vec2.cross(vcp2.rB, vc.normal)
        val k11 = mA + mB + iA * rn1A * rn1A + iB * rn1B * rn1B
        val k22 = mA + mB + iA * rn2A * rn2A + iB * rn2B * rn2B
        val k12 = mA + mB + iA * rn1A * rn2A + iB * rn1B * rn2B
        if (k11 * k11 < k_maxConditionNumber * (k11 * k22 - k12 * k12)) {
          // K is safe to invert.
          vc.K.ex.set(k11, k12)
          vc.K.ey.set(k12, k22)
          vc.K.invertToOut(vc.normalMass)
        } else {
          // The constraints are redundant, just use one.
          // TODO_ERIN use deepest?
          vc.pointCount = 1
        }
      }
    }
  }

  fun solveVelocityConstraints() {
    for (i in 0 until count) {
      val vc = velocityConstraints[i]
      val indexA = vc.indexA
      val indexB = vc.indexB
      val mA = vc.invMassA
      val mB = vc.invMassB
      val iA = vc.invIA
      val iB = vc.invIB
      val pointCount = vc.pointCount
      val vA = velocities[indexA].v
      var wA = velocities[indexA].w
      val vB = velocities[indexB].v
      var wB = velocities[indexB].w
      val normal = vc.normal
      tangent.x = 1.0f * vc.normal.y
      tangent.y = -1.0f * vc.normal.x
      val friction = vc.friction
      // assert is not supported in KMP.
      // assert(pointCount == 1 || pointCount == 2)

      // Solve tangent constraints
      for (j in 0 until pointCount) {
        val vcp = vc.points[j]
        val a = vcp.rA
        val dvx = -wB * vcp.rB.y + vB.x - vA.x + wA * a.y
        val dvy = wB * vcp.rB.x + vB.y - vA.y - wA * a.x

        // Compute tangent force
        val vt = dvx * tangent.x + dvy * tangent.y - vc.tangentSpeed
        var lambda = vcp.tangentMass * -vt

        // Clamp the accumulated force
        val maxFriction = friction * vcp.normalImpulse
        val newImpulse = MathUtils.clamp(vcp.tangentImpulse + lambda, -maxFriction, maxFriction)
        lambda = newImpulse - vcp.tangentImpulse
        vcp.tangentImpulse = newImpulse

        // Apply contact impulse
        // Vec2 P = lambda * tangent;
        val Px = tangent.x * lambda
        val Py = tangent.y * lambda

        // vA -= invMassA * P;
        vA.x -= Px * mA
        vA.y -= Py * mA
        wA -= iA * (vcp.rA.x * Py - vcp.rA.y * Px)

        // vB += invMassB * P;
        vB.x += Px * mB
        vB.y += Py * mB
        wB += iB * (vcp.rB.x * Py - vcp.rB.y * Px)
      }

      // Solve normal constraints
      if (vc.pointCount == 1) {
        val vcp = vc.points[0]

        // Relative velocity at contact
        // Vec2 dv = vB + Cross(wB, vcp.rB) - vA - Cross(wA, vcp.rA);
        val dvx = -wB * vcp.rB.y + vB.x - vA.x + wA * vcp.rA.y
        val dvy = wB * vcp.rB.x + vB.y - vA.y - wA * vcp.rA.x

        // Compute normal impulse
        val vn = dvx * normal.x + dvy * normal.y
        var lambda = -vcp.normalMass * (vn - vcp.velocityBias)

        // Clamp the accumulated impulse
        val a = vcp.normalImpulse + lambda
        val newImpulse = if (a > 0.0f) a else 0.0f
        lambda = newImpulse - vcp.normalImpulse
        vcp.normalImpulse = newImpulse

        // Apply contact impulse
        val Px = normal.x * lambda
        val Py = normal.y * lambda

        // vA -= invMassA * P;
        vA.x -= Px * mA
        vA.y -= Py * mA
        wA -= iA * (vcp.rA.x * Py - vcp.rA.y * Px)

        // vB += invMassB * P;
        vB.x += Px * mB
        vB.y += Py * mB
        wB += iB * (vcp.rB.x * Py - vcp.rB.y * Px)
      } else {
        // Block solver developed in collaboration with Dirk Gregorius (back in 01/07 on
        // Box2D_Lite).
        // Build the mini LCP for this contact patch
        //
        // vn = A * x + b, vn >= 0, , vn >= 0, x >= 0 and vn_i * x_i = 0 with i = 1..2
        //
        // A = J * W * JT and J = ( -n, -r1 x n, n, r2 x n )
        // b = vn_0 - velocityBias
        //
        // The system is solved using the "Total enumeration method" (s. Murty). The complementary
        // constraint vn_i * x_i
        // implies that we must have in any solution either vn_i = 0 or x_i = 0. So for the 2D
        // contact problem the cases
        // vn1 = 0 and vn2 = 0, x1 = 0 and x2 = 0, x1 = 0 and vn2 = 0, x2 = 0 and vn1 = 0 need to be
        // tested. The first valid
        // solution that satisfies the problem is chosen.
        //
        // In order to account of the accumulated impulse 'a' (because of the iterative nature of
        // the solver which only requires
        // that the accumulated impulse is clamped and not the incremental impulse) we change the
        // impulse variable (x_i).
        //
        // Substitute:
        //
        // x = a + d
        //
        // a := old total impulse
        // x := new total impulse
        // d := incremental impulse
        //
        // For the current iteration we extend the formula for the incremental impulse
        // to compute the new total impulse:
        //
        // vn = A * d + b
        // = A * (x - a) + b
        // = A * x + b - A * a
        // = A * x + b'
        // b' = b - A * a;
        val cp1 = vc.points[0]
        val cp2 = vc.points[1]
        a.x = cp1.normalImpulse
        a.y = cp2.normalImpulse
        // assert is not supported in KMP.
        // assert(a.x >= 0.0f && a.y >= 0.0f)
        // Relative velocity at contact
        // Vec2 dv1 = vB + Cross(wB, cp1.rB) - vA - Cross(wA, cp1.rA);
        dv1.x = -wB * cp1.rB.y + vB.x - vA.x + wA * cp1.rA.y
        dv1.y = wB * cp1.rB.x + vB.y - vA.y - wA * cp1.rA.x

        // Vec2 dv2 = vB + Cross(wB, cp2.rB) - vA - Cross(wA, cp2.rA);
        dv2.x = -wB * cp2.rB.y + vB.x - vA.x + wA * cp2.rA.y
        dv2.y = wB * cp2.rB.x + vB.y - vA.y - wA * cp2.rA.x

        // Compute normal velocity
        var vn1 = dv1.x * normal.x + dv1.y * normal.y
        var vn2 = dv2.x * normal.x + dv2.y * normal.y
        b.x = vn1 - cp1.velocityBias
        b.y = vn2 - cp2.velocityBias
        // System.out.println("b is " + b.x + "," + b.y);

        // Compute b'
        val R = vc.K
        b.x -= R.ex.x * a.x + R.ey.x * a.y
        b.y -= R.ex.y * a.x + R.ey.y * a.y
        // System.out.println("b' is " + b.x + "," + b.y);

        // final float k_errorTol = 1e-3f;
        // B2_NOT_USED(k_errorTol);
        while (true) {

          //
          // Case 1: vn = 0
          //
          // 0 = A * x' + b'
          //
          // Solve for x':
          //
          // x' = - inv(A) * b'
          //
          // Vec2 x = - Mul(c.normalMass, b);
          Mat22.mulToOutUnsafe(vc.normalMass, b, x)
          x.x *= -1f
          x.y *= -1f
          if (x.x >= 0.0f && x.y >= 0.0f) {
            // System.out.println("case 1");
            // Get the incremental impulse
            // Vec2 d = x - a;
            d.set(x).subLocal(a)

            // Apply incremental impulse
            // Vec2 P1 = d.x * normal;
            // Vec2 P2 = d.y * normal;
            P1.set(normal).mulLocal(d.x)
            P2.set(normal).mulLocal(d.y)

            /*
             * vA -= invMassA * (P1 + P2); wA -= invIA * (Cross(cp1.rA, P1) + Cross(cp2.rA, P2));
             *
             * vB += invMassB * (P1 + P2); wB += invIB * (Cross(cp1.rB, P1) + Cross(cp2.rB, P2));
             */
            temp1.set(P1).addLocal(P2)
            temp2.set(temp1).mulLocal(mA)
            vA.subLocal(temp2)
            temp2.set(temp1).mulLocal(mB)
            vB.addLocal(temp2)
            wA -= iA * (Vec2.cross(cp1.rA, P1) + Vec2.cross(cp2.rA, P2))
            wB += iB * (Vec2.cross(cp1.rB, P1) + Vec2.cross(cp2.rB, P2))

            // Accumulate
            cp1.normalImpulse = x.x
            cp2.normalImpulse = x.y

            /*
             * #if B2_DEBUG_SOLVER == 1 // Postconditions dv1 = vB + Cross(wB, cp1.rB) - vA -
             * Cross(wA, cp1.rA); dv2 = vB + Cross(wB, cp2.rB) - vA - Cross(wA, cp2.rA);
             *
             * // Compute normal velocity vn1 = Dot(dv1, normal); vn2 = Dot(dv2, normal);
             *
             * assert(Abs(vn1 - cp1.velocityBias) < k_errorTol); assert(Abs(vn2 - cp2.velocityBias)
             * < k_errorTol); #endif
             */
            if (DEBUG_SOLVER) {
              // Postconditions
              val dv1 = vB.add(Vec2.cross(wB, cp1.rB).subLocal(vA).subLocal(Vec2.cross(wA, cp1.rA)))
              val dv2 = vB.add(Vec2.cross(wB, cp2.rB).subLocal(vA).subLocal(Vec2.cross(wA, cp2.rA)))
              // Compute normal velocity
              @Suppress("UNUSED_VALUE")
              vn1 = Vec2.dot(dv1, normal)
              @Suppress("UNUSED_VALUE")
              vn2 = Vec2.dot(dv2, normal)
              // assert is not supported in KMP.
              // assert(MathUtils.abs(vn1 - cp1.velocityBias) < k_errorTol)
              // assert(MathUtils.abs(vn2 - cp2.velocityBias) < k_errorTol)
            }
            break
          }

          //
          // Case 2: vn1 = 0 and x2 = 0
          //
          // 0 = a11 * x1' + a12 * 0 + b1'
          // vn2 = a21 * x1' + a22 * 0 + '
          //
          x.x = -cp1.normalMass * b.x
          x.y = 0.0f
          @Suppress("UNUSED_VALUE")
          vn1 = 0.0f
          vn2 = vc.K.ex.y * x.x + b.y
          if (x.x >= 0.0f && vn2 >= 0.0f) {
            // System.out.println("case 2");
            // Get the incremental impulse
            d.set(x).subLocal(a)

            // Apply incremental impulse
            // Vec2 P1 = d.x * normal;
            // Vec2 P2 = d.y * normal;
            P1.set(normal).mulLocal(d.x)
            P2.set(normal).mulLocal(d.y)

            /*
             * Vec2 P1 = d.x * normal; Vec2 P2 = d.y * normal; vA -= invMassA * (P1 + P2); wA -=
             * invIA * (Cross(cp1.rA, P1) + Cross(cp2.rA, P2));
             *
             * vB += invMassB * (P1 + P2); wB += invIB * (Cross(cp1.rB, P1) + Cross(cp2.rB, P2));
             */
            temp1.set(P1).addLocal(P2)
            temp2.set(temp1).mulLocal(mA)
            vA.subLocal(temp2)
            temp2.set(temp1).mulLocal(mB)
            vB.addLocal(temp2)
            wA -= iA * (Vec2.cross(cp1.rA, P1) + Vec2.cross(cp2.rA, P2))
            wB += iB * (Vec2.cross(cp1.rB, P1) + Vec2.cross(cp2.rB, P2))

            // Accumulate
            cp1.normalImpulse = x.x
            cp2.normalImpulse = x.y

            /*
             * #if B2_DEBUG_SOLVER == 1 // Postconditions dv1 = vB + Cross(wB, cp1.rB) - vA -
             * Cross(wA, cp1.rA);
             *
             * // Compute normal velocity vn1 = Dot(dv1, normal);
             *
             * assert(Abs(vn1 - cp1.velocityBias) < k_errorTol); #endif
             */
            if (DEBUG_SOLVER) {
              // Postconditions
              val dv1 = vB.add(Vec2.cross(wB, cp1.rB).subLocal(vA).subLocal(Vec2.cross(wA, cp1.rA)))
              // Compute normal velocity
              @Suppress("UNUSED_VALUE")
              vn1 = Vec2.dot(dv1, normal)
              // assert is not supported in KMP.
              // assert(MathUtils.abs(vn1 - cp1.velocityBias) < k_errorTol)
            }
            break
          }

          //
          // Case 3: wB = 0 and x1 = 0
          //
          // vn1 = a11 * 0 + a12 * x2' + b1'
          // 0 = a21 * 0 + a22 * x2' + '
          //
          x.x = 0.0f
          x.y = -cp2.normalMass * b.y
          vn1 = vc.K.ey.x * x.y + b.x
          @Suppress("UNUSED_VALUE")
          vn2 = 0.0f
          if (x.y >= 0.0f && vn1 >= 0.0f) {
            // System.out.println("case 3");
            // Resubstitute for the incremental impulse
            d.set(x).subLocal(a)

            // Apply incremental impulse
            /*
             * Vec2 P1 = d.x * normal; Vec2 P2 = d.y * normal; vA -= invMassA * (P1 + P2); wA -=
             * invIA * (Cross(cp1.rA, P1) + Cross(cp2.rA, P2));
             *
             * vB += invMassB * (P1 + P2); wB += invIB * (Cross(cp1.rB, P1) + Cross(cp2.rB, P2));
             */

            P1.set(normal).mulLocal(d.x)
            P2.set(normal).mulLocal(d.y)
            temp1.set(P1).addLocal(P2)
            temp2.set(temp1).mulLocal(mA)
            vA.subLocal(temp2)
            temp2.set(temp1).mulLocal(mB)
            vB.addLocal(temp2)
            wA -= iA * (Vec2.cross(cp1.rA, P1) + Vec2.cross(cp2.rA, P2))
            wB += iB * (Vec2.cross(cp1.rB, P1) + Vec2.cross(cp2.rB, P2))

            // Accumulate
            cp1.normalImpulse = x.x
            cp2.normalImpulse = x.y

            /*
             * #if B2_DEBUG_SOLVER == 1 // Postconditions dv2 = vB + Cross(wB, cp2.rB) - vA -
             * Cross(wA, cp2.rA);
             *
             * // Compute normal velocity vn2 = Dot(dv2, normal);
             *
             * assert(Abs(vn2 - cp2.velocityBias) < k_errorTol); #endif
             */
            if (DEBUG_SOLVER) {
              // Postconditions
              val dv2 = vB.add(Vec2.cross(wB, cp2.rB).subLocal(vA).subLocal(Vec2.cross(wA, cp2.rA)))
              // Compute normal velocity
              @Suppress("UNUSED_VALUE")
              vn2 = Vec2.dot(dv2, normal)
              // assert is not supported in KMP.
              // assert(MathUtils.abs(vn2 - cp2.velocityBias) < k_errorTol)
            }
            break
          }

          //
          // Case 4: x1 = 0 and x2 = 0
          //
          // vn1 = b1
          // vn2 = ;
          x.x = 0.0f
          x.y = 0.0f
          vn1 = b.x
          vn2 = b.y
          if (vn1 >= 0.0f && vn2 >= 0.0f) {
            // System.out.println("case 4");
            // Resubstitute for the incremental impulse
            d.set(x).subLocal(a)

            // Apply incremental impulse
            /*
             * Vec2 P1 = d.x * normal; Vec2 P2 = d.y * normal; vA -= invMassA * (P1 + P2); wA -=
             * invIA * (Cross(cp1.rA, P1) + Cross(cp2.rA, P2));
             *
             * vB += invMassB * (P1 + P2); wB += invIB * (Cross(cp1.rB, P1) + Cross(cp2.rB, P2));
             */
            P1.set(normal).mulLocal(d.x)
            P2.set(normal).mulLocal(d.y)
            temp1.set(P1).addLocal(P2)
            temp2.set(temp1).mulLocal(mA)
            vA.subLocal(temp2)
            temp2.set(temp1).mulLocal(mB)
            vB.addLocal(temp2)
            wA -= iA * (Vec2.cross(cp1.rA, P1) + Vec2.cross(cp2.rA, P2))
            wB += iB * (Vec2.cross(cp1.rB, P1) + Vec2.cross(cp2.rB, P2))

            // Accumulate
            cp1.normalImpulse = x.x
            cp2.normalImpulse = x.y
            break
          }

          // No solution, give up. This is hit sometimes, but it doesn't seem to matter.
          break
        }
      }

      // m_velocities[indexA].v.set(vA);
      velocities[indexA].w = wA
      // m_velocities[indexB].v.set(vB);
      velocities[indexB].w = wB
    }
  }

  fun storeImpulses() {
    for (i in 0 until count) {
      val vc = velocityConstraints[i]
      val manifold = contacts[vc.contactIndex]!!.manifold
      for (j in 0 until vc.pointCount) {
        manifold.points[j].normalImpulse = vc.points[j].normalImpulse
        manifold.points[j].tangentImpulse = vc.points[j].tangentImpulse
      }
    }
  }

  /*
   * #if 0 // Sequential solver. bool ContactSolver::SolvePositionConstraints(float baumgarte) {
   * float minSeparation = 0.0f;
   *
   * for (int i = 0; i < m_constraintCount; ++i) { ContactConstraint* c = m_constraints + i; Body*
   * bodyA = c.bodyA; Body* bodyB = c.bodyB; float invMassA = bodyA.m_mass * bodyA.m_invMass; float
   * invIA = bodyA.m_mass * bodyA.m_invI; float invMassB = bodyB.m_mass * bodyB.m_invMass; float
   * invIB = bodyB.m_mass * bodyB.m_invI;
   *
   * Vec2 normal = c.normal;
   *
   * // Solve normal constraints for (int j = 0; j < c.pointCount; ++j) { ContactConstraintPoint*
   * ccp = c.points + j;
   *
   * Vec2 r1 = Mul(bodyA.GetXForm().R, ccp.localAnchorA - bodyA.GetLocalCenter()); Vec2 r2 =
   * Mul(bodyB.GetXForm().R, ccp.localAnchorB - bodyB.GetLocalCenter());
   *
   * Vec2 p1 = bodyA.m_sweep.c + r1; Vec2 p2 = bodyB.m_sweep.c + r2; Vec2 dp = p2 - p1;
   *
   * // Approximate the current separation. float separation = Dot(dp, normal) + ccp.separation;
   *
   * // Track max constraint error. minSeparation = Min(minSeparation, separation);
   *
   * // Prevent large corrections and allow slop. float C = Clamp(baumgarte * (separation +
   * _linearSlop), -_maxLinearCorrection, 0.0f);
   *
   * // Compute normal impulse float impulse = -ccp.equalizedMass * C;
   *
   * Vec2 P = impulse * normal;
   *
   * bodyA.m_sweep.c -= invMassA * P; bodyA.m_sweep.a -= invIA * Cross(r1, P);
   * bodyA.SynchronizeTransform();
   *
   * bodyB.m_sweep.c += invMassB * P; bodyB.m_sweep.a += invIB * Cross(r2, P);
   * bodyB.SynchronizeTransform(); } }
   *
   * // We can't expect minSpeparation >= -_linearSlop because we don't // push the separation above
   * -_linearSlop. return minSeparation >= -1.5f * _linearSlop; }
   */

  /** Sequential solver. */
  fun solvePositionConstraints(): Boolean {
    var minSeparation = 0.0f
    for (i in 0 until count) {
      val pc = positionConstraints[i]
      val indexA = pc.indexA
      val indexB = pc.indexB
      val mA = pc.invMassA
      val iA = pc.invIA
      val localCenterA = pc.localCenterA
      val mB = pc.invMassB
      val iB = pc.invIB
      val localCenterB = pc.localCenterB
      val pointCount = pc.pointCount
      val cA = positions[indexA].c
      var aA = positions[indexA].a
      val cB = positions[indexB].c
      var aB = positions[indexB].a

      // Solve normal constraints
      for (j in 0 until pointCount) {
        xfA.q.set(aA)
        xfB.q.set(aB)
        Rot.mulToOutUnsafe(xfA.q, localCenterA, xfA.p)
        xfA.p.negateLocal().addLocal(cA)
        Rot.mulToOutUnsafe(xfB.q, localCenterB, xfB.p)
        xfB.p.negateLocal().addLocal(cB)
        val psm = psolver
        psm.initialize(pc, xfA, xfB, j)
        val normal = psm.normal
        val point = psm.point
        val separation = psm.separation
        rA.set(point).subLocal(cA)
        rB.set(point).subLocal(cB)

        // Track max constraint error.
        minSeparation = MathUtils.min(minSeparation, separation)

        // Prevent large corrections and allow slop.
        val C =
          MathUtils.clamp(
            Settings.BAUGARTE * (separation + Settings.LINEAR_SLOP),
            -Settings.MAX_LINEAR_CORRECTION,
            0.0f
          )

        // Compute the effective mass.
        val rnA = Vec2.cross(rA, normal)
        val rnB = Vec2.cross(rB, normal)
        val K = mA + mB + iA * rnA * rnA + iB * rnB * rnB

        // Compute normal impulse
        val impulse = if (K > 0.0f) -C / K else 0.0f
        P.set(normal).mulLocal(impulse)
        cA.subLocal(temp.set(P).mulLocal(mA))
        aA -= iA * Vec2.cross(rA, P)
        cB.addLocal(temp.set(P).mulLocal(mB))
        aB += iB * Vec2.cross(rB, P)
      }

      // m_positions[indexA].c.set(cA);
      positions[indexA].a = aA

      // m_positions[indexB].c.set(cB);
      positions[indexB].a = aB
    }

    // We can't expect minSpeparation >= -linearSlop because we don't
    // push the separation above -linearSlop.
    return minSeparation >= -3.0f * Settings.LINEAR_SLOP
  }

  // Sequential position solver for position constraints.
  fun solveTOIPositionConstraints(toiIndexA: Int, toiIndexB: Int): Boolean {
    var minSeparation = 0.0f
    for (i in 0 until count) {
      val pc = positionConstraints[i]
      val indexA = pc.indexA
      val indexB = pc.indexB
      val localCenterA = pc.localCenterA
      val localCenterB = pc.localCenterB
      val pointCount = pc.pointCount
      var mA = 0.0f
      var iA = 0.0f
      if (indexA == toiIndexA || indexA == toiIndexB) {
        mA = pc.invMassA
        iA = pc.invIA
      }
      var mB = 0f
      var iB = 0f
      if (indexB == toiIndexA || indexB == toiIndexB) {
        mB = pc.invMassB
        iB = pc.invIB
      }
      val cA = positions[indexA].c
      var aA = positions[indexA].a
      val cB = positions[indexB].c
      var aB = positions[indexB].a

      // Solve normal constraints
      for (j in 0 until pointCount) {
        xfA.q.set(aA)
        xfB.q.set(aB)
        Rot.mulToOutUnsafe(xfA.q, localCenterA, xfA.p)
        xfA.p.negateLocal().addLocal(cA)
        Rot.mulToOutUnsafe(xfB.q, localCenterB, xfB.p)
        xfB.p.negateLocal().addLocal(cB)
        val psm = psolver
        psm.initialize(pc, xfA, xfB, j)
        val normal = psm.normal
        val point = psm.point
        val separation = psm.separation
        rA.set(point).subLocal(cA)
        rB.set(point).subLocal(cB)

        // Track max constraint error.
        minSeparation = MathUtils.min(minSeparation, separation)

        // Prevent large corrections and allow slop.
        val C =
          MathUtils.clamp(
            Settings.TOI_BAUGARTE * (separation + Settings.LINEAR_SLOP),
            -Settings.MAX_LINEAR_CORRECTION,
            0.0f
          )

        // Compute the effective mass.
        val rnA = Vec2.cross(rA, normal)
        val rnB = Vec2.cross(rB, normal)
        val K = mA + mB + iA * rnA * rnA + iB * rnB * rnB

        // Compute normal impulse
        val impulse = if (K > 0.0f) -C / K else 0.0f
        P.set(normal).mulLocal(impulse)
        cA.subLocal(temp.set(P).mulLocal(mA))
        aA -= iA * Vec2.cross(rA, P)
        cB.addLocal(temp.set(P).mulLocal(mB))
        aB += iB * Vec2.cross(rB, P)
      }

      // m_positions[indexA].c.set(cA);
      positions[indexA].a = aA

      // m_positions[indexB].c.set(cB);
      positions[indexB].a = aB
    }

    // We can't expect minSpeparation >= -_linearSlop because we don't
    // push the separation above -_linearSlop.
    return minSeparation >= -1.5f * Settings.LINEAR_SLOP
  }

  class ContactSolverDef {
    lateinit var step: TimeStep
    lateinit var contacts: Array<Contact?>
    var count = 0
    lateinit var positions: Array<Position>
    lateinit var velocities: Array<Velocity>
  }

  companion object {
    const val DEBUG_SOLVER = false
    const val K_ERROR_TOL = 1e-3f

    /**
     * For each solver, this is the initial number of constraints in the array, which expands as
     * needed.
     */
    const val INITIAL_NUM_CONSTRAINTS = 256

    /** Ensure a reasonable condition number. for the block solver */
    const val k_maxConditionNumber = 100.0f
  }
}

internal class PositionSolverManifold {
  val normal = Vec2()
  val point = Vec2()
  var separation = 0f

  fun initialize(pc: ContactPositionConstraint, xfA: Transform, xfB: Transform, index: Int) {
    // assert is not supported in KMP.
    // assert(pc!!.pointCount > 0)
    val xfAq = xfA.q
    val xfBq = xfB.q
    val pcLocalPointsI = pc.localPoints[index]
    when (pc.type) {
      Manifold.ManifoldType.CIRCLES -> {

        // Transform.mulToOutUnsafe(xfA, pc.localPoint, pointA);
        // Transform.mulToOutUnsafe(xfB, pc.localPoints[0], pointB);
        // normal.set(pointB).subLocal(pointA);
        // normal.normalize();
        //
        // point.set(pointA).addLocal(pointB).mulLocal(.5f);
        // temp.set(pointB).subLocal(pointA);
        // separation = Vec2.dot(temp, normal) - pc.radiusA - pc.radiusB;
        val plocalPoint = pc.localPoint
        val pLocalPoints0 = pc.localPoints[0]
        val pointAx: Float = xfAq.cos * plocalPoint.x - xfAq.sin * plocalPoint.y + xfA.p.x
        val pointAy: Float = xfAq.sin * plocalPoint.x + xfAq.cos * plocalPoint.y + xfA.p.y
        val pointBx: Float = xfBq.cos * pLocalPoints0.x - xfBq.sin * pLocalPoints0.y + xfB.p.x
        val pointBy: Float = xfBq.sin * pLocalPoints0.x + xfBq.cos * pLocalPoints0.y + xfB.p.y
        normal.x = pointBx - pointAx
        normal.y = pointBy - pointAy
        normal.normalize()
        point.x = (pointAx + pointBx) * .5f
        point.y = (pointAy + pointBy) * .5f
        val tempx = pointBx - pointAx
        val tempy = pointBy - pointAy
        separation = tempx * normal.x + tempy * normal.y - pc.radiusA - pc.radiusB
      }
      Manifold.ManifoldType.FACE_A -> {

        // Rot.mulToOutUnsafe(xfAq, pc.localNormal, normal);
        // Transform.mulToOutUnsafe(xfA, pc.localPoint, planePoint);
        //
        // Transform.mulToOutUnsafe(xfB, pc.localPoints[index], clipPoint);
        // temp.set(clipPoint).subLocal(planePoint);
        // separation = Vec2.dot(temp, normal) - pc.radiusA - pc.radiusB;
        // point.set(clipPoint);
        val pcLocalNormal = pc.localNormal
        val pcLocalPoint = pc.localPoint
        normal.x = xfAq.cos * pcLocalNormal.x - xfAq.sin * pcLocalNormal.y
        normal.y = xfAq.sin * pcLocalNormal.x + xfAq.cos * pcLocalNormal.y
        val planePointx: Float = xfAq.cos * pcLocalPoint.x - xfAq.sin * pcLocalPoint.y + xfA.p.x
        val planePointy: Float = xfAq.sin * pcLocalPoint.x + xfAq.cos * pcLocalPoint.y + xfA.p.y
        val clipPointx: Float = xfBq.cos * pcLocalPointsI.x - xfBq.sin * pcLocalPointsI.y + xfB.p.x
        val clipPointy: Float = xfBq.sin * pcLocalPointsI.x + xfBq.cos * pcLocalPointsI.y + xfB.p.y
        val tempx = clipPointx - planePointx
        val tempy = clipPointy - planePointy
        separation = tempx * normal.x + tempy * normal.y - pc.radiusA - pc.radiusB
        point.x = clipPointx
        point.y = clipPointy
      }
      Manifold.ManifoldType.FACE_B -> {

        // Rot.mulToOutUnsafe(xfBq, pc.localNormal, normal);
        // Transform.mulToOutUnsafe(xfB, pc.localPoint, planePoint);
        //
        // Transform.mulToOutUnsafe(xfA, pcLocalPointsI, clipPoint);
        // temp.set(clipPoint).subLocal(planePoint);
        // separation = Vec2.dot(temp, normal) - pc.radiusA - pc.radiusB;
        // point.set(clipPoint);
        //
        // // Ensure normal points from A to B
        // normal.negateLocal();
        val pcLocalNormal = pc.localNormal
        val pcLocalPoint = pc.localPoint
        normal.x = xfBq.cos * pcLocalNormal.x - xfBq.sin * pcLocalNormal.y
        normal.y = xfBq.sin * pcLocalNormal.x + xfBq.cos * pcLocalNormal.y
        val planePointx: Float = xfBq.cos * pcLocalPoint.x - xfBq.sin * pcLocalPoint.y + xfB.p.x
        val planePointy: Float = xfBq.sin * pcLocalPoint.x + xfBq.cos * pcLocalPoint.y + xfB.p.y
        val clipPointx: Float = xfAq.cos * pcLocalPointsI.x - xfAq.sin * pcLocalPointsI.y + xfA.p.x
        val clipPointy: Float = xfAq.sin * pcLocalPointsI.x + xfAq.cos * pcLocalPointsI.y + xfA.p.y
        val tempx = clipPointx - planePointx
        val tempy = clipPointy - planePointy
        separation = tempx * normal.x + tempy * normal.y - pc.radiusA - pc.radiusB
        point.x = clipPointx
        point.y = clipPointy
        normal.x *= -1f
        normal.y *= -1f
      }
    }
  }
}
