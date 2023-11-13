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

import org.jbox2d.callbacks.ContactListener
import org.jbox2d.collision.Manifold
import org.jbox2d.collision.WorldManifold
import org.jbox2d.common.MathUtils
import org.jbox2d.common.Transform
import org.jbox2d.dynamics.Fixture
import org.jbox2d.pooling.IWorldPool

/**
 * The class manages contact between two shapes. A contact exists for each overlapping AABB in the
 * broad-phase (except if filtered). Therefore a contact object may exist that has no contact
 * points.
 *
 * @author daniel
 */
abstract class Contact protected constructor(argPool: IWorldPool) {
  var m_flags = 0

  // World pool and list pointers.
  var m_prev: Contact? = null

  /**
   * Get the next contact in the world's contact list.
   *
   * @return
   */
  var m_next: Contact? = null

  // Nodes for connecting bodies.
  var m_nodeA: ContactEdge
  var m_nodeB: ContactEdge

  var m_fixtureA: Fixture? = null
  var m_fixtureB: Fixture? = null
  var m_indexA = 0
  var m_indexB = 0

  val m_manifold: Manifold
  var m_toiCount = 0f
  var m_toi = 0f
  var m_friction = 0f
  var m_restitution = 0f
  var m_tangentSpeed = 0f
  protected val pool: IWorldPool

  init {
    m_nodeA = ContactEdge()
    m_nodeB = ContactEdge()
    m_manifold = Manifold()
    pool = argPool
  }

  /** initialization for pooling */
  open fun init(fA: Fixture, indexA: Int, fB: Fixture, indexB: Int) {
    m_flags = 0
    m_fixtureA = fA
    m_fixtureB = fB
    m_indexA = indexA
    m_indexB = indexB
    m_manifold.pointCount = 0
    m_prev = null
    m_next = null
    m_nodeA.contact = null
    m_nodeA.prev = null
    m_nodeA.next = null
    m_nodeA.other = null
    m_nodeB.contact = null
    m_nodeB.prev = null
    m_nodeB.next = null
    m_nodeB.other = null
    m_toiCount = 0f
    m_friction = mixFriction(fA.m_friction, fB.m_friction)
    m_restitution = mixRestitution(fA.m_restitution, fB.m_restitution)
    m_tangentSpeed = 0f
  }

  /** Get the contact manifold. Do not set the point count to zero. Instead call Disable. */
  fun getManifold(): Manifold {
    return m_manifold
  }

  /** Get the world manifold. */
  fun getWorldManifold(worldManifold: WorldManifold) {
    val bodyA = m_fixtureA!!.m_body
    val bodyB = m_fixtureB!!.m_body
    val shapeA = m_fixtureA!!.m_shape
    val shapeB = m_fixtureB!!.m_shape
    worldManifold.initialize(
      m_manifold,
      bodyA!!.m_xf,
      shapeA!!.m_radius,
      bodyB!!.m_xf,
      shapeB!!.m_radius
    )
  }

  /**
   * Is this contact touching
   *
   * @return
   */
  fun isTouching(): Boolean {
    return m_flags and TOUCHING_FLAG == TOUCHING_FLAG
  }

  /**
   * Enable/disable this contact. This can be used inside the pre-solve contact listener. The
   * contact is only disabled for the current time step (or sub-step in continuous collisions).
   *
   * @param flag
   */
  fun setEnabled(flag: Boolean) {
    m_flags =
      if (flag) {
        m_flags or ENABLED_FLAG
      } else {
        m_flags and ENABLED_FLAG.inv()
      }
  }

  /**
   * Has this contact been disabled?
   *
   * @return
   */
  fun isEnabled(): Boolean {
    return m_flags and ENABLED_FLAG == ENABLED_FLAG
  }

  /**
   * Get the next contact in the world's contact list.
   *
   * @return
   */
  fun getNext(): Contact? {
    return m_next
  }

  /**
   * Get the first fixture in this contact.
   *
   * @return
   */
  fun getFixtureA(): Fixture? {
    return m_fixtureA
  }

  fun getChildIndexA(): Int {
    return m_indexA
  }

  /**
   * Get the second fixture in this contact.
   *
   * @return
   */
  fun getFixtureB(): Fixture? {
    return m_fixtureB
  }

  fun getChildIndexB(): Int {
    return m_indexB
  }

  fun setFriction(friction: Float) {
    m_friction = friction
  }

  fun getFriction(): Float {
    return m_friction
  }

  fun resetFriction() {
    m_friction = mixFriction(m_fixtureA!!.m_friction, m_fixtureB!!.m_friction)
  }

  fun setRestitution(restitution: Float) {
    m_restitution = restitution
  }

  fun getRestitution(): Float {
    return m_restitution
  }

  fun resetRestitution() {
    m_restitution = mixRestitution(m_fixtureA!!.m_restitution, m_fixtureB!!.m_restitution)
  }

  fun setTangentSpeed(speed: Float) {
    m_tangentSpeed = speed
  }

  fun getTangentSpeed(): Float {
    return m_tangentSpeed
  }

  abstract fun evaluate(manifold: Manifold, xfA: Transform, xfB: Transform)

  /** Flag this contact for filtering. Filtering will occur the next time step. */
  fun flagForFiltering() {
    m_flags = m_flags or FILTER_FLAG
  }

  // djm pooling
  private val oldManifold = Manifold()

  fun update(listener: ContactListener?) {
    oldManifold.set(m_manifold)

    // Re-enable this contact.
    m_flags = m_flags or ENABLED_FLAG
    @Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER") var touching = false
    val wasTouching = m_flags and TOUCHING_FLAG == TOUCHING_FLAG
    val sensorA = m_fixtureA!!.isSensor()
    val sensorB = m_fixtureB!!.isSensor()
    val sensor = sensorA || sensorB
    val bodyA = m_fixtureA!!.m_body
    val bodyB = m_fixtureB!!.m_body
    val xfA = bodyA!!.m_xf
    val xfB = bodyB!!.m_xf
    // log.debug("TransformA: "+xfA);
    // log.debug("TransformB: "+xfB);
    if (sensor) {
      val shapeA = m_fixtureA!!.m_shape!!
      val shapeB = m_fixtureB!!.m_shape!!
      touching = pool.getCollision().testOverlap(shapeA, m_indexA, shapeB, m_indexB, xfA, xfB)

      // Sensors don't generate manifolds.
      m_manifold.pointCount = 0
    } else {
      evaluate(m_manifold, xfA, xfB)
      touching = m_manifold.pointCount > 0

      // Match old contact ids to new contact ids and copy the
      // stored impulses to warm start the solver.
      for (i in 0 until m_manifold.pointCount) {
        val mp2 = m_manifold.points[i]
        mp2.normalImpulse = 0.0f
        mp2.tangentImpulse = 0.0f
        val id2 = mp2.id
        for (j in 0 until oldManifold.pointCount) {
          val mp1 = oldManifold.points[j]
          if (mp1.id.isEqual(id2)) {
            mp2.normalImpulse = mp1.normalImpulse
            mp2.tangentImpulse = mp1.tangentImpulse
            break
          }
        }
      }
      if (touching != wasTouching) {
        bodyA.setAwake(true)
        bodyB.setAwake(true)
      }
    }
    m_flags =
      if (touching) {
        m_flags or TOUCHING_FLAG
      } else {
        m_flags and TOUCHING_FLAG.inv()
      }
    if (listener == null) {
      return
    }
    if (wasTouching == false && touching == true) {
      listener.beginContact(this)
    }
    if (wasTouching == true && touching == false) {
      listener.endContact(this)
    }
    if (sensor == false && touching) {
      listener.preSolve(this, oldManifold)
    }
  }

  companion object {
    // Flags stored in m_flags
    // Used when crawling contact graph when forming islands.
    const val ISLAND_FLAG = 0x0001

    // Set when the shapes are touching.
    const val TOUCHING_FLAG = 0x0002 // NO_UCD

    // This contact can be disabled (by user)
    const val ENABLED_FLAG = 0x0004

    // This contact needs filtering because a fixture filter was changed.
    const val FILTER_FLAG = 0x0008

    // This bullet contact had a TOI event
    const val BULLET_HIT_FLAG = 0x0010
    const val TOI_FLAG = 0x0020

    /**
     * Friction mixing law. The idea is to allow either fixture to drive the restitution to zero.
     * For example, anything slides on ice.
     *
     * @param friction1
     * @param friction2
     * @return
     */
    fun mixFriction(friction1: Float, friction2: Float): Float {
      return MathUtils.sqrt(friction1 * friction2)
    }

    /**
     * Restitution mixing law. The idea is allow for anything to bounce off an inelastic surface.
     * For example, a superball bounces on anything.
     *
     * @param restitution1
     * @param restitution2
     * @return
     */
    fun mixRestitution(restitution1: Float, restitution2: Float): Float {
      return if (restitution1 > restitution2) restitution1 else restitution2
    }
  }
}
