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
package org.jbox2d.dynamics

import org.jbox2d.collision.AABB
import org.jbox2d.collision.RayCastInput
import org.jbox2d.collision.RayCastOutput
import org.jbox2d.collision.broadphase.BroadPhase
import org.jbox2d.collision.shapes.MassData
import org.jbox2d.collision.shapes.Shape
import org.jbox2d.collision.shapes.ShapeType
import org.jbox2d.common.MathUtils
import org.jbox2d.common.Transform
import org.jbox2d.common.Vec2

/**
 * A fixture is used to attach a shape to a body for collision detection. A fixture inherits its
 * transform from its parent. Fixtures hold additional non-geometric data such as friction,
 * collision filters, etc. Fixtures are created via Body::CreateFixture.
 *
 * @author daniel
 * @warning you cannot reuse fixtures.
 */
class Fixture {
  var m_density = 0f
  var m_next: Fixture? = null

  var m_body: Body? = null

  var m_shape: Shape? = null
  var m_friction = 0f

  var m_restitution = 0f
  var m_proxies: Array<FixtureProxy?>? = null
  var m_proxyCount = 0
  val m_filter: Filter = Filter()
  var m_isSensor = false

  var m_userData: Any? = null

  /**
   * Get the type of the child shape. You can use this to down cast to the concrete shape.
   *
   * @return the shape type.
   */
  fun getType(): ShapeType {
    return m_shape!!.m_type
  }

  /**
   * Get the child shape. You can modify the child shape, however you should not change the number
   * of vertices because this will crash some collision caching mechanisms.
   *
   * @return
   */
  fun getShape(): Shape {
    return m_shape!!
  }

  /**
   * Is this fixture a sensor (non-solid)?
   *
   * @return the true if the shape is a sensor.
   * @return
   */
  fun isSensor(): Boolean {
    return m_isSensor
  }

  /**
   * Set if this fixture is a sensor.
   *
   * @param sensor
   */
  fun setSensor(sensor: Boolean) {
    if (sensor != m_isSensor) {
      m_body!!.setAwake(true)
      m_isSensor = sensor
    }
  }

  /**
   * Set the contact filtering data. This is an expensive operation and should not be called
   * frequently. This will not update contacts until the next time step when either parent body is
   * awake. This automatically calls refilter.
   *
   * @param filter
   */
  fun setFilterData(filter: Filter) {
    m_filter.set(filter)
    refilter()
  }

  /**
   * Get the contact filtering data.
   *
   * @return
   */
  fun getFilterData(): Filter {
    return m_filter
  }

  /**
   * Call this if you want to establish collision that was previously disabled by
   * ContactFilter::ShouldCollide.
   */
  fun refilter() {
    if (m_body == null) {
      return
    }

    // Flag associated contacts for filtering.
    var edge = m_body!!.m_contactList
    while (edge != null) {
      val contact = edge.contact!!
      val fixtureA = contact.m_fixtureA
      val fixtureB = contact.m_fixtureB
      if (fixtureA === this || fixtureB === this) {
        contact.flagForFiltering()
      }
      edge = edge.next
    }
    val world = m_body!!.m_world

    // Touch each proxy so that new pairs may be created
    val broadPhase = world.m_contactManager.m_broadPhase
    for (i in 0 until m_proxyCount) {
      broadPhase.touchProxy(m_proxies!![i]!!.proxyId)
    }
  }

  /**
   * Get the parent body of this fixture. This is NULL if the fixture is not attached.
   *
   * @return the parent body.
   * @return
   */
  fun getBody(): Body {
    return m_body!!
  }

  /**
   * Get the next fixture in the parent body's fixture list.
   *
   * @return the next shape.
   * @return
   */
  fun getNext(): Fixture {
    return m_next!!
  }

  fun setDensity(density: Float) {
    // assert is not supported in KMP.
    // assert(density >= 0f)
    m_density = density
  }

  fun getDensity(): Float {
    return m_density
  }

  /**
   * Get the user data that was assigned in the fixture definition. Use this to store your
   * application specific data.
   *
   * @return
   */
  fun getUserData(): Any {
    return m_userData!!
  }

  /**
   * Set the user data. Use this to store your application specific data.
   *
   * @param data
   */
  fun setUserData(data: Any) {
    m_userData = data
  }

  /**
   * Test a point for containment in this fixture. This only works for convex shapes.
   *
   * @param p a point in world coordinates.
   * @return
   */
  fun testPoint(p: Vec2): Boolean {
    return m_shape!!.testPoint(m_body!!.m_xf, p)
  }

  /**
   * Cast a ray against this shape.
   *
   * @param output the ray-cast results.
   * @param input the ray-cast input parameters.
   * @param output
   * @param input
   */
  fun raycast(output: RayCastOutput, input: RayCastInput, childIndex: Int): Boolean {
    return m_shape!!.raycast(output, input, m_body!!.m_xf, childIndex)
  }

  /**
   * Get the mass data for this fixture. The mass data is based on the density and the shape. The
   * rotational inertia is about the shape's origin.
   *
   * @return
   */
  fun getMassData(massData: MassData) {
    m_shape!!.computeMass(massData, m_density)
  }

  /**
   * Get the coefficient of friction.
   *
   * @return
   */
  fun getFriction(): Float {
    return m_friction
  }

  /**
   * Set the coefficient of friction. This will _not_ change the friction of existing contacts.
   *
   * @param friction
   */
  fun setFriction(friction: Float) {
    m_friction = friction
  }

  /**
   * Get the coefficient of restitution.
   *
   * @return
   */
  fun getRestitution(): Float {
    return m_restitution
  }

  /**
   * Set the coefficient of restitution. This will _not_ change the restitution of existing
   * contacts.
   *
   * @param restitution
   */
  fun setRestitution(restitution: Float) {
    m_restitution = restitution
  }

  /**
   * Get the fixture's AABB. This AABB may be enlarge and/or stale. If you need a more accurate
   * AABB, compute it using the shape and the body transform.
   *
   * @return
   */
  fun getAABB(childIndex: Int): AABB? {
    // assert is not supported in KMP.
    // assert(childIndex >= 0 && childIndex < m_proxyCount)
    return m_proxies!![childIndex]!!.aabb
  }

  /**
   * Dump this fixture to the log file.
   *
   * @param bodyIndex
   */
  @Suppress("UNUSED_PARAMETER") fun dump(bodyIndex: Int) {}

  // We need separation create/destroy functions from the constructor/destructor because
  // the destructor cannot access the allocator (no destructor arguments allowed by C++).
  fun create(body: Body, def: FixtureDef) {
    m_userData = def.userData
    m_friction = def.friction
    m_restitution = def.restitution
    m_body = body
    m_next = null
    m_filter.set(def.filter)
    m_isSensor = def.isSensor
    m_shape = def.shape!!.clone()

    // Reserve proxy space
    val childCount: Int = m_shape!!.getChildCount()
    if (m_proxies == null) {
      m_proxies = arrayOfNulls(childCount)
      for (i in 0 until childCount) {
        m_proxies!![i] = FixtureProxy()
        m_proxies!![i]!!.fixture = null
        m_proxies!![i]!!.proxyId = BroadPhase.NULL_PROXY
      }
    }
    if (m_proxies!!.size < childCount) {
      val old: Array<FixtureProxy?> = m_proxies!!
      val newLen = MathUtils.max(old.size * 2, childCount)
      m_proxies = arrayOfNulls(newLen)
      old.copyInto(m_proxies!!)
      for (i in 0 until newLen) {
        if (i >= old.size) {
          m_proxies!![i] = FixtureProxy()
        }
        m_proxies!![i]!!.fixture = null
        m_proxies!![i]!!.proxyId = BroadPhase.NULL_PROXY
      }
    }
    m_proxyCount = 0
    m_density = def.density
  }

  fun destroy() {
    // The proxies must be destroyed before calling this.
    // assert is not supported in KMP.
    // assert(m_proxyCount == 0)

    // Free the child shape.
    m_shape = null
    m_proxies = null
    m_next = null

    // TODO pool shapes
    // TODO pool fixtures
  }

  // These support body activation/deactivation.
  fun createProxies(broadPhase: BroadPhase, xf: Transform) {
    // assert is not supported in KMP.
    // assert(m_proxyCount == 0)

    // Create proxies in the broad-phase.
    m_proxyCount = m_shape!!.getChildCount()
    for (i in 0 until m_proxyCount) {
      val proxy = m_proxies!![i]!!
      m_shape!!.computeAABB(proxy.aabb, xf, i)
      proxy.proxyId = broadPhase.createProxy(proxy.aabb, proxy)
      proxy.fixture = this
      proxy.childIndex = i
    }
  }

  /**
   * Internal method
   *
   * @param broadPhase
   */
  fun destroyProxies(broadPhase: BroadPhase) {
    // Destroy proxies in the broad-phase.
    for (i in 0 until m_proxyCount) {
      val proxy = m_proxies!![i]
      broadPhase.destroyProxy(proxy!!.proxyId)
      proxy.proxyId = BroadPhase.NULL_PROXY
    }
    m_proxyCount = 0
  }

  private val pool1 = AABB()
  private val pool2 = AABB()
  private val displacement = Vec2()

  /**
   * Internal method
   *
   * @param broadPhase
   * @param xf1
   * @param xf2
   */
  fun synchronize(broadPhase: BroadPhase, transform1: Transform, transform2: Transform) {
    if (m_proxyCount == 0) {
      return
    }
    for (i in 0 until m_proxyCount) {
      val proxy = m_proxies!![i]

      // Compute an AABB that covers the swept shape (may miss some rotation effect).
      val aabb1 = pool1
      val aab = pool2
      m_shape!!.computeAABB(aabb1, transform1, proxy!!.childIndex)
      m_shape!!.computeAABB(aab, transform2, proxy.childIndex)
      proxy.aabb.lowerBound.x =
        if (aabb1.lowerBound.x < aab.lowerBound.x) aabb1.lowerBound.x else aab.lowerBound.x
      proxy.aabb.lowerBound.y =
        if (aabb1.lowerBound.y < aab.lowerBound.y) aabb1.lowerBound.y else aab.lowerBound.y
      proxy.aabb.upperBound.x =
        if (aabb1.upperBound.x > aab.upperBound.x) aabb1.upperBound.x else aab.upperBound.x
      proxy.aabb.upperBound.y =
        if (aabb1.upperBound.y > aab.upperBound.y) aabb1.upperBound.y else aab.upperBound.y
      displacement.x = transform2.p.x - transform1.p.x
      displacement.y = transform2.p.y - transform1.p.y
      broadPhase.moveProxy(proxy.proxyId, proxy.aabb, displacement)
    }
  }
}
