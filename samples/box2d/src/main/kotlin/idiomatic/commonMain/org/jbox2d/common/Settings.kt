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
 */
package org.jbox2d.common

import org.jbox2d.common.MathUtils.sqrt

/**
 * Global tuning constants based on MKS units and various integer maximums (vertices per shape,
 * pairs, etc.).
 */
object Settings {
  /** A "close to zero" float epsilon value for use */
  const val EPSILON = 1.1920928955078125E-7f

  /** Pi. */
  const val PI = kotlin.math.PI.toFloat()

  // JBox2D specific settings
  var FAST_ABS = true
  var FAST_FLOOR = true
  var FAST_CEIL = true
  var FAST_ROUND = true
  var FAST_ATAN2 = true
  var CONTACT_STACK_INIT_SIZE = 10
  var SINCOS_LUT_ENABLED = true

  /**
   * smaller the precision, the larger the table. If a small table is used (eg, precision is .006 or
   * greater), make sure you set the table to lerp it's results. Accuracy chart is in the MathUtils
   * source. Or, run the tests yourself in [SinCosTest]. Good lerp precision values:
   * * .0092
   * * .008201
   * * .005904
   * * .005204
   * * .004305
   * * .002807
   * * .001508
   * * 9.32500E-4
   * * 7.48000E-4
   * * 8.47000E-4
   * * .0005095
   * * .0001098
   * * 9.50499E-5
   * * 6.08500E-5
   * * 3.07000E-5
   * * 1.53999E-5
   */
  const val SINCOS_LUT_PRECISION = .00011f
  val SINCOS_LUT_LENGTH = kotlin.math.ceil(kotlin.math.PI * 2 / SINCOS_LUT_PRECISION).toInt()

  /**
   * Use if the table's precision is large (eg .006 or greater). Although it is more expensive, it
   * greatly increases accuracy. Look in the MathUtils source for some test results on the accuracy
   * and speed of lerp vs non lerp. Or, run the tests yourself in [SinCosTest].
   */
  var SINCOS_LUT_LERP = false
  // Collision
  /** The maximum number of contact points between two convex shapes. */
  const val MAX_MANIFOLD_POINTS = 2

  /** The maximum number of vertices on a convex polygon. */
  const val MAX_POLYGON_VERTICES = 8

  /**
   * This is used to fatten AABBs in the dynamic tree. This allows proxies to move by a small amount
   * without triggering a tree adjustment. This is in meters.
   */
  const val AABB_EXTENSION = 0.1f

  /**
   * This is used to fatten AABBs in the dynamic tree. This is used to predict the future position
   * based on the current displacement. This is a dimensionless multiplier.
   */
  const val AABB_MULTIPLIER = 2.0f

  /**
   * A small length used as a collision and constraint tolerance. Usually it is chosen to be
   * numerically significant, but visually insignificant.
   */
  const val LINEAR_SLOP = 0.005f

  /**
   * A small angle used as a collision and constraint tolerance. Usually it is chosen to be
   * numerically significant, but visually insignificant.
   */
  const val ANGULAR_SLOP = 2.0f / 180.0f * PI

  /**
   * The radius of the polygon/edge shape skin. This should not be modified. Making this smaller
   * means polygons will have and insufficient for continuous collision. Making it larger may create
   * artifacts for vertex collision.
   */
  const val POLYGON_RADIUS = 2.0f * LINEAR_SLOP

  /** Maximum number of sub-steps per contact in continuous physics simulation. */
  const val MAX_SUB_STEPS = 8
  // Dynamics
  /** Maximum number of contacts to be handled to solve a TOI island. */
  const val MAX_TOI_CONTACTS = 32

  /**
   * A velocity threshold for elastic collisions. Any collision with a relative linear velocity
   * below this threshold will be treated as inelastic.
   */
  const val VELOCITY_THRESHOLD = 1.0f

  /**
   * The maximum linear position correction used when solving constraints. This helps to prevent
   * overshoot.
   */
  const val MAX_LINEAR_CORRECTION = 0.2f

  /**
   * The maximum angular position correction used when solving constraints. This helps to prevent
   * overshoot.
   */
  const val MAX_ANGULAR_CORRECTION = 8.0f / 180.0f * PI

  /**
   * The maximum linear velocity of a body. This limit is very large and is used to prevent
   * numerical problems. You shouldn't need to adjust this.
   */
  const val MAX_TRANSLATION = 2.0f
  const val MAX_TRANSLATION_SQUARED = MAX_TRANSLATION * MAX_TRANSLATION

  /**
   * The maximum angular velocity of a body. This limit is very large and is used to prevent
   * numerical problems. You shouldn't need to adjust this.
   */
  const val MAX_ROTATION = 0.5f * PI
  var maxRotationSquared = MAX_ROTATION * MAX_ROTATION

  /**
   * This scale factor controls how fast overlap is resolved. Ideally this would be 1 so that
   * overlap is removed in one time step. However using values close to 1 often lead to overshoot.
   */
  const val BAUGARTE = 0.2f
  const val TOI_BAUGARTE = 0.75f
  // Sleep
  /** The time that a body must be still before it will go to sleep. */
  const val TIME_TO_SLEEP = 0.5f

  /** A body cannot sleep if its linear velocity is above this tolerance. */
  const val LINEAR_SLEEP_TOLERANCE = 0.01f

  /** A body cannot sleep if its angular velocity is above this tolerance. */
  const val ANGULAR_SLEEP_TOLERANCE = 2.0f / 180.0f * PI

  /**
   * Friction mixing law. Feel free to customize this. TODO djm: add customization
   *
   * @param friction1
   * @param friction2
   * @return
   */
  fun mixFriction(friction1: Float, friction2: Float): Float = sqrt(friction1 * friction2)

  /**
   * Restitution mixing law. Feel free to customize this. TODO djm: add customization
   *
   * @param restitution1
   * @param restitution2
   * @return
   */
  fun mixRestitution(restitution1: Float, restitution2: Float): Float =
    if (restitution1 > restitution2) restitution1 else restitution2
}
