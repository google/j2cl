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
  var m_type: BodyType

  var m_flags: Int
  var m_islandIndex = 0
  /** The body origin transform. */
  val m_xf = Transform()

  /** The swept motion for CCD */
  val m_sweep = Sweep()
  val m_linearVelocity = Vec2()
  var m_angularVelocity = 0f
  val m_force = Vec2()
  var m_torque = 0f

  /** Get the parent world of this body. */
  var m_world: World
  var m_prev: Body?

  /** Get the next body in the world's body list. */
  var m_next: Body?

  /** Get the list of all fixtures attached to this body. */
  var m_fixtureList: Fixture?
  var m_fixtureCount: Int

  /** Get the list of all joints attached to this body. */
  var m_jointList: JointEdge?

  /**
   * Get the list of all contacts attached to this body.
   *
   * @warning this list changes during the time step and you may miss some collisions if you don't
   *   use ContactListener.
   */
  var m_contactList: ContactEdge?

  /**
   * Get the total mass of the body.
   *
   * @return the mass, usually in kilograms (kg).
   */
  var m_mass = 0f
  var m_invMass = 0f

  // Rotational inertia about the center of mass.
  var m_I: Float
  var m_invI: Float
  /** Get the linear damping of the body. */
  /** Set the linear damping of the body. */
  var m_linearDamping: Float
  /** Get the angular damping of the body. */
  /** Set the angular damping of the body. */
  var m_angularDamping: Float
  /**
   * Get the gravity scale of the body.
   *
   * @return
   */
  /**
   * Set the gravity scale of the body.
   *
   * @param gravityScale
   */
  var m_gravityScale: Float

  var m_sleepTime: Float

  var m_userData: Any?

  init {
    // assert is not supported in KMP.
    /* assert(bd.position.isValid)
    assert(bd.linearVelocity.isValid)
    assert(bd.gravityScale >= 0.0f)
    assert(bd.angularDamping >= 0.0f)
    assert(bd.linearDamping >= 0.0f) */
    m_flags = 0
    if (bd.bullet) {
      m_flags = m_flags or e_bulletFlag
    }
    if (bd.fixedRotation) {
      m_flags = m_flags or e_fixedRotationFlag
    }
    if (bd.allowSleep) {
      m_flags = m_flags or e_autoSleepFlag
    }
    if (bd.awake) {
      m_flags = m_flags or e_awakeFlag
    }
    if (bd.active) {
      m_flags = m_flags or e_activeFlag
    }
    this.m_world = world
    this.m_xf.p.set(bd.position)
    this.m_xf.q.set(bd.angle)
    m_sweep.localCenter.setZero()
    m_sweep.c0.set(this.m_xf.p)
    m_sweep.c.set(this.m_xf.p)
    m_sweep.a0 = bd.angle
    m_sweep.a = bd.angle
    m_sweep.alpha0 = 0.0f
    m_jointList = null
    m_contactList = null
    m_prev = null
    m_next = null
    m_linearVelocity.set(bd.linearVelocity)
    m_angularVelocity = bd.angularVelocity
    m_linearDamping = bd.linearDamping
    m_angularDamping = bd.angularDamping
    m_gravityScale = bd.gravityScale
    m_force.setZero()
    m_torque = 0.0f
    m_sleepTime = 0.0f
    m_type = bd.type
    if (m_type == BodyType.DYNAMIC) {
      m_mass = 1f
      m_invMass = 1f
    } else {
      m_mass = 0f
      m_invMass = 0f
    }
    m_I = 0.0f
    m_invI = 0.0f
    m_userData = bd.userData
    m_fixtureList = null
    m_fixtureCount = 0
  }

  /**
   * Creates a fixture and attach it to this body. Use this function if you need to set some fixture
   * parameters, like friction. Otherwise you can create the fixture directly from a shape. If the
   * density is non-zero, this function automatically updates the mass of the body. Contacts are not
   * created until the next time step.
   *
   * @param def the fixture definition.
   * @warning This function is locked during callbacks.
   */
  fun createFixture(def: FixtureDef): Fixture? {
    // assert is not supported in KMP.
    // assert(world.isLocked == false)
    if (m_world.isLocked() == true) {
      return null
    }
    val fixture = Fixture()
    fixture.create(this, def)
    if (m_flags and e_activeFlag == e_activeFlag) {
      val broadPhase = m_world.m_contactManager.m_broadPhase
      fixture.createProxies(broadPhase, this.m_xf)
    }
    fixture.m_next = m_fixtureList
    m_fixtureList = fixture
    ++m_fixtureCount
    fixture.m_body = this

    // Adjust mass properties if needed.
    if (fixture.m_density > 0.0f) {
      resetMassData()
    }

    // Let the world know we have a new fixture. This will cause new contacts
    // to be created at the beginning of the next time step.
    m_world.m_flags = m_world.m_flags or World.Companion.NEW_FIXTURE
    return fixture
  }

  private val fixDef = FixtureDef()

  /**
   * Creates a fixture from a shape and attach it to this body. This is a convenience function. Use
   * FixtureDef if you need to set parameters like friction, restitution, user data, or filtering.
   * If the density is non-zero, this function automatically updates the mass of the body.
   *
   * @param shape the shape to be cloned.
   * @param density the shape density (set to zero for static bodies).
   * @warning This function is locked during callbacks.
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
   * @param fixture the fixture to be removed.
   * @warning This function is locked during callbacks.
   */
  fun destroyFixture(fixture: Fixture?) {
    @Suppress("NAME_SHADOWING") var fixture = fixture
    // assert is not supported in KMP.
    // assert(world.isLocked == false)
    if (m_world.isLocked() == true) {
      return
    }
    // assert is not supported in KMP.
    // assert(fixture.m_body === this)
    // assert(m_fixtureCount > 0)
    var node = m_fixtureList
    var last: Fixture? = null // java change
    @Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE") var found = false
    while (node != null) {
      if (node == fixture) {
        @Suppress("UNUSED_VALUE")
        node = fixture.m_next
        @Suppress("UNUSED_VALUE")
        found = true
        break
      }
      last = node
      node = node.m_next
    }
    // assert is not supported in KMP.
    // assert(found)

    // java change, remove it from the list
    if (last == null) {
      m_fixtureList = fixture!!.m_next
    } else {
      last.m_next = fixture!!.m_next
    }

    // Destroy any contacts associated with the fixture.
    var edge = m_contactList
    while (edge != null) {
      val c = edge.contact!!
      edge = edge.next
      val fixtureA = c.m_fixtureA
      val fixtureB = c.m_fixtureB
      if (fixture == fixtureA || fixture == fixtureB) {
        // This destroys the contact and removes it from
        // this body's contact list.
        m_world.m_contactManager.destroy(c)
      }
    }
    if (m_flags and e_activeFlag == e_activeFlag) {
      val broadPhase = m_world.m_contactManager.m_broadPhase
      fixture.destroyProxies(broadPhase)
    }
    fixture.destroy()
    fixture.m_body = null
    fixture.m_next = null
    @Suppress("UNUSED_VALUE")
    fixture = null
    --m_fixtureCount

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
    // assert is not supported in KMP.
    // assert(world.isLocked == false)
    if (m_world.isLocked() == true) {
      return
    }
    this.m_xf.q.set(angle)
    this.m_xf.p.set(position)

    // m_sweep.c0 = m_sweep.c = Mul(m_xf, m_sweep.localCenter);
    Transform.mulToOutUnsafe(this.m_xf, m_sweep.localCenter, m_sweep.c)
    m_sweep.a = angle
    m_sweep.c0.set(m_sweep.c)
    m_sweep.a0 = m_sweep.a
    val broadPhase = m_world.m_contactManager.m_broadPhase
    var f = m_fixtureList
    while (f != null) {
      f.synchronize(broadPhase, this.m_xf, this.m_xf)
      f = f.m_next
    }
    m_world.m_contactManager.findNewContacts()
  }

  /**
   * Get the body transform for the body's origin.
   *
   * @return the world transform of the body's origin.
   */
  fun getTransform(): Transform {
    return this.m_xf
  }

  /**
   * Get the world body origin position. Do not modify.
   *
   * @return the world position of the body's origin.
   */
  fun getPosition(): Vec2 {
    return m_xf.p
  }

  fun getAngle(): Float {
    return m_sweep.a
  }

  fun getWorldCenter(): Vec2 {
    return m_sweep.c
  }

  /** Get the local position of the center of mass. Do not modify. */
  fun getLocalCenter(): Vec2 {
    return m_sweep.localCenter
  }

  /**
   * Set the linear velocity of the center of mass.
   *
   * @param v the new linear velocity of the center of mass.
   */
  fun setLinearVelocity(v: Vec2) {
    if (m_type == BodyType.STATIC) {
      return
    }
    if (Vec2.dot(v, v) > 0.0f) {
      setAwake(true)
    }
    m_linearVelocity.set(v)
  }

  /**
   * Get the linear velocity of the center of mass. Do not modify, instead use
   * {@link #setLinearVelocity(Vec2)}.
   *
   * @return the linear velocity of the center of mass.
   */
  fun getLinearVelocity(): Vec2 {
    return m_linearVelocity
  }

  /**
   * Set the angular velocity.
   *
   * @param omega the new angular velocity in radians/second.
   */
  fun setAngularVelocity(w: Float) {
    if (m_type == BodyType.STATIC) {
      return
    }
    if (w * w > 0f) {
      setAwake(true)
    }
    m_angularVelocity = w
  }

  /**
   * Get the angular velocity.
   *
   * @return the angular velocity in radians/second.
   */
  fun getAngularVelocity(): Float {
    return m_angularVelocity
  }

  /**
   * Get the gravity scale of the body.
   *
   * @return
   */
  fun getGravityScale(): Float {
    return m_gravityScale
  }

  /**
   * Set the gravity scale of the body.
   *
   * @param gravityScale
   */
  fun setGravityScale(gravityScale: Float) {
    this.m_gravityScale = gravityScale
  }

  /**
   * Apply a force at a world point. If the force is not applied at the center of mass, it will
   * generate a torque and affect the angular velocity. This wakes up the body.
   *
   * @param force the world force vector, usually in Newtons (N).
   * @param point the world position of the point of application.
   */
  fun applyForce(force: Vec2, point: Vec2) {
    if (m_type != BodyType.DYNAMIC) {
      return
    }
    if (isAwake() == false) {
      setAwake(true)
    }

    // m_force.addLocal(force);
    // Vec2 temp = tltemp.get();
    // temp.set(point).subLocal(m_sweep.c);
    // m_torque += Vec2.cross(temp, force);
    m_force.x += force.x
    m_force.y += force.y
    m_torque += (point.x - m_sweep.c.x) * force.y - (point.y - m_sweep.c.y) * force.x
  }

  /**
   * Apply a force to the center of mass. This wakes up the body.
   *
   * @param force the world force vector, usually in Newtons (N).
   */
  fun applyForceToCenter(force: Vec2) {
    if (m_type != BodyType.DYNAMIC) {
      return
    }
    if (isAwake() == false) {
      setAwake(true)
    }
    m_force.x += force.x
    m_force.y += force.y
  }

  /**
   * Apply a torque. This affects the angular velocity without affecting the linear velocity of the
   * center of mass. This wakes up the body.
   *
   * @param torque about the z-axis (out of the screen), usually in N-m.
   */
  fun applyTorque(torque: Float) {
    if (m_type != BodyType.DYNAMIC) {
      return
    }
    if (isAwake() == false) {
      setAwake(true)
    }
    m_torque += torque
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
    if (m_type != BodyType.DYNAMIC) {
      return
    }
    if (isAwake() == false) {
      setAwake(true)
    }

    // Vec2 temp = tltemp.get();
    // temp.set(impulse).mulLocal(m_invMass);
    // m_linearVelocity.addLocal(temp);
    //
    // temp.set(point).subLocal(m_sweep.c);
    // m_angularVelocity += m_invI * Vec2.cross(temp, impulse);
    m_linearVelocity.x += impulse.x * m_invMass
    m_linearVelocity.y += impulse.y * m_invMass
    m_angularVelocity +=
      m_invI * ((point.x - m_sweep.c.x) * impulse.y - (point.y - m_sweep.c.y) * impulse.x)
  }

  /**
   * Apply an angular impulse.
   *
   * @param impulse the angular impulse in units of kg*m*m/s
   */
  fun applyAngularImpulse(impulse: Float) {
    if (m_type != BodyType.DYNAMIC) {
      return
    }
    if (isAwake() == false) {
      setAwake(true)
    }
    m_angularVelocity += m_invI * impulse
  }

  /**
   * Get the total mass of the body.
   *
   * @return the mass, usually in kilograms (kg).
   */
  fun getMass(): Float {
    return m_mass
  }

  /**
   * Get the central rotational inertia of the body.
   *
   * @return the rotational inertia, usually in kg-m^2.
   */
  fun getInertia(): Float {
    return (m_I +
      m_mass *
        (m_sweep.localCenter.x * m_sweep.localCenter.x +
          m_sweep.localCenter.y * m_sweep.localCenter.y))
  }

  /**
   * Get the mass data of the body. The rotational inertia is relative to the center of mass.
   *
   * @return a struct containing the mass, inertia and center of the body.
   */
  fun getMassData(data: MassData) {
    // data.mass = m_mass;
    // data.I = m_I + m_mass * Vec2.dot(m_sweep.localCenter, m_sweep.localCenter);
    // data.center.set(m_sweep.localCenter);
    data.mass = m_mass
    data.I =
      (m_I +
        m_mass *
          (m_sweep.localCenter.x * m_sweep.localCenter.x +
            m_sweep.localCenter.y * m_sweep.localCenter.y))
    data.center.x = m_sweep.localCenter.x
    data.center.y = m_sweep.localCenter.y
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
    // assert is not supported in KMP.
    // assert(world.isLocked == false)
    if (m_world.isLocked() == true) {
      return
    }
    if (m_type != BodyType.DYNAMIC) {
      return
    }
    m_invMass = 0.0f
    m_I = 0.0f
    m_invI = 0.0f
    m_mass = massData.mass
    if (m_mass <= 0.0f) {
      m_mass = 1f
    }
    m_invMass = 1.0f / m_mass
    if (massData.I > 0.0f && m_flags and e_fixedRotationFlag == 0) {
      m_I = massData.I - m_mass * Vec2.dot(massData.center, massData.center)
      // assert is not supported in KMP.
      // assert(m_I > 0.0f)
      m_invI = 1.0f / m_I
    }
    val oldCenter = m_world.pool.popVec2()
    // Move center of mass.
    oldCenter.set(m_sweep.c)
    m_sweep.localCenter.set(massData.center)
    // m_sweep.c0 = m_sweep.c = Mul(m_xf, m_sweep.localCenter);
    Transform.mulToOutUnsafe(this.m_xf, m_sweep.localCenter, m_sweep.c0)
    m_sweep.c.set(m_sweep.c0)

    // Update center of mass velocity.
    // m_linearVelocity += Cross(m_angularVelocity, m_sweep.c - oldCenter);
    val temp = m_world.pool.popVec2()
    temp.set(m_sweep.c).subLocal(oldCenter)
    Vec2.crossToOut(m_angularVelocity, temp, temp)
    m_linearVelocity.addLocal(temp)
    m_world.pool.pushVec2(2)
  }

  private val pmd = MassData()

  /**
   * This resets the mass properties to the sum of the mass properties of the fixtures. This
   * normally does not need to be called unless you called setMassData to override the mass and you
   * later want to reset the mass.
   */
  fun resetMassData() {
    // Compute mass data from shapes. Each shape has its own density.
    m_mass = 0.0f
    m_invMass = 0.0f
    m_I = 0.0f
    m_invI = 0.0f
    m_sweep.localCenter.setZero()

    // Static and kinematic bodies have zero mass.
    if (m_type == BodyType.STATIC || m_type == BodyType.KINEMATIC) {
      // m_sweep.c0 = m_sweep.c = m_xf.position;
      m_sweep.c0.set(m_xf.p)
      m_sweep.c.set(m_xf.p)
      m_sweep.a0 = m_sweep.a
      return
    }
    // assert is not supported in KMP.
    // assert(type == BodyType.DYNAMIC)

    // Accumulate mass over all fixtures.
    val localCenter = m_world.pool.popVec2()
    localCenter.setZero()
    val temp = m_world.pool.popVec2()
    val massData = pmd
    var f = m_fixtureList
    while (f != null) {
      if (f.m_density == 0.0f) {
        f = f.m_next
        continue
      }
      f.getMassData(massData)
      m_mass += massData.mass
      // center += massData.mass * massData.center;
      temp.set(massData.center).mulLocal(massData.mass)
      localCenter.addLocal(temp)
      m_I += massData.I
      f = f.m_next
    }

    // Compute center of mass.
    if (m_mass > 0.0f) {
      m_invMass = 1.0f / m_mass
      localCenter.mulLocal(m_invMass)
    } else {
      // Force all dynamic bodies to have a positive mass.
      m_mass = 1.0f
      m_invMass = 1.0f
    }
    if (m_I > 0.0f && (m_flags and e_fixedRotationFlag) == 0) {
      // Center the inertia about the center of mass.
      m_I -= m_mass * Vec2.dot(localCenter, localCenter)
      // assert is not supported in KMP.
      // assert(m_I > 0.0f)
      m_invI = 1.0f / m_I
    } else {
      m_I = 0.0f
      m_invI = 0.0f
    }
    val oldCenter = m_world.pool.popVec2()
    // Move center of mass.
    oldCenter.set(m_sweep.c)
    m_sweep.localCenter.set(localCenter)
    // m_sweep.c0 = m_sweep.c = Mul(m_xf, m_sweep.localCenter);
    Transform.mulToOutUnsafe(m_xf, m_sweep.localCenter, m_sweep.c0)
    m_sweep.c.set(m_sweep.c0)

    // Update center of mass velocity.
    // m_linearVelocity += Cross(m_angularVelocity, m_sweep.c - oldCenter);
    temp.set(m_sweep.c).subLocal(oldCenter)
    Vec2.crossToOutUnsafe(m_angularVelocity, temp, oldCenter)
    m_linearVelocity.addLocal(oldCenter)
    m_world.pool.pushVec2(3)
  }

  /**
   * Get the world coordinates of a point given the local coordinates.
   *
   * @param localPoint a point on the body measured relative the the body's origin.
   * @return the same point expressed in world coordinates.
   */
  fun getWorldPoint(localPoint: Vec2): Vec2 {
    val v = Vec2()
    getWorldPointToOut(localPoint, v)
    return v
  }

  fun getWorldPointToOut(localPoint: Vec2, out: Vec2) {
    Transform.mulToOut(m_xf, localPoint, out)
  }

  /**
   * Get the world coordinates of a vector given the local coordinates.
   *
   * @param localVector a vector fixed in the body.
   * @return the same vector expressed in world coordinates.
   */
  fun getWorldVector(localVector: Vec2): Vec2 {
    val out = Vec2()
    getWorldVectorToOut(localVector, out)
    return out
  }

  fun getWorldVectorToOut(localVector: Vec2, out: Vec2) {
    Rot.mulToOut(m_xf.q, localVector, out)
  }

  fun getWorldVectorToOutUnsafe(localVector: Vec2, out: Vec2) {
    Rot.mulToOutUnsafe(m_xf.q, localVector, out)
  }

  /**
   * Gets a local point relative to the body's origin given a world point.
   *
   * @param a point in world coordinates.
   * @return the corresponding local point relative to the body's origin.
   */
  fun getLocalPoint(worldPoint: Vec2): Vec2 {
    val out = Vec2()
    getLocalPointToOut(worldPoint, out)
    return out
  }

  fun getLocalPointToOut(worldPoint: Vec2, out: Vec2) {
    Transform.mulTransToOut(this.m_xf, worldPoint, out)
  }

  /**
   * Gets a local vector given a world vector.
   *
   * @param a vector in world coordinates.
   * @return the corresponding local vector.
   */
  fun getLocalVector(worldVector: Vec2): Vec2 {
    val out = Vec2()
    getLocalVectorToOut(worldVector, out)
    return out
  }

  fun getLocalVectorToOut(worldVector: Vec2, out: Vec2) {
    Rot.mulTrans(this.m_xf.q, worldVector, out)
  }

  fun getLocalVectorToOutUnsafe(worldVector: Vec2, out: Vec2) {
    Rot.mulTransUnsafe(this.m_xf.q, worldVector, out)
  }

  /**
   * Get the world linear velocity of a world point attached to this body.
   *
   * @param a point in world coordinates.
   * @return the world velocity of a point.
   */
  fun getLinearVelocityFromWorldPoint(worldPoint: Vec2): Vec2 {
    val out = Vec2()
    getLinearVelocityFromWorldPointToOut(worldPoint, out)
    return out
  }

  fun getLinearVelocityFromWorldPointToOut(worldPoint: Vec2, out: Vec2) {
    out.set(worldPoint).subLocal(m_sweep.c)
    Vec2.crossToOut(m_angularVelocity, out, out)
    out.addLocal(m_linearVelocity)
  }

  /**
   * Get the world velocity of a local point.
   *
   * @param a point in local coordinates.
   * @return the world velocity of a point.
   */
  fun getLinearVelocityFromLocalPoint(localPoint: Vec2): Vec2 {
    val out = Vec2()
    getLinearVelocityFromLocalPointToOut(localPoint, out)
    return out
  }

  fun getLinearVelocityFromLocalPointToOut(localPoint: Vec2, out: Vec2) {
    getWorldPointToOut(localPoint, out)
    getLinearVelocityFromWorldPointToOut(out, out)
  }

  /** Get the linear damping of the body. */
  fun getLinearDamping(): Float {
    return m_linearDamping
  }

  /** Set the linear damping of the body. */
  fun setLinearDamping(linearDamping: Float) {
    m_linearDamping = linearDamping
  }

  /** Get the angular damping of the body. */
  fun getAngularDamping(): Float {
    return m_angularDamping
  }

  /** Set the angular damping of the body. */
  fun setAngularDamping(angularDamping: Float) {
    m_angularDamping = angularDamping
  }

  fun getType(): BodyType {
    return m_type
  }

  /**
   * Set the type of this body. This may alter the mass and velocity.
   *
   * @param type
   */
  fun setType(type: BodyType) {
    // assert is not supported in KMP.
    // assert(world.isLocked == false)
    if (m_world.isLocked() == true) {
      return
    }
    if (m_type == type) {
      return
    }
    m_type = type
    resetMassData()
    if (m_type == BodyType.STATIC) {
      m_linearVelocity.setZero()
      m_angularVelocity = 0.0f
      m_sweep.a0 = m_sweep.a
      m_sweep.c0.set(m_sweep.c)
      synchronizeFixtures()
    }
    setAwake(true)
    m_force.setZero()
    m_torque = 0.0f

    // Delete the attached contacts.
    var ce = m_contactList
    while (ce != null) {
      val ce0: ContactEdge = ce
      ce = ce.next
      m_world.m_contactManager.destroy(ce0.contact!!)
    }
    m_contactList = null

    // Touch the proxies so that new contacts will be created (when appropriate)
    val broadPhase = m_world.m_contactManager.m_broadPhase
    var f = m_fixtureList
    while (f != null) {
      val proxyCount = f.m_proxyCount
      for (i in 0 until proxyCount) {
        broadPhase.touchProxy(f.m_proxies!![i]!!.proxyId)
      }
      f = f.m_next
    }
  }

  /** Is this body treated like a bullet for continuous collision detection? */
  fun isBullet(): Boolean {
    return m_flags and e_bulletFlag == e_bulletFlag
  }

  /** Should this body be treated like a bullet for continuous collision detection? */
  fun setBullent(flag: Boolean) {
    m_flags =
      if (flag) {
        m_flags or e_bulletFlag
      } else {
        m_flags and e_bulletFlag.inv()
      }
  }

  /**
   * You can disable sleeping on this body. If you disable sleeping, the body will be woken.
   *
   * @param flag
   */
  fun setSleepingAllowed(flag: Boolean) {
    if (flag) {
      m_flags = m_flags or e_autoSleepFlag
    } else {
      m_flags = m_flags and e_autoSleepFlag.inv()
      setAwake(true)
    }
  }

  /**
   * Is this body allowed to sleep
   *
   * @return
   */
  fun isSleepingAllowed(): Boolean {
    return (m_flags and e_autoSleepFlag) == e_autoSleepFlag
  }

  /**
   * Set the sleep state of the body. A sleeping body has very low CPU cost.
   *
   * @param flag set to true to put body to sleep, false to wake it.
   * @param flag
   */
  fun setAwake(flag: Boolean) {
    if (flag) {
      if (m_flags and e_awakeFlag == 0) {
        m_flags = m_flags or e_awakeFlag
        m_userData = 0.0f
      }
    } else {
      m_flags = m_flags and e_awakeFlag.inv()
      m_userData = 0.0f
      m_linearVelocity.setZero()
      m_angularVelocity = 0.0f
      m_force.setZero()
      m_torque = 0.0f
    }
  }

  /**
   * Get the sleeping state of this body.
   *
   * @return true if the body is sleeping.
   */
  fun isAwake(): Boolean {
    return (m_flags and e_awakeFlag) == e_awakeFlag
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
   *
   * @param flag
   */
  fun setIsActive(flag: Boolean) {
    // assert is not supported in KMP.
    // assert(world.isLocked == false)
    if (flag == isActive()) {
      return
    }
    if (flag) {
      m_flags = m_flags or e_activeFlag

      // Create all proxies.
      val broadPhase = m_world.m_contactManager.m_broadPhase
      var f = m_fixtureList
      while (f != null) {
        f.createProxies(broadPhase, this.m_xf)
        f = f.m_next
      }

      // Contacts are created the next time step.
    } else {
      m_flags = m_flags and e_activeFlag.inv()

      // Destroy all proxies.
      val broadPhase = m_world.m_contactManager.m_broadPhase
      var f = m_fixtureList
      while (f != null) {
        f.destroyProxies(broadPhase)
        f = f.m_next
      }

      // Destroy the attached contacts.
      var ce = m_contactList
      while (ce != null) {
        val ce0: ContactEdge = ce
        ce = ce.next
        m_world.m_contactManager.destroy(ce0.contact!!)
      }
      m_contactList = null
    }
  }

  /**
   * Get the active state of the body.
   *
   * @return
   */
  fun isActive(): Boolean {
    return (m_flags and e_activeFlag) == e_activeFlag
  }

  /**
   * Set this body to have fixed rotation. This causes the mass to be reset.
   *
   * @param flag
   */
  fun setFixedRotation(flag: Boolean) {
    m_flags =
      if (flag) {
        m_flags or e_fixedRotationFlag
      } else {
        m_flags and e_fixedRotationFlag.inv()
      }
    resetMassData()
  }

  /**
   * Does this body have fixed rotation?
   *
   * @return
   */
  fun isFixedRotation(): Boolean {
    return m_flags and Body.e_fixedRotationFlag == Body.e_fixedRotationFlag
  }

  /** Get the list of all fixtures attached to this body. */
  fun getFixtureList(): Fixture {
    return m_fixtureList!!
  }

  /** Get the list of all joints attached to this body. */
  fun getJointList(): JointEdge {
    return m_jointList!!
  }

  /**
   * Get the list of all contacts attached to this body.
   *
   * @warning this list changes during the time step and you may miss some collisions if you don't
   *   use ContactListener.
   */
  fun getContactList(): ContactEdge {
    return m_contactList!!
  }

  /** Get the next body in the world's body list. */
  fun getNext(): Body {
    return m_next!!
  }

  /** Get the user data pointer that was provided in the body definition. */
  fun getUserData(): Any? {
    return m_userData
  }

  /** Set the user data. Use this to store your application specific data. */
  fun setUserData(data: Any) {
    m_userData = data
  }

  /** Get the parent world of this body. */
  fun getWorld(): World {
    return m_world
  }

  // djm pooling
  private val pxf = Transform()

  fun synchronizeFixtures() {
    val xf1 = pxf
    // xf1.position = m_sweep.c0 - Mul(xf1.R, m_sweep.localCenter);

    // xf1.q.set(m_sweep.a0);
    // Rot.mulToOutUnsafe(xf1.q, m_sweep.localCenter, xf1.p);
    // xf1.p.mulLocal(-1).addLocal(m_sweep.c0);
    // inlined:
    xf1.q.sin = MathUtils.sin(m_sweep.a0)
    xf1.q.cos = MathUtils.cos(m_sweep.a0)
    xf1.p.x = m_sweep.c0.x - xf1.q.cos * m_sweep.localCenter.x + xf1.q.sin * m_sweep.localCenter.y
    xf1.p.y = m_sweep.c0.y - xf1.q.sin * m_sweep.localCenter.x - xf1.q.cos * m_sweep.localCenter.y
    // end inline
    var f = m_fixtureList
    while (f != null) {
      f.synchronize(m_world.m_contactManager.m_broadPhase, xf1, m_xf)
      f = f.m_next
    }
  }

  fun synchronizeTransform() {
    // m_xf.q.set(m_sweep.a);
    //
    // // m_xf.position = m_sweep.c - Mul(m_xf.R, m_sweep.localCenter);
    // Rot.mulToOutUnsafe(m_xf.q, m_sweep.localCenter, m_xf.p);
    // m_xf.p.mulLocal(-1).addLocal(m_sweep.c);
    //
    m_xf.q.sin = MathUtils.sin(m_sweep.a)
    m_xf.q.cos = MathUtils.cos(m_sweep.a)
    val q = m_xf.q
    val v = m_sweep.localCenter
    m_xf.p.x = m_sweep.c.x - q.cos * v.x + q.sin * v.y
    m_xf.p.y = m_sweep.c.y - q.sin * v.x - q.cos * v.y
  }

  /**
   * This is used to prevent connected bodies from colliding. It may lie, depending on the
   * collideConnected flag.
   *
   * @param other
   * @return
   */
  fun shouldCollide(other: Body): Boolean {
    // At least one body should be dynamic.
    if (m_type != BodyType.DYNAMIC && other.m_type != BodyType.DYNAMIC) {
      return false
    }

    // Does a joint prevent collision?
    var jn = m_jointList
    while (jn != null) {
      if (jn.other === other) {
        if (jn.joint!!.m_collideConnected == false) {
          return false
        }
      }
      jn = jn.next
    }
    return true
  }

  fun advance(t: Float) {
    // Advance to the new safe time. This doesn't sync the broad-phase.
    m_sweep.advance(t)
    m_sweep.c.set(m_sweep.c0)
    m_sweep.a = m_sweep.a0
    m_xf.q.set(m_sweep.a)
    // m_xf.position = m_sweep.c - Mul(m_xf.R, m_sweep.localCenter);
    Rot.mulToOutUnsafe(m_xf.q, m_sweep.localCenter, m_xf.p)
    m_xf.p.mulLocal(-1f).addLocal(m_sweep.c)
  }

  companion object {
    const val e_islandFlag = 0x0001
    const val e_awakeFlag = 0x0002
    const val e_autoSleepFlag = 0x0004
    const val e_bulletFlag = 0x0008
    const val e_fixedRotationFlag = 0x0010
    const val e_activeFlag = 0x0020
    const val e_toiFlag = 0x0040
  }
}
