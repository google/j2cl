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

import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.Body
import org.jbox2d.dynamics.SolverData
import org.jbox2d.dynamics.World
import org.jbox2d.pooling.IWorldPool

// updated to rev 100
/**
 * The base joint class. Joints are used to constrain two bodies together in various fashions. Some
 * joints also feature limits and motors.
 *
 * @author Daniel Murphy
 */
abstract class Joint protected constructor(worldPool: IWorldPool, def: JointDef) {

  val m_type: JointType
  var m_prev: Joint?

  /** get the next joint the world joint list. */
  var m_next: Joint?
  var m_edgeA: JointEdge
  var m_edgeB: JointEdge

  /** get the first body attached to this joint. */
  var m_bodyA: Body?
    protected set

  /**
   * get the second body attached to this joint.
   *
   * @return
   */
  var m_bodyB: Body?
    protected set
  var m_islandFlag: Boolean

  // / Get collide connected.
  // / Note: modifying the collide connect flag won't work correctly because
  // / the flag is only checked when fixture AABBs begin to overlap.
  public val m_collideConnected: Boolean
  /** get the user data pointer. */
  /** Set the user data pointer. */
  var m_userData: Any?
  protected var pool: IWorldPool

  // Cache here per time step to reduce cache misses.
  // final Vec2 m_localCenterA, m_localCenterB;
  // float m_invMassA, m_invIA;
  // float m_invMassB, m_invIB;
  init {
    // assert is not supported in KMP.
    // assert(def.bodyA != def.bodyB)
    pool = worldPool
    m_type = def.type
    m_prev = null
    m_next = null
    m_bodyA = def.bodyA
    m_bodyB = def.bodyB
    m_collideConnected = def.collideConnected
    m_islandFlag = false
    m_userData = def.userData
    m_edgeA = JointEdge()
    m_edgeA.joint = null
    m_edgeA.other = null
    m_edgeA.prev = null
    m_edgeA.next = null
    m_edgeB = JointEdge()
    m_edgeB.joint = null
    m_edgeB.other = null
    m_edgeB.prev = null
    m_edgeB.next = null

    // m_localCenterA = new Vec2();
    // m_localCenterB = new Vec2();
  }

  /**
   * get the type of the concrete joint.
   *
   * @return
   */
  fun getType(): JointType {
    return m_type
  }

  /** get the first body attached to this joint. */
  fun getBodyA(): Body? {
    return m_bodyA
  }

  /**
   * get the second body attached to this joint.
   *
   * @return
   */
  fun getBodyB(): Body? {
    return m_bodyB
  }

  /**
   * get the anchor point on bodyA in world coordinates.
   *
   * @return
   */
  abstract fun getAnchorA(out: Vec2)

  /**
   * get the anchor point on bodyB in world coordinates.
   *
   * @return
   */
  abstract fun getAnchorB(out: Vec2)

  /**
   * get the reaction force on body2 at the joint anchor in Newtons.
   *
   * @param inv_dt
   * @return
   */
  abstract fun getReactionForce(inv_dt: Float, out: Vec2)

  /**
   * get the reaction torque on body2 in N*m.
   *
   * @param inv_dt
   * @return
   */
  abstract fun getReactionTorque(inv_dt: Float): Float

  /** get the next joint the world joint list. */
  fun getNext(): Joint? {
    return m_next
  }

  /** get the user data pointer. */
  fun getUserData(): Any? {
    return m_userData
  }

  /** Set the user data pointer. */
  fun setUserData(data: Any) {
    m_userData = data
  }

  // / Get collide connected.
  // / Note: modifying the collide connect flag won't work correctly because
  // / the flag is only checked when fixture AABBs begin to overlap.
  fun getCollideConnected(): Boolean {
    return m_collideConnected
  }

  /**
   * Short-cut function to determine if either body is inactive.
   *
   * @return
   */
  fun isActive(): Boolean {
    return m_bodyA!!.isActive() && m_bodyB!!.isActive()
  }

  abstract fun initVelocityConstraints(data: SolverData)
  abstract fun solveVelocityConstraints(data: SolverData)

  /**
   * This returns true if the position errors are within tolerance.
   *
   * @param baumgarte
   * @return
   */
  abstract fun solvePositionConstraints(data: SolverData): Boolean

  /** Override to handle destruction of joint */
  open fun destructor() {}

  companion object {
    fun create(world: World, def: JointDef): Joint? {
      // Joint joint = null;
      @Suppress("REDUNDANT_ELSE_IN_WHEN")
      return when (def.type) {
        JointType.MOUSE -> MouseJoint(world.pool, def as MouseJointDef)
        JointType.DISTANCE -> DistanceJoint(world.pool, def as DistanceJointDef)
        JointType.PRISMATIC -> PrismaticJoint(world.pool, def as PrismaticJointDef)
        JointType.REVOLUTE -> RevoluteJoint(world.pool, def as RevoluteJointDef)
        JointType.WELD -> WeldJoint(world.pool, def as WeldJointDef)
        JointType.FRICTION -> FrictionJoint(world.pool, def as FrictionJointDef)
        JointType.WHEEL -> WheelJoint(world.pool, def as WheelJointDef)
        JointType.GEAR -> GearJoint(world.pool, def as GearJointDef)
        JointType.PULLEY -> PulleyJoint(world.pool, def as PulleyJointDef)
        JointType.CONSTANT_VOLUME -> ConstantVolumeJoint(world, def as ConstantVolumeJointDef)
        JointType.ROPE -> RopeJoint(world.pool, def as RopeJointDef)
        JointType.UNKNOWN -> null
        else -> null
      }
    }

    fun destroy(joint: Joint) {
      joint.destructor()
    }
  }
}
