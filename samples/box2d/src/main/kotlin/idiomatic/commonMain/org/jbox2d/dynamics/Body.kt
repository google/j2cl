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

import org.jbox2d.collision.shapes.MassData
import org.jbox2d.collision.shapes.Shape
import org.jbox2d.common.Flags
import org.jbox2d.common.MathUtils
import org.jbox2d.common.Rot
import org.jbox2d.common.Sweep
import org.jbox2d.common.Transform
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.contacts.ContactEdge
import org.jbox2d.dynamics.joints.JointEdge

/**
 * A rigid body. These are created via World.createBody.
 *
 * @author Daniel Murphy
 */
class Body(bd: BodyDef, world: World) {
  var type: BodyType = bd.type
    private set

  var flags =
    Flags(
      if (bd.bullet) BULLET_FLAG else 0,
      if (bd.fixedRotation) FIXED_ROTATION_FLAG else 0,
      if (bd.allowSleep) AUTO_SLEEP_FLAG else 0,
      if (bd.awake) AWAKE_FLAG else 0,
      if (bd.active) ACTIVE_FLAG else 0,
    )

  var islandIndex = 0
  /** The body origin transform. */
  val xf =
    Transform().apply {
      p.set(bd.position)
      q.set(bd.angle)
    }

  /** The swept motion for CCD */
  val sweep =
    Sweep().apply {
      localCenter.setZero()
      c0.set(xf.p)
      c.set(xf.p)
      a0 = bd.angle
      a = bd.angle
      alpha0 = 0.0f
    }

  var linearVelocity = Vec2().apply { this.set(bd.linearVelocity) }
    set(value) {
      if (type == BodyType.STATIC) {
        return
      }
      if (value dot value > 0.0f) {
        setAwake(true)
      }
      field.set(value)
    }

  var angularVelocity = bd.angularVelocity
    set(value) {
      if (type == BodyType.STATIC) {
        return
      }
      if (value * value > 0f) {
        setAwake(true)
      }
      field = value
    }

  val force = Vec2().apply { this.setZero() }
  var torque = 0.0f

  /** Get the parent world of this body. */
  var world: World = world
  var prev: Body? = null

  /** Get the next body in the world's body list. */
  var next: Body? = null

  /** Get the list of all fixtures attached to this body. */
  var fixtureList: Fixture? = null
  var fixtureCount: Int = 0

  /** Get the list of all joints attached to this body. */
  var jointList: JointEdge? = null

  var contactList: ContactEdge? = null

  var mass = if (type == BodyType.DYNAMIC) 1f else 0f
  var invMass = if (type == BodyType.DYNAMIC) 1f else 0f

  // Rotational inertia about the center of mass.
  var I: Float = 0.0f
  var invI: Float = 0.0f
  var linearDamping: Float = bd.linearDamping
  var angularDamping: Float = bd.angularDamping
  var gravityScale: Float = bd.gravityScale

  var sleepTime: Float = 0.0f

  var userData: Any? = bd.userData

  val position: Vec2
    get() = xf.p

  val angle: Float
    get() = sweep.a

  val worldCenter: Vec2
    get() = sweep.c

  /** Get the local position of the center of mass. Do not modify. */
  val localCenter: Vec2
    get() = sweep.localCenter

  val isAwake: Boolean
    get() = AWAKE_FLAG in flags

  val isSleepingAllowed: Boolean
    get() = AUTO_SLEEP_FLAG in flags

  val isBullet: Boolean
    get() = BULLET_FLAG in flags

  val isActive: Boolean
    get() = ACTIVE_FLAG in flags

  val isFixedRotation: Boolean
    get() = FIXED_ROTATION_FLAG in flags

  private val fixDef = FixtureDef()
  private val pmd = MassData()
  // djm pooling
  private val pxf = Transform()

  init {
    assert(bd.position.isValid)
    assert(bd.linearVelocity.isValid)
    assert(bd.gravityScale >= 0.0f)
    assert(bd.angularDamping >= 0.0f)
    assert(bd.linearDamping >= 0.0f)
  }

  fun setType(newType: BodyType) {
    assert(!world.isLocked)
    if (world.isLocked) {
      return
    }

    if (type == newType) {
      return
    }
    type = newType
    resetMassData()
    if (type == BodyType.STATIC) {
      linearVelocity.setZero()
      angularVelocity = 0.0f
      sweep.a0 = sweep.a
      sweep.c0.set(sweep.c)
      synchronizeFixtures()
    }
    setAwake(true)
    force.setZero()
    torque = 0.0f

    // Delete the attached contacts.
    var ce = contactList
    while (ce != null) {
      val ce0: ContactEdge = ce
      ce = ce.next
      world.contactManager.destroy(ce0.contact!!)
    }
    contactList = null

    // Touch the proxies so that new contacts will be created (when appropriate)
    val broadPhase = world.contactManager.broadPhase
    var f = fixtureList
    while (f != null) {
      val proxyCount = f.proxyCount
      for (i in 0 until proxyCount) {
        broadPhase.touchProxy(f.proxies!![i].proxyId)
      }
      f = f.next
    }
  }

  /**
   * Creates a fixture and attach it to this body. Use this function if you need to set some fixture
   * parameters, like friction. Otherwise you can create the fixture directly from a shape. If the
   * density is non-zero, this function automatically updates the mass of the body. Contacts are not
   * created until the next time step.
   *
   * This function is locked during callbacks.
   *
   * @param def the fixture definition.
   */
  fun createFixture(def: FixtureDef): Fixture? {
    assert(!world.isLocked)
    if (world.isLocked) {
      return null
    }

    val fixture = Fixture()
    fixture.create(this, def)
    if (ACTIVE_FLAG in flags) {
      val broadPhase = world.contactManager.broadPhase
      fixture.createProxies(broadPhase, this.xf)
    }
    fixture.next = fixtureList
    fixtureList = fixture
    ++fixtureCount
    fixture.body = this

    // Adjust mass properties if needed.
    if (fixture.density > 0.0f) {
      resetMassData()
    }

    // Let the world know we have a new fixture. This will cause new contacts
    // to be created at the beginning of the next time step.
    world.flags += World.NEW_FIXTURE
    return fixture
  }

  /**
   * Creates a fixture from a shape and attach it to this body. This is a convenience function. Use
   * FixtureDef if you need to set parameters like friction, restitution, user data, or filtering.
   * If the density is non-zero, this function automatically updates the mass of the body.
   *
   * This function is locked during callbacks.
   *
   * @param shape the shape to be cloned.
   * @param density the shape density (set to zero for static bodies).
   */
  fun createFixture(shape: Shape, density: Float): Fixture? {
    fixDef.shape = shape
    fixDef.density = density
    return createFixture(fixDef)
  }

  /**
   * Destroy a fixture. This removes the fixture from the broad-phase and destroys all contacts
   * associated with this fixture. This will automatically adjust the mass of the body if the body
   * is dynamic and the fixture has positive density. All fixtures attached to a body are implicitly
   * destroyed when the body is destroyed.
   *
   * This function is locked during callbacks.
   *
   * @param fixture the fixture to be removed.
   */
  fun destroyFixture(fixture: Fixture?) {
    var tempFixture = fixture
    assert(!world.isLocked)
    if (world.isLocked) {
      return
    }

    assert(fixture!!.body === this)
    assert(fixtureCount > 0)
    var node = fixtureList
    var last: Fixture? = null // java change
    var found = false
    while (node != null) {
      if (node == tempFixture) {
        @Suppress("UNUSED_VALUE")
        node = tempFixture.next
        found = true
        break
      }
      last = node
      node = node.next
    }
    assert(found)

    // java change, remove it from the list
    if (last == null) {
      fixtureList = tempFixture!!.next
    } else {
      last.next = tempFixture!!.next
    }

    // Destroy any contacts associated with the fixture.
    var edge = contactList
    while (edge != null) {
      val c = edge.contact!!
      edge = edge.next
      val fixtureA = c.fixtureA
      val fixtureB = c.fixtureB
      if (tempFixture == fixtureA || tempFixture == fixtureB) {
        // This destroys the contact and removes it from
        // this body's contact list.
        world.contactManager.destroy(c)
      }
    }
    if (ACTIVE_FLAG in flags) {
      val broadPhase = world.contactManager.broadPhase
      tempFixture.destroyProxies(broadPhase)
    }
    tempFixture.destroy()
    tempFixture.body = null
    tempFixture.next = null
    @Suppress("UNUSED_VALUE")
    tempFixture = null
    --fixtureCount

    // Reset the mass data.
    resetMassData()
  }

  /**
   * Set the position of the body's origin and rotation. This breaks any contacts and wakes the
   * other bodies. Manipulating a body's transform may cause non-physical behavior.
   *
   * @param position the world position of the body's local origin.
   * @param angle the world rotation in radians.
   */
  fun setTransform(position: Vec2, angle: Float) {
    assert(!world.isLocked)
    if (world.isLocked) {
      return
    }

    xf.q.set(angle)
    xf.p.set(position)

    // m_sweep.c0 = m_sweep.c = Mul(m_xf, m_sweep.localCenter);
    Transform.mulToOutUnsafe(xf, sweep.localCenter, sweep.c)
    sweep.a = angle
    sweep.c0.set(sweep.c)
    sweep.a0 = sweep.a
    val broadPhase = world.contactManager.broadPhase
    var f = fixtureList
    while (f != null) {
      f.synchronize(broadPhase, xf, xf)
      f = f.next
    }
    world.contactManager.findNewContacts()
  }

  /**
   * Apply a force at a world point. If the force is not applied at the center of mass, it will
   * generate a torque and affect the angular velocity. This wakes up the body.
   *
   * @param newForce the world force vector, usually in Newtons (N).
   * @param point the world position of the point of application.
   */
  fun applyForce(newForce: Vec2, point: Vec2) {
    if (type != BodyType.DYNAMIC) {
      return
    }
    if (isAwake) {
      setAwake(true)
    }

    // m_force.addLocal(force);
    // Vec2 temp = tltemp.get();
    // temp.set(point).subLocal(m_sweep.c);
    // m_torque += Vec2.cross(temp, force);
    force.x += newForce.x
    force.y += newForce.y
    torque += (point.x - sweep.c.x) * newForce.y - (point.y - sweep.c.y) * newForce.x
  }

  /**
   * Apply a force to the center of mass. This wakes up the body.
   *
   * @param newForce the world force vector, usually in Newtons (N).
   */
  fun applyForceToCenter(newForce: Vec2) {
    if (type != BodyType.DYNAMIC) {
      return
    }
    if (!isAwake) {
      setAwake(true)
    }
    force.x += newForce.x
    force.y += newForce.y
  }

  /**
   * Apply a torque. This affects the angular velocity without affecting the linear velocity of the
   * center of mass. This wakes up the body.
   *
   * @param newTorque about the z-axis (out of the screen), usually in N-m.
   */
  fun applyTorque(newTorque: Float) {
    if (type != BodyType.DYNAMIC) {
      return
    }
    if (!isAwake) {
      setAwake(true)
    }
    torque += newTorque
  }

  /**
   * Apply an impulse at a point. This immediately modifies the velocity. It also modifies the
   * angular velocity if the point of application is not at the center of mass. This wakes up the
   * body.
   *
   * @param impulse the world impulse vector, usually in N-seconds or kg-m/s.
   * @param point the world position of the point of application.
   */
  fun applyLinearImpulse(impulse: Vec2, point: Vec2) {
    if (type != BodyType.DYNAMIC) {
      return
    }
    if (!isAwake) {
      setAwake(true)
    }

    // Vec2 temp = tltemp.get();
    // temp.set(impulse).mulLocal(m_invMass);
    // m_linearVelocity.addLocal(temp);
    //
    // temp.set(point).subLocal(m_sweep.c);
    // m_angularVelocity += m_invI * Vec2.cross(temp, impulse);
    linearVelocity.x += impulse.x * invMass
    linearVelocity.y += impulse.y * invMass
    angularVelocity +=
      invI * ((point.x - sweep.c.x) * impulse.y - (point.y - sweep.c.y) * impulse.x)
  }

  /**
   * Apply an angular impulse.
   *
   * @param impulse the angular impulse in units of kg*m*m/s
   */
  fun applyAngularImpulse(impulse: Float) {
    if (type != BodyType.DYNAMIC) {
      return
    }
    if (!isAwake) {
      setAwake(true)
    }
    angularVelocity += invI * impulse
  }

  /**
   * Get the central rotational inertia of the body.
   *
   * @return the rotational inertia, usually in kg-m^2.
   */
  fun getInertia(): Float =
    (I +
      mass *
        (sweep.localCenter.x * sweep.localCenter.x + sweep.localCenter.y * sweep.localCenter.y))

  /**
   * Get the mass data of the body. The rotational inertia is relative to the center of mass.
   *
   * @return a struct containing the mass, inertia and center of the body.
   */
  fun getMassData(data: MassData) {
    // data.mass = m_mass;
    // data.I = m_I + m_mass * Vec2.dot(m_sweep.localCenter, m_sweep.localCenter);
    // data.center.set(m_sweep.localCenter);
    data.mass = mass
    data.I =
      (I +
        mass *
          (sweep.localCenter.x * sweep.localCenter.x + sweep.localCenter.y * sweep.localCenter.y))
    data.center.x = sweep.localCenter.x
    data.center.y = sweep.localCenter.y
  }

  /**
   * Set the mass properties to override the mass properties of the fixtures. Note that this changes
   * the center of mass position. Note that creating or destroying fixtures can also alter the mass.
   * This function has no effect if the body isn't dynamic.
   *
   * @param massData the mass properties.
   */
  fun setMassData(massData: MassData) {
    // TODO_ERIN adjust linear velocity and torque to account for movement of center.
    assert(!world.isLocked)
    if (world.isLocked) {
      return
    }

    if (type != BodyType.DYNAMIC) {
      return
    }
    invMass = 0.0f
    I = 0.0f
    invI = 0.0f
    mass = massData.mass
    if (mass <= 0.0f) {
      mass = 1f
    }
    invMass = 1.0f / mass
    if (massData.I > 0.0f && FIXED_ROTATION_FLAG !in flags) {
      I = massData.I - mass * (massData.center dot massData.center)
      assert(I > 0.0f)
      invI = 1.0f / I
    }
    val oldCenter = world.pool.popVec2()
    // Move center of mass.
    oldCenter.set(sweep.c)
    sweep.localCenter.set(massData.center)
    // m_sweep.c0 = m_sweep.c = Mul(m_xf, m_sweep.localCenter);
    Transform.mulToOutUnsafe(xf, sweep.localCenter, sweep.c0)
    sweep.c.set(sweep.c0)

    // Update center of mass velocity.
    // m_linearVelocity += Cross(m_angularVelocity, m_sweep.c - oldCenter);
    val temp = world.pool.popVec2()
    temp.set(sweep.c).subLocal(oldCenter)
    Vec2.crossToOut(angularVelocity, temp, temp)
    linearVelocity.addLocal(temp)
    world.pool.pushVec2(2)
  }

  /**
   * This resets the mass properties to the sum of the mass properties of the fixtures. This
   * normally does not need to be called unless you called setMassData to override the mass and you
   * later want to reset the mass.
   */
  fun resetMassData() {
    // Compute mass data from shapes. Each shape has its own density.
    mass = 0.0f
    invMass = 0.0f
    I = 0.0f
    invI = 0.0f
    sweep.localCenter.setZero()

    // Static and kinematic bodies have zero mass.
    if (type == BodyType.STATIC || type == BodyType.KINEMATIC) {
      // m_sweep.c0 = m_sweep.c = m_xf.position;
      sweep.c0.set(xf.p)
      sweep.c.set(xf.p)
      sweep.a0 = sweep.a
      return
    }
    assert(type == BodyType.DYNAMIC)

    // Accumulate mass over all fixtures.
    val localCenter = world.pool.popVec2()
    localCenter.setZero()
    val temp = world.pool.popVec2()
    val massData = pmd
    var f = fixtureList
    while (f != null) {
      if (f.density == 0.0f) {
        f = f.next
        continue
      }
      f.getMassData(massData)
      mass += massData.mass
      // center += massData.mass * massData.center;
      temp.set(massData.center).mulLocal(massData.mass)
      localCenter.addLocal(temp)
      I += massData.I
      f = f.next
    }

    // Compute center of mass.
    if (mass > 0.0f) {
      invMass = 1.0f / mass
      localCenter.mulLocal(invMass)
    } else {
      // Force all dynamic bodies to have a positive mass.
      mass = 1.0f
      invMass = 1.0f
    }
    if (I > 0.0f && FIXED_ROTATION_FLAG !in flags) {
      // Center the inertia about the center of mass.
      I -= mass * (localCenter dot localCenter)
      assert(I > 0.0f)
      invI = 1.0f / I
    } else {
      I = 0.0f
      invI = 0.0f
    }
    val oldCenter = world.pool.popVec2()
    // Move center of mass.
    oldCenter.set(sweep.c)
    sweep.localCenter.set(localCenter)
    // m_sweep.c0 = m_sweep.c = Mul(m_xf, m_sweep.localCenter);
    Transform.mulToOutUnsafe(xf, sweep.localCenter, sweep.c0)
    sweep.c.set(sweep.c0)

    // Update center of mass velocity.
    // m_linearVelocity += Cross(m_angularVelocity, m_sweep.c - oldCenter);
    temp.set(sweep.c).subLocal(oldCenter)
    Vec2.crossToOutUnsafe(angularVelocity, temp, oldCenter)
    linearVelocity.addLocal(oldCenter)
    world.pool.pushVec2(3)
  }

  /**
   * Get the world coordinates of a point given the local coordinates.
   *
   * @param localPoint a point on the body measured relative the the body's origin.
   * @return the same point expressed in world coordinates.
   */
  fun getWorldPoint(localPoint: Vec2): Vec2 = Vec2().apply { getWorldPointToOut(localPoint, this) }

  fun getWorldPointToOut(localPoint: Vec2, out: Vec2) {
    Transform.mulToOut(xf, localPoint, out)
  }

  /**
   * Get the world coordinates of a vector given the local coordinates.
   *
   * @param localVector a vector fixed in the body.
   * @return the same vector expressed in world coordinates.
   */
  fun getWorldVector(localVector: Vec2): Vec2 =
    Vec2().apply { getWorldVectorToOut(localVector, this) }

  fun getWorldVectorToOut(localVector: Vec2, out: Vec2) {
    Rot.mulToOut(xf.q, localVector, out)
  }

  fun getWorldVectorToOutUnsafe(localVector: Vec2, out: Vec2) {
    Rot.mulToOutUnsafe(xf.q, localVector, out)
  }

  /**
   * Gets a local point relative to the body's origin given a world point.
   *
   * @param a point in world coordinates.
   * @return the corresponding local point relative to the body's origin.
   */
  fun getLocalPoint(worldPoint: Vec2): Vec2 = Vec2().apply { getLocalPointToOut(worldPoint, this) }

  fun getLocalPointToOut(worldPoint: Vec2, out: Vec2) {
    Transform.mulTransToOut(xf, worldPoint, out)
  }

  /**
   * Gets a local vector given a world vector.
   *
   * @param a vector in world coordinates.
   * @return the corresponding local vector.
   */
  fun getLocalVector(worldVector: Vec2): Vec2 =
    Vec2().apply { getLocalVectorToOut(worldVector, this) }

  fun getLocalVectorToOut(worldVector: Vec2, out: Vec2) {
    Rot.mulTrans(xf.q, worldVector, out)
  }

  fun getLocalVectorToOutUnsafe(worldVector: Vec2, out: Vec2) {
    Rot.mulTransUnsafe(xf.q, worldVector, out)
  }

  /**
   * Get the world linear velocity of a world point attached to this body.
   *
   * @param a point in world coordinates.
   * @return the world velocity of a point.
   */
  fun getLinearVelocityFromWorldPoint(worldPoint: Vec2): Vec2 =
    Vec2().apply { getLinearVelocityFromWorldPointToOut(worldPoint, this) }

  fun getLinearVelocityFromWorldPointToOut(worldPoint: Vec2, out: Vec2) {
    out.set(worldPoint).subLocal(sweep.c)
    Vec2.crossToOut(angularVelocity, out, out)
    out.addLocal(linearVelocity)
  }

  /**
   * Get the world velocity of a local point.
   *
   * @param a point in local coordinates.
   * @return the world velocity of a point.
   */
  fun getLinearVelocityFromLocalPoint(localPoint: Vec2): Vec2 =
    Vec2().apply { getLinearVelocityFromLocalPointToOut(localPoint, this) }

  fun getLinearVelocityFromLocalPointToOut(localPoint: Vec2, out: Vec2) {
    getWorldPointToOut(localPoint, out)
    getLinearVelocityFromWorldPointToOut(out, out)
  }

  /** Should this body be treated like a bullet for continuous collision detection? */
  fun setBullent(flag: Boolean) {
    flags = flags.setOrRemove(BULLET_FLAG, flag)
  }

  /** You can disable sleeping on this body. If you disable sleeping, the body will be woken. */
  fun setSleepingAllowed(flag: Boolean) {
    flags = flags.setOrRemove(AUTO_SLEEP_FLAG, flag)
    if (!flag) {
      setAwake(true)
    }
  }

  /**
   * Set the sleep state of the body. A sleeping body has very low CPU cost.
   *
   * @param flag set to true to put body to sleep, false to wake it.
   */
  fun setAwake(flag: Boolean) {
    if (flag) {
      if (AWAKE_FLAG !in flags) {
        flags += AWAKE_FLAG
        userData = 0.0f
      }
    } else {
      flags -= AWAKE_FLAG
      userData = 0.0f
      linearVelocity.setZero()
      angularVelocity = 0.0f
      force.setZero()
      torque = 0.0f
    }
  }

  /**
   * Set the active state of the body. An inactive body is not simulated and cannot be collided with
   * or woken up. If you pass a flag of true, all fixtures will be added to the broad-phase. If you
   * pass a flag of false, all fixtures will be removed from the broad-phase and all contacts will
   * be destroyed. Fixtures and joints are otherwise unaffected. You may continue to create/destroy
   * fixtures and joints on inactive bodies. Fixtures on an inactive body are implicitly inactive
   * and will not participate in collisions, ray-casts, or queries. Joints connected to an inactive
   * body are implicitly inactive. An inactive body is still owned by a World object and remains in
   * the body list.
   */
  fun setIsActive(flag: Boolean) {
    assert(world.isLocked == false)
    if (flag == isActive) {
      return
    }
    if (flag) {
      flags += ACTIVE_FLAG

      // Create all proxies.
      val broadPhase = world.contactManager.broadPhase
      var f = fixtureList
      while (f != null) {
        f.createProxies(broadPhase, xf)
        f = f.next
      }

      // Contacts are created the next time step.
    } else {
      flags -= ACTIVE_FLAG

      // Destroy all proxies.
      val broadPhase = world.contactManager.broadPhase
      var f = fixtureList
      while (f != null) {
        f.destroyProxies(broadPhase)
        f = f.next
      }

      // Destroy the attached contacts.
      var ce = contactList
      while (ce != null) {
        val ce0: ContactEdge = ce
        ce = ce.next
        world.contactManager.destroy(ce0.contact!!)
      }
      contactList = null
    }
  }

  /** Set this body to have fixed rotation. This causes the mass to be reset. */
  fun setFixedRotation(flag: Boolean) {
    flags = flags.setOrRemove(FIXED_ROTATION_FLAG, flag)
    resetMassData()
  }

  fun synchronizeFixtures() {
    val xf1 = pxf
    // xf1.position = m_sweep.c0 - Mul(xf1.R, m_sweep.localCenter);

    // xf1.q.set(m_sweep.a0);
    // Rot.mulToOutUnsafe(xf1.q, m_sweep.localCenter, xf1.p);
    // xf1.p.mulLocal(-1).addLocal(m_sweep.c0);
    // inlined:
    xf1.q.sin = MathUtils.sin(sweep.a0)
    xf1.q.cos = MathUtils.cos(sweep.a0)
    xf1.p.x = sweep.c0.x - xf1.q.cos * sweep.localCenter.x + xf1.q.sin * sweep.localCenter.y
    xf1.p.y = sweep.c0.y - xf1.q.sin * sweep.localCenter.x - xf1.q.cos * sweep.localCenter.y
    // end inline
    var f = fixtureList
    while (f != null) {
      f.synchronize(world.contactManager.broadPhase, xf1, xf)
      f = f.next
    }
  }

  fun synchronizeTransform() {
    // m_xf.q.set(m_sweep.a);
    //
    // // m_xf.position = m_sweep.c - Mul(m_xf.R, m_sweep.localCenter);
    // Rot.mulToOutUnsafe(m_xf.q, m_sweep.localCenter, m_xf.p);
    // m_xf.p.mulLocal(-1).addLocal(m_sweep.c);
    //
    xf.q.sin = MathUtils.sin(sweep.a)
    xf.q.cos = MathUtils.cos(sweep.a)
    val q = xf.q
    val v = sweep.localCenter
    xf.p.x = sweep.c.x - q.cos * v.x + q.sin * v.y
    xf.p.y = sweep.c.y - q.sin * v.x - q.cos * v.y
  }

  /**
   * This is used to prevent connected bodies from colliding. It may lie, depending on the
   * collideConnected flag.
   */
  fun shouldCollide(other: Body): Boolean {
    // At least one body should be dynamic.
    if (type != BodyType.DYNAMIC && other.type != BodyType.DYNAMIC) {
      return false
    }

    // Does a joint prevent collision?
    var jn = jointList
    while (jn != null) {
      if (jn.other === other) {
        if (!jn.joint!!.collideConnected) {
          return false
        }
      }
      jn = jn.next
    }
    return true
  }

  fun advance(t: Float) {
    // Advance to the new safe time. This doesn't sync the broad-phase.
    sweep.advance(t)
    sweep.c.set(sweep.c0)
    sweep.a = sweep.a0
    xf.q.set(sweep.a)
    // m_xf.position = m_sweep.c - Mul(m_xf.R, m_sweep.localCenter);
    Rot.mulToOutUnsafe(xf.q, sweep.localCenter, xf.p)
    xf.p.mulLocal(-1f).addLocal(sweep.c)
  }

  companion object {
    const val IS_LAND_FLAG = 0x0001
    const val AWAKE_FLAG = 0x0002
    const val AUTO_SLEEP_FLAG = 0x0004
    const val BULLET_FLAG = 0x0008
    const val FIXED_ROTATION_FLAG = 0x0010
    const val ACTIVE_FLAG = 0x0020
    const val TOIL_FLAG = 0x0040
  }
}
