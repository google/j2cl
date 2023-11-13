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
  var density = 0f
  var next: Fixture? = null
  var body: Body? = null
  var shape: Shape? = null
    private set

  var friction = 0f

  var restitution = 0f
  var proxies: Array<FixtureProxy>? = null
  var proxyCount = 0
  var filterData: Filter = Filter()
    set(value) {
      field.set(value)
      refilter()
    }

  var isSensor = false
    set(value) {
      if (value != field) {
        body!!.setAwake(true)
        field = value
      }
    }

  var userData: Any? = null

  private val pool1 = AABB()
  private val pool2 = AABB()
  private val displacement = Vec2()

  /**
   * Get the type of the child shape. You can use this to down cast to the concrete shape.
   *
   * @return the shape type.
   */
  fun getType(): ShapeType = shape!!.m_type

  /**
   * Call this if you want to establish collision that was previously disabled by
   * ContactFilter::ShouldCollide.
   */
  fun refilter() {
    val nonNullBody = body ?: return

    // Flag associated contacts for filtering.
    var edge = nonNullBody.contactList
    while (edge != null) {
      val contact = edge.contact!!
      val fixtureA = contact.fixtureA
      val fixtureB = contact.fixtureB
      if (fixtureA === this || fixtureB === this) {
        contact.flagForFiltering()
      }
      edge = edge.next
    }
    val world = nonNullBody.world

    // Touch each proxy so that new pairs may be created
    val broadPhase = world.contactManager.broadPhase
    for (i in 0 until proxyCount) {
      broadPhase.touchProxy(proxies!![i].proxyId)
    }
  }

  /**
   * Test a point for containment in this fixture. This only works for convex shapes.
   *
   * @param p a point in world coordinates.
   * @return
   */
  fun testPoint(p: Vec2): Boolean = shape!!.testPoint(body!!.xf, p)

  /**
   * Cast a ray against this shape.
   *
   * @param output the ray-cast results.
   * @param input the ray-cast input parameters.
   * @param output
   * @param input
   */
  fun raycast(output: RayCastOutput, input: RayCastInput, childIndex: Int): Boolean =
    shape!!.raycast(output, input, body!!.xf, childIndex)

  /**
   * Get the mass data for this fixture. The mass data is based on the density and the shape. The
   * rotational inertia is about the shape's origin.
   *
   * @return
   */
  fun getMassData(massData: MassData) {
    shape!!.computeMass(massData, density)
  }

  /**
   * Get the fixture's AABB. This AABB may be enlarge and/or stale. If you need a more accurate
   * AABB, compute it using the shape and the body transform.
   *
   * @return
   */
  fun getAABB(childIndex: Int): AABB? = proxies!![childIndex].aabb

  /**
   * Dump this fixture to the log file.
   *
   * @param bodyIndex
   */
  @Suppress("UNUSED_PARAMETER") fun dump(bodyIndex: Int) {}

  // We need separation create/destroy functions from the constructor/destructor because
  // the destructor cannot access the allocator (no destructor arguments allowed by C++).
  fun create(newBody: Body, def: FixtureDef) {
    userData = def.userData
    friction = def.friction
    restitution = def.restitution
    body = newBody
    next = null
    filterData.set(def.filter)
    isSensor = def.isSensor
    shape = def.shape!!.clone()

    // Reserve proxy space
    val childCount: Int = shape!!.getChildCount()
    if (proxies == null) {
      proxies =
        Array(childCount) {
          val proxy = FixtureProxy()
          proxy.fixture = null
          proxy.proxyId = BroadPhase.NULL_PROXY
          proxy
        }
    }
    if (proxies!!.size < childCount) {
      val old: Array<FixtureProxy> = proxies!!
      val newLen = MathUtils.max(old.size * 2, childCount)
      proxies =
        Array(newLen) { index ->
          var proxy: FixtureProxy = if (index < old.size) old[index] else FixtureProxy()
          proxy.fixture = null
          proxy.proxyId = BroadPhase.NULL_PROXY
          proxy
        }
    }
    proxyCount = 0
    density = def.density
  }

  fun destroy() {
    // The proxies must be destroyed before calling this.
    // assert is not supported in KMP.
    // assert(m_proxyCount == 0)

    // Free the child shape.
    shape = null
    proxies = null
    next = null

    // TODO pool shapes
    // TODO pool fixtures
  }

  // These support body activation/deactivation.
  fun createProxies(broadPhase: BroadPhase, xf: Transform) {
    // assert is not supported in KMP.
    // assert(m_proxyCount == 0)

    // Create proxies in the broad-phase.
    proxyCount = shape!!.getChildCount()
    for (i in 0 until proxyCount) {
      val proxy = proxies!![i]
      shape!!.computeAABB(proxy.aabb, xf, i)
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
    for (i in 0 until proxyCount) {
      val proxy = proxies!![i]
      broadPhase.destroyProxy(proxy.proxyId)
      proxy.proxyId = BroadPhase.NULL_PROXY
    }
    proxyCount = 0
  }

  /**
   * Internal method
   *
   * @param broadPhase
   * @param xf1
   * @param xf2
   */
  fun synchronize(broadPhase: BroadPhase, transform1: Transform, transform2: Transform) {
    if (proxyCount == 0) {
      return
    }
    for (i in 0 until proxyCount) {
      val proxy = proxies!![i]

      // Compute an AABB that covers the swept shape (may miss some rotation effect).
      val aabb1 = pool1
      val aab = pool2
      val nonNullShape = shape!!
      nonNullShape.computeAABB(aabb1, transform1, proxy.childIndex)
      nonNullShape.computeAABB(aab, transform2, proxy.childIndex)
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
