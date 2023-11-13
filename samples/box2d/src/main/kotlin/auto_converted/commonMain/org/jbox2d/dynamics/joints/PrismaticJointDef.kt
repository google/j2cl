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

/**
 * Prismatic joint definition. This requires defining a line of motion using an axis and an anchor
 * point. The definition uses local anchor points and a local axis so that the initial configuration
 * can violate the constraint slightly. The joint translation is zero when the local anchor points
 * coincide in world space. Using local anchors and a local axis helps when saving and loading a
 * game.
 *
 * @author Daniel
 * @warning at least one body should by dynamic with a non-fixed rotation.
 */
class PrismaticJointDef : JointDef() {
  /** The local anchor point relative to body1's origin. */
  val localAnchorA: Vec2

  /** The local anchor point relative to body2's origin. */
  val localAnchorB: Vec2

  /** The local translation axis in body1. */
  val localAxisA: Vec2

  /** The constrained angle between the bodies: body2_angle - body1_angle. */
  var referenceAngle: Float

  /** Enable/disable the joint limit. */
  var enableLimit: Boolean

  /** The lower translation limit, usually in meters. */
  var lowerTranslation: Float

  /** The upper translation limit, usually in meters. */
  var upperTranslation: Float

  /** Enable/disable the joint motor. */
  var enableMotor: Boolean

  /** The maximum motor torque, usually in N-m. */
  var maxMotorForce: Float

  /** The desired motor speed in radians per second. */
  var motorSpeed: Float

  init {
    type = JointType.PRISMATIC
    localAnchorA = Vec2()
    localAnchorB = Vec2()
    localAxisA = Vec2(1.0f, 0.0f)
    referenceAngle = 0.0f
    enableLimit = false
    lowerTranslation = 0.0f
    upperTranslation = 0.0f
    enableMotor = false
    maxMotorForce = 0.0f
    motorSpeed = 0.0f
  }

  /**
   * Initialize the bodies, anchors, axis, and reference angle using the world anchor and world
   * axis.
   */
  fun initialize(b1: Body, b2: Body, anchor: Vec2, axis: Vec2) {
    bodyA = b1
    bodyB = b2
    bodyA!!.getLocalPointToOut(anchor, localAnchorA)
    bodyB!!.getLocalPointToOut(anchor, localAnchorB)
    bodyA!!.getLocalVectorToOut(axis, localAxisA)
    referenceAngle = bodyB!!.getAngle() - bodyA!!.getAngle()
  }
}
