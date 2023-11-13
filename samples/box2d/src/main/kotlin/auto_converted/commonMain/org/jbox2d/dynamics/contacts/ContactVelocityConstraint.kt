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

import org.jbox2d.common.Mat22
import org.jbox2d.common.Settings
import org.jbox2d.common.Vec2

class ContactVelocityConstraint {
  var points =
    Array<VelocityConstraintPoint>(Settings.maxManifoldPoints) { VelocityConstraintPoint() }
  val normal = Vec2()
  val normalMass = Mat22()
  val K = Mat22()
  var indexA = 0
  var indexB = 0
  var invMassA = 0f
  var invMassB = 0f
  var invIA = 0f
  var invIB = 0f
  var friction = 0f
  var restitution = 0f
  var tangentSpeed = 0f
  var pointCount = 0
  var contactIndex = 0

  class VelocityConstraintPoint {
    val rA = Vec2()
    val rB = Vec2()
    var normalImpulse = 0f
    var tangentImpulse = 0f
    var normalMass = 0f
    var tangentMass = 0f
    var velocityBias = 0f
  }
}
