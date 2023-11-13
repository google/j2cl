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
abstract class Joint protected constructor(protected var pool: IWorldPool, def: JointDef) {

  val type: JointType = def.type
  var prev: Joint? = null

  /** get the next joint the world joint list. */
  var next: Joint? = null
  var edgeA: JointEdge =
    JointEdge().apply {
      joint = null
      other = null
      prev = null
      next = null
    }
  var edgeB: JointEdge =
    JointEdge().apply {
      joint = null
      other = null
      prev = null
      next = null
    }

  var bodyA: Body = def.bodyA
    protected set

  var bodyB: Body = def.bodyB
    protected set
  var islandFlag: Boolean = false

  // / Get collide connected.
  // / Note: modifying the collide connect flag won't work correctly because
  // / the flag is only checked when fixture AABBs begin to overlap.
  val collideConnected: Boolean = def.collideConnected
  /** get the user data pointer. */
  /** Set the user data pointer. */
  var userData: Any = def.userData

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

  /**
   * Short-cut function to determine if either body is inactive.
   *
   * @return
   */
  fun isActive(): Boolean = bodyA.isActive && bodyB.isActive

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
      }
    }

    fun destroy(joint: Joint) {
      joint.destructor()
    }
  }
}
