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

import org.jbox2d.callbacks.ContactFilter
import org.jbox2d.callbacks.ContactListener
import org.jbox2d.callbacks.DebugDraw
import org.jbox2d.callbacks.DestructionListener
import org.jbox2d.callbacks.QueryCallback
import org.jbox2d.callbacks.RayCastCallback
import org.jbox2d.callbacks.TreeCallback
import org.jbox2d.callbacks.TreeRayCastCallback
import org.jbox2d.collision.AABB
import org.jbox2d.collision.RayCastInput
import org.jbox2d.collision.RayCastOutput
import org.jbox2d.collision.TimeOfImpact
import org.jbox2d.collision.broadphase.BroadPhase
import org.jbox2d.collision.broadphase.BroadPhaseStrategy
import org.jbox2d.collision.broadphase.DynamicTree
import org.jbox2d.collision.shapes.ChainShape
import org.jbox2d.collision.shapes.CircleShape
import org.jbox2d.collision.shapes.EdgeShape
import org.jbox2d.collision.shapes.PolygonShape
import org.jbox2d.collision.shapes.ShapeType
import org.jbox2d.common.Color3f
import org.jbox2d.common.MathUtils
import org.jbox2d.common.Settings
import org.jbox2d.common.Sweep
import org.jbox2d.common.Timer
import org.jbox2d.common.Transform
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.contacts.Contact
import org.jbox2d.dynamics.contacts.ContactRegister
import org.jbox2d.dynamics.joints.Joint
import org.jbox2d.dynamics.joints.JointDef
import org.jbox2d.dynamics.joints.JointType
import org.jbox2d.dynamics.joints.PulleyJoint
import org.jbox2d.pooling.IDynamicStack
import org.jbox2d.pooling.IWorldPool
import org.jbox2d.pooling.arrays.Vec2Array
import org.jbox2d.pooling.normal.DefaultWorldPool

/**
 * The world class manages all physics entities, dynamic simulation, and asynchronous queries. The
 * world also contains efficient memory management facilities.
 *
 * @author Daniel Murphy
 */
class World(gravity: Vec2, val pool: IWorldPool, broadPhaseStrategy: BroadPhaseStrategy) {
  // statistics gathering
  var activeContacts = 0
  var contactPoolCount = 0
  var m_flags: Int

  var m_contactManager: ContactManager
  private var m_bodyList: Body? = null
  private var m_jointList: Joint? = null
  private var m_bodyCount = 0
  private var m_jointCount = 0
  private val m_gravity = Vec2()
  private var m_allowSleep = true

  // private Body m_groundBody;
  private var m_destructionListener: DestructionListener? = null
  private var m_debugDraw: DebugDraw? = null

  private var m_inv_dt0: Float = 0f

  // these are for debugging the solver
  private var m_warmStarting = true
  private var m_continuousPhysics = true
  private var m_subStepping = false
  private var m_stepComplete = true
  private val m_profile: Profile

  private val contactStacks =
    Array(ShapeType.values().size) { arrayOfNulls<ContactRegister>(ShapeType.values().size) }

  /**
   * Construct a world object.
   *
   * @param gravity the world gravity vector.
   */
  constructor(
    gravity: Vec2,
    pool: IWorldPool = DefaultWorldPool(WORLD_POOL_SIZE, WORLD_POOL_CONTAINER_SIZE)
  ) : this(gravity, pool, DynamicTree()) {}

  init {
    m_gravity.set(gravity)
    m_flags = CLEAR_FORCES
    m_inv_dt0 = 0f
    m_contactManager = ContactManager(this, broadPhaseStrategy)
    m_profile = Profile()
    initializeRegisters()
  }

  fun setAllowSleep(flag: Boolean) {
    if (flag == m_allowSleep) {
      return
    }
    m_allowSleep = flag
    if (m_allowSleep == false) {
      var b = m_bodyList
      while (b != null) {
        b.setAwake(true)
        b = b.m_next
      }
    }
  }

  fun setSubStepping(subStepping: Boolean) {
    m_subStepping = subStepping
  }

  fun isSubStepping(): Boolean {
    return m_subStepping
  }

  fun isAllowSleep(): Boolean {
    return m_allowSleep
  }

  private fun addType(creator: IDynamicStack<Contact>, type1: ShapeType, type2: ShapeType) {
    val register = ContactRegister()
    register.creator = creator
    register.primary = true
    contactStacks[type1.ordinal][type2.ordinal] = register
    if (type1 != type2) {
      val register2 = ContactRegister()
      register2.creator = creator
      register2.primary = false
      contactStacks[type2.ordinal][type1.ordinal] = register2
    }
  }

  private fun initializeRegisters() {
    addType(pool.getCircleContactStack(), ShapeType.CIRCLE, ShapeType.CIRCLE)
    addType(pool.getPolyCircleContactStack(), ShapeType.POLYGON, ShapeType.CIRCLE)
    addType(pool.getPolyContactStack(), ShapeType.POLYGON, ShapeType.POLYGON)
    addType(pool.getEdgeCircleContactStack(), ShapeType.EDGE, ShapeType.CIRCLE)
    addType(pool.getEdgePolyContactStack(), ShapeType.EDGE, ShapeType.POLYGON)
    addType(pool.getChainCircleContactStack(), ShapeType.CHAIN, ShapeType.CIRCLE)
    addType(pool.getChainPolyContactStack(), ShapeType.CHAIN, ShapeType.POLYGON)
  }

  fun popContact(fixtureA: Fixture, indexA: Int, fixtureB: Fixture, indexB: Int): Contact? {
    val type1 = fixtureA.getType()
    val type2 = fixtureB.getType()
    val reg = contactStacks[type1.ordinal][type2.ordinal]
    val creator = reg!!.creator
    return if (creator != null) {
      if (reg.primary) {
        val c = creator.pop()
        c.init(fixtureA, indexA, fixtureB, indexB)
        c
      } else {
        val c = creator.pop()
        c.init(fixtureB, indexB, fixtureA, indexA)
        c
      }
    } else {
      null
    }
  }

  fun pushContact(contact: Contact) {
    val fixtureA = contact.m_fixtureA!!
    val fixtureB = contact.m_fixtureB!!
    if (contact.m_manifold.pointCount > 0 && !fixtureA.isSensor() && !fixtureB.isSensor()) {
      fixtureA.m_body!!.setAwake(true)
      fixtureB.m_body!!.setAwake(true)
    }
    val type1 = fixtureA.getType()
    val type2 = fixtureB.getType()
    val creator = contactStacks[type1.ordinal][type2.ordinal]!!.creator
    creator!!.push(contact)
  }

  /**
   * Register a destruction listener. The listener is owned by you and must remain in scope.
   *
   * @param listener
   */
  fun setDestructionListener(listener: DestructionListener) {
    m_destructionListener = listener
  }

  /**
   * Register a contact filter to provide specific control over collision. Otherwise the default
   * filter is used (_defaultFilter). The listener is owned by you and must remain in scope.
   *
   * @param filter
   */
  fun setContactFilter(filter: ContactFilter) {
    m_contactManager.m_contactFilter = filter
  }

  /**
   * Register a contact event listener. The listener is owned by you and must remain in scope.
   *
   * @param listener
   */
  fun setContactListener(listener: ContactListener) {
    m_contactManager.m_contactListener = listener
  }

  /**
   * Register a routine for debug drawing. The debug draw functions are called inside with
   * World.DrawDebugData method. The debug draw object is owned by you and must remain in scope.
   *
   * @param debugDraw
   */
  fun setDebugDraw(debugDraw: DebugDraw) {
    m_debugDraw = debugDraw
  }

  /**
   * create a rigid body given a definition. No reference to the definition is retained.
   *
   * @param def
   * @return
   * @warning This function is locked during callbacks.
   */
  fun createBody(def: BodyDef): Body? {
    // assert is not supported in KMP.
    // assert(isLocked == false)
    if (isLocked()) {
      return null
    }
    // TODO djm pooling
    val b = Body(def, this)

    // add to world doubly linked list
    b.m_prev = null
    b.m_next = m_bodyList
    if (m_bodyList != null) {
      m_bodyList!!.m_prev = b
    }
    m_bodyList = b
    ++m_bodyCount
    return b
  }

  /**
   * destroy a rigid body given a definition. No reference to the definition is retained. This
   * function is locked during callbacks.
   *
   * @param body
   * @warning This automatically deletes all associated shapes and joints.
   * @warning This function is locked during callbacks.
   */
  fun destroyBody(body: Body) {
    // assert is not supported in KMP.
    // assert(bodyCount > 0)
    // assert(isLocked == false)
    if (isLocked()) {
      return
    }

    // Delete the attached joints.
    var je = body.m_jointList
    while (je != null) {
      val je0 = je
      je = je.next
      if (m_destructionListener != null) {
        m_destructionListener!!.sayGoodbye(je0.joint!!)
      }
      destroyJoint(je0.joint!!)
      body.m_jointList = je
    }
    body.m_jointList = null

    // Delete the attached contacts.
    var ce = body.m_contactList
    while (ce != null) {
      val ce0 = ce
      ce = ce.next
      m_contactManager.destroy(ce0.contact!!)
    }
    body.m_contactList = null
    var f = body.m_fixtureList
    while (f != null) {
      val f0 = f
      f = f.m_next
      if (m_destructionListener != null) {
        m_destructionListener!!.sayGoodbye(f0)
      }
      f0.destroyProxies(m_contactManager.m_broadPhase)
      f0.destroy()
      // TODO djm recycle fixtures (here or in that destroy method)
      body.m_fixtureList = f
      body.m_fixtureCount -= 1
    }
    body.m_fixtureList = null
    body.m_fixtureCount = 0

    // Remove world body list.
    if (body.m_prev != null) {
      body.m_prev!!.m_next = body.m_next
    }
    if (body.m_next != null) {
      body.m_next!!.m_prev = body.m_prev
    }
    if (body === m_bodyList) {
      m_bodyList = body.m_next
    }
    --m_bodyCount
    // TODO djm recycle body
  }

  /**
   * create a joint to constrain bodies together. No reference to the definition is retained. This
   * may cause the connected bodies to cease colliding.
   *
   * @param def
   * @return
   * @warning This function is locked during callbacks.
   */
  fun createJoint(def: JointDef): Joint? {
    // assert is not supported in KMP.
    // assert(isLocked == false)
    if (isLocked()) {
      return null
    }
    val j = Joint.create(this, def)

    // Connect to the world list.
    j!!.m_prev = null
    j.m_next = m_jointList
    if (m_jointList != null) {
      m_jointList!!.m_prev = j
    }
    m_jointList = j
    ++m_jointCount

    // Connect to the bodies' doubly linked lists.
    j.m_edgeA.joint = j
    j.m_edgeA.other = j.m_bodyB
    j.m_edgeA.prev = null
    j.m_edgeA.next = j.m_bodyA!!.m_jointList
    if (j.m_bodyA!!.m_jointList != null) {
      j.m_bodyA!!.m_jointList!!.prev = j.m_edgeA
    }
    j.m_bodyA!!.m_jointList = j.m_edgeA
    j.m_edgeB.joint = j
    j.m_edgeB.other = j.m_bodyA
    j.m_edgeB.prev = null
    j.m_edgeB.next = j.m_bodyB!!.m_jointList
    if (j.m_bodyB!!.m_jointList != null) {
      j.m_bodyB!!.m_jointList!!.prev = j.m_edgeB
    }
    j.m_bodyB!!.m_jointList = j.m_edgeB
    val bodyA = def.bodyA
    val bodyB = def.bodyB

    // If the joint prevents collisions, then flag any contacts for filtering.
    if (def.collideConnected == false) {
      var edge = bodyB!!.m_contactList
      while (edge != null) {
        if (edge.other === bodyA) {
          // Flag the contact for filtering at the next time step (where either
          // body is awake).
          edge.contact!!.flagForFiltering()
        }
        edge = edge.next
      }
    }

    // Note: creating a joint doesn't wake the bodies.
    return j
  }

  /**
   * destroy a joint. This may cause the connected bodies to begin colliding.
   *
   * @param joint
   * @warning This function is locked during callbacks.
   */
  fun destroyJoint(j: Joint) {
    // assert is not supported in KMP.
    // assert(isLocked == false)
    if (isLocked()) {
      return
    }
    val collideConnected = j.m_collideConnected

    // Remove from the doubly linked list.
    if (j.m_prev != null) {
      j.m_prev!!.m_next = j.m_next
    }
    if (j.m_next != null) {
      j.m_next!!.m_prev = j.m_prev
    }
    if (j === m_jointList) {
      m_jointList = j.m_next
    }

    // Disconnect from island graph.
    val bodyA = j.m_bodyA
    val bodyB = j.m_bodyB

    // Wake up connected bodies.
    bodyA!!.setAwake(true)
    bodyB!!.setAwake(true)

    // Remove from body 1.
    if (j.m_edgeA.prev != null) {
      j.m_edgeA.prev!!.next = j.m_edgeA.next
    }
    if (j.m_edgeA.next != null) {
      j.m_edgeA.next!!.prev = j.m_edgeA.prev
    }
    if (j.m_edgeA === bodyA.m_jointList) {
      bodyA.m_jointList = j.m_edgeA.next
    }
    j.m_edgeA.prev = null
    j.m_edgeA.next = null

    // Remove from body 2
    if (j.m_edgeB.prev != null) {
      j.m_edgeB.prev!!.next = j.m_edgeB.next
    }
    if (j.m_edgeB.next != null) {
      j.m_edgeB.next!!.prev = j.m_edgeB.prev
    }
    if (j.m_edgeB === bodyB.m_jointList) {
      bodyB.m_jointList = j.m_edgeB.next
    }
    j.m_edgeB.prev = null
    j.m_edgeB.next = null
    Joint.destroy(j)
    // assert is not supported in KMP.
    // assert(jointCount > 0)
    --m_jointCount

    // If the joint prevents collisions, then flag any contacts for filtering.
    if (collideConnected == false) {
      var edge = bodyB.m_contactList
      while (edge != null) {
        if (edge.other === bodyA) {
          // Flag the contact for filtering at the next time step (where either
          // body is awake).
          edge.contact!!.flagForFiltering()
        }
        edge = edge.next
      }
    }
  }

  // djm pooling
  private val step = TimeStep()
  private val stepTimer = Timer()
  private val tempTimer = Timer()

  /**
   * Take a time step. This performs collision detection, integration, and constraint solution.
   *
   * @param timeStep the amount of time to simulate, this should not vary.
   * @param velocityIterations for the velocity constraint solver.
   * @param positionIterations for the position constraint solver.
   */
  fun step(dt: Float, velocityIterations: Int, positionIterations: Int) {
    stepTimer.reset()
    // log.debug("Starting step");
    // If new fixtures were added, we need to find the new contacts.
    if (m_flags and NEW_FIXTURE == NEW_FIXTURE) {
      // log.debug("There's a new fixture, lets look for new contacts");
      m_contactManager.findNewContacts()
      m_flags = m_flags and NEW_FIXTURE.inv()
    }
    m_flags = m_flags or LOCKED
    step.dt = dt
    step.velocityIterations = velocityIterations
    step.positionIterations = positionIterations
    if (dt > 0.0f) {
      step.inv_dt = 1.0f / dt
    } else {
      step.inv_dt = 0.0f
    }
    step.dtRatio = m_inv_dt0 * dt
    step.warmStarting = m_warmStarting

    // Update contacts. This is where some contacts are destroyed.
    tempTimer.reset()
    m_contactManager.collide()
    m_profile.collide = tempTimer.milliseconds

    // Integrate velocities, solve velocity constraints, and integrate positions.
    if (m_stepComplete && step.dt > 0.0f) {
      tempTimer.reset()
      solve(step)
      m_profile.solve = tempTimer.milliseconds
    }

    // Handle TOI events.
    if (m_continuousPhysics && step.dt > 0.0f) {
      tempTimer.reset()
      solveTOI(step)
      m_profile.solveTOI = tempTimer.milliseconds
    }
    if (step.dt > 0.0f) {
      m_inv_dt0 = step.inv_dt
    }
    if (m_flags and CLEAR_FORCES == CLEAR_FORCES) {
      clearForces()
    }
    m_flags = m_flags and LOCKED.inv()
    // log.debug("ending step");
    m_profile.step = stepTimer.milliseconds
  }

  /**
   * Call this after you are done with time steps to clear the forces. You normally call this after
   * each call to Step, unless you are performing sub-steps. By default, forces will be
   * automatically cleared, so you don't need to call this function.
   *
   * @see setAutoClearForces
   */
  fun clearForces() {
    var body = m_bodyList
    while (body != null) {
      body.m_force.setZero()
      body.m_torque = 0.0f
      body = body.m_next
    }
  }

  private val color = Color3f()
  private val xf = Transform()
  private val cA = Vec2()
  private val cB = Vec2()
  private val avs = Vec2Array()

  /** Call this to draw shapes and other debug draw data. */
  fun drawDebugData() {
    if (m_debugDraw == null) {
      return
    }
    val flags: Int = m_debugDraw!!.flags
    if (flags and DebugDraw.e_shapeBit == DebugDraw.e_shapeBit) {
      var b = m_bodyList
      while (b != null) {
        xf.set(b.m_xf)
        var f = b.m_fixtureList
        while (f != null) {
          if (b.isActive() == false) {
            color.set(0.5f, 0.5f, 0.3f)
            drawShape(f, xf, color)
          } else if (b.getType() == BodyType.STATIC) {
            color.set(0.5f, 0.9f, 0.3f)
            drawShape(f, xf, color)
          } else if (b.getType() == BodyType.KINEMATIC) {
            color.set(0.5f, 0.5f, 0.9f)
            drawShape(f, xf, color)
          } else if (b.isAwake() == false) {
            color.set(0.5f, 0.5f, 0.5f)
            drawShape(f, xf, color)
          } else {
            color.set(0.9f, 0.7f, 0.7f)
            drawShape(f, xf, color)
          }
          f = f.m_next
        }
        b = b.m_next
      }
    }
    if (flags and DebugDraw.e_jointBit == DebugDraw.e_jointBit) {
      var j = m_jointList
      while (j != null) {
        drawJoint(j)
        j = j.m_next
      }
    }
    if (flags and DebugDraw.e_pairBit == DebugDraw.e_pairBit) {
      color.set(0.3f, 0.9f, 0.9f)
      var c = m_contactManager.m_contactList
      while (c != null) {
        val fixtureA = c.m_fixtureA
        val fixtureB = c.m_fixtureB
        fixtureA!!.getAABB(c.m_indexA)!!.getCenterToOut(cA)
        fixtureB!!.getAABB(c.m_indexB)!!.getCenterToOut(cB)
        m_debugDraw!!.drawSegment(cA, cB, color)
        c = c.m_next
      }
    }
    if (flags and DebugDraw.e_aabbBit == DebugDraw.e_aabbBit) {
      color.set(0.9f, 0.3f, 0.9f)
      var b = m_bodyList
      while (b != null) {
        if (b.isActive() == false) {
          b = b.m_next
          continue
        }
        var f = b.m_fixtureList
        while (f != null) {
          for (i in 0 until f.m_proxyCount) {
            val proxy = f.m_proxies!![i]
            val aabb = m_contactManager.m_broadPhase.getFatAABB(proxy!!.proxyId)
            val vs: Array<Vec2> = avs[4]
            vs[0].set(aabb.lowerBound.x, aabb.lowerBound.y)
            vs[1].set(aabb.upperBound.x, aabb.lowerBound.y)
            vs[2].set(aabb.upperBound.x, aabb.upperBound.y)
            vs[3].set(aabb.lowerBound.x, aabb.upperBound.y)
            m_debugDraw!!.drawPolygon(vs, 4, color)
          }
          f = f.m_next
        }
        b = b.m_next
      }
    }
    if (flags and DebugDraw.e_centerOfMassBit == DebugDraw.e_centerOfMassBit) {
      var b = m_bodyList
      while (b != null) {
        xf.set(b.m_xf)
        xf.p.set(b.m_sweep.c)
        m_debugDraw!!.drawTransform(xf)
        b = b.m_next
      }
    }
    if (flags and DebugDraw.e_dynamicTreeBit == DebugDraw.e_dynamicTreeBit) {
      m_contactManager.m_broadPhase.drawTree(m_debugDraw!!)
    }
  }

  private val wqwrapper = WorldQueryWrapper()

  /**
   * Query the world for all fixtures that potentially overlap the provided AABB.
   *
   * @param callback a user implemented callback class.
   * @param aabb the query box.
   */
  fun queryAABB(callback: QueryCallback, aabb: AABB) {
    wqwrapper.broadPhase = m_contactManager.m_broadPhase
    wqwrapper.callback = callback
    m_contactManager.m_broadPhase.query(wqwrapper, aabb)
  }

  private val wrcwrapper = WorldRayCastWrapper()
  private val input = RayCastInput()

  /**
   * Ray-cast the world for all fixtures in the path of the ray. Your callback controls whether you
   * get the closest point, any point, or n-points. The ray-cast ignores shapes that contain the
   * starting point.
   *
   * @param callback a user implemented callback class.
   * @param point1 the ray starting point
   * @param point2 the ray ending point
   */
  fun raycast(callback: RayCastCallback, point1: Vec2, point2: Vec2) {
    wrcwrapper.broadPhase = m_contactManager.m_broadPhase
    wrcwrapper.callback = callback
    input.maxFraction = 1.0f
    input.p1.set(point1)
    input.p2.set(point2)
    m_contactManager.m_broadPhase.raycast(wrcwrapper, input)
  }

  /**
   * Get the world body list. With the returned body, use Body.getNext to get the next body in the
   * world list. A null body indicates the end of the list.
   *
   * @return the head of the world body list.
   */
  fun getBodyList(): Body {
    return m_bodyList!!
  }

  /**
   * Get the world joint list. With the returned joint, use Joint.getNext to get the next joint in
   * the world list. A null joint indicates the end of the list.
   *
   * @return the head of the world joint list.
   */
  fun getJointList(): Joint {
    return m_jointList!!
  }

  /**
   * Get the world contact list. With the returned contact, use Contact.getNext to get the next
   * contact in the world list. A null contact indicates the end of the list.
   *
   * @return the head of the world contact list.
   * @warning contacts are created and destroyed in the middle of a time step. Use ContactListener
   *   to avoid missing contacts.
   */
  fun getContactList(): Contact {
    return m_contactManager.m_contactList!!
  }

  fun isSleepingAllowed(): Boolean {
    return m_allowSleep
  }

  fun setSleepingAllowed(sleepingAllowed: Boolean) {
    m_allowSleep = sleepingAllowed
  }

  /**
   * Enable/disable warm starting. For testing.
   *
   * @param flag
   */
  fun setWarmStarting(flag: Boolean) {
    m_warmStarting = flag
  }

  fun isWarmStarting(): Boolean {
    return m_warmStarting
  }

  /**
   * Enable/disable continuous physics. For testing.
   *
   * @param flag
   */
  fun setContinuousPhysics(flag: Boolean) {
    m_continuousPhysics = flag
  }

  fun isContinuousPhysics(): Boolean {
    return m_continuousPhysics
  }

  /**
   * Get the number of broad-phase proxies.
   *
   * @return
   */
  fun getProxyCount(): Int {
    return m_contactManager.m_broadPhase.getProxyCount()
  }

  /**
   * Get the number of bodies.
   *
   * @return
   */
  fun getBodyCount(): Int {
    return m_bodyCount
  }

  /**
   * Get the number of joints.
   *
   * @return
   */
  fun getJointCount(): Int {
    return m_jointCount
  }

  /**
   * Get the number of contacts (each may have 0 or more contact points).
   *
   * @return
   */
  fun getContactCount(): Int {
    return m_contactManager.m_contactCount
  }

  /**
   * Gets the height of the dynamic tree
   *
   * @return
   */
  fun getTreeHeight(): Int {
    return m_contactManager.m_broadPhase.getTreeHeight()
  }

  /**
   * Gets the balance of the dynamic tree
   *
   * @return
   */
  fun getTreeBalance(): Int {
    return m_contactManager.m_broadPhase.getTreeBalance()
  }

  /**
   * Gets the quality of the dynamic tree
   *
   * @return
   */
  fun getTreeQuality(): Float {
    return m_contactManager.m_broadPhase.getTreeQuality()
  }

  /**
   * Change the global gravity vector.
   *
   * @param gravity
   */
  fun setGravity(gravity: Vec2) {
    m_gravity.set(gravity)
  }

  /**
   * Get the global gravity vector.
   *
   * @return
   */
  fun getGravity(): Vec2 {
    return m_gravity
  }

  /**
   * Is the world locked (in the middle of a time step).
   *
   * @return
   */
  fun isLocked(): Boolean {
    return (m_flags and LOCKED) == LOCKED
  }

  /**
   * Set flag to control automatic clearing of forces after each time step.
   *
   * @param flag
   */
  fun setAutoClearForces(flag: Boolean) {
    m_flags =
      if (flag) {
        m_flags or CLEAR_FORCES
      } else {
        m_flags and CLEAR_FORCES.inv()
      }
  }

  /**
   * Get the flag that controls automatic clearing of forces after each time step.
   *
   * @return
   */
  fun getAutoClearForces(): Boolean {
    return m_flags and CLEAR_FORCES == CLEAR_FORCES
  }

  /**
   * Get the contact manager for testing purposes
   *
   * @return
   */
  fun getContactManager(): ContactManager {
    return m_contactManager
  }

  fun getProfile(): Profile {
    return m_profile
  }

  private val island = Island()
  private var stack = arrayOfNulls<Body>(10) // TODO djm find a good initial stack number;
  private val islandProfile = Profile()
  private val broadphaseTimer = Timer()

  private fun solve(step: TimeStep) {
    m_profile.solveInit = 0f
    m_profile.solveVelocity = 0f
    m_profile.solvePosition = 0f

    // Size the island for the worst case.
    island.initialize(
      m_bodyCount,
      m_contactManager.m_contactCount,
      m_jointCount,
      m_contactManager.m_contactListener
    )

    // Clear all the island flags.
    var body: Body? = this.m_bodyList
    while (body != null) {
      body.m_flags = body.m_flags and Body.Companion.e_islandFlag.inv()
      body = body.m_next
    }

    var c = m_contactManager.m_contactList
    while (c != null) {
      c.m_flags = c.m_flags and Contact.ISLAND_FLAG.inv()
      c = c.m_next
    }
    var j = m_jointList
    while (j != null) {
      j.m_islandFlag = false
      j = j.m_next
    }

    // Build and simulate all awake islands.
    val stackSize = m_bodyCount
    if (stack.size < stackSize) {
      stack = arrayOfNulls(stackSize)
    }
    var seed = m_bodyList
    while (seed != null) {
      if (seed.m_flags and Body.Companion.e_islandFlag == Body.Companion.e_islandFlag) {
        seed = seed.m_next
        continue
      }
      if (seed.isAwake() == false || seed.isActive() == false) {
        seed = seed.m_next
        continue
      }

      // The seed can be dynamic or kinematic.
      if (seed.m_type == BodyType.STATIC) {
        seed = seed.m_next
        continue
      }

      // Reset island and stack.
      island.clear()
      var stackCount = 0
      stack[stackCount++] = seed
      seed.m_flags = seed.m_flags or Body.Companion.e_islandFlag

      // Perform a depth first search (DFS) on the constraint graph.
      while (stackCount > 0) {
        // Grab the next body off the stack and add it to the island.
        val b = stack[--stackCount]
        // assert is not supported in KMP.
        // assert(b!!.isActive == true)
        island.add(b!!)

        // Make sure the body is awake.
        b.setAwake(true)

        // To keep islands as small as possible, we don't
        // propagate islands across static bodies.
        if (b.m_type == BodyType.STATIC) {
          continue
        }

        // Search all contacts connected to this body.
        var ce = b.m_contactList
        while (ce != null) {
          val contact = ce.contact!!

          // Has this contact already been added to an island?
          if (contact.m_flags and Contact.ISLAND_FLAG == Contact.ISLAND_FLAG) {
            ce = ce.next
            continue
          }

          // Is this contact solid and touching?
          if (contact.isEnabled() == false || contact.isTouching() == false) {
            ce = ce.next
            continue
          }

          // Skip sensors.
          val sensorA = contact.m_fixtureA!!.m_isSensor
          val sensorB = contact.m_fixtureB!!.m_isSensor
          if (sensorA || sensorB) {
            ce = ce.next
            continue
          }
          island.add(contact)
          contact.m_flags = contact.m_flags or Contact.ISLAND_FLAG
          val other = ce.other

          // Was the other body already added to this island?
          if (other!!.m_flags and Body.Companion.e_islandFlag == Body.Companion.e_islandFlag) {
            ce = ce.next
            continue
          }
          // assert is not supported in KMP.
          // assert(stackCount < stackSize)
          stack[stackCount++] = other
          other.m_flags = other.m_flags or Body.Companion.e_islandFlag
          ce = ce.next
        }

        // Search all joints connect to this body.
        var je = b.m_jointList
        while (je != null) {
          if (je.joint!!.m_islandFlag == true) {
            je = je.next
            continue
          }
          val other = je.other!!

          // Don't simulate joints connected to inactive bodies.
          if (other.isActive() == false) {
            je = je.next
            continue
          }
          island.add(je.joint!!)
          je.joint!!.m_islandFlag = true
          if (other.m_flags and Body.Companion.e_islandFlag == Body.Companion.e_islandFlag) {
            je = je.next
            continue
          }
          // assert is not supported in KMP.
          // assert(stackCount < stackSize)
          stack[stackCount++] = other
          other.m_flags = other.m_flags or Body.Companion.e_islandFlag
          je = je.next
        }
      }
      island.solve(islandProfile, step, m_gravity, m_allowSleep)
      m_profile.solveInit += islandProfile.solveInit
      m_profile.solveVelocity += islandProfile.solveVelocity
      m_profile.solvePosition += islandProfile.solvePosition

      // Post solve cleanup.
      for (i in 0 until island.m_bodyCount) {
        // Allow static bodies to participate in other islands.
        val b = island.m_bodies!![i]!!
        if (b.m_type == BodyType.STATIC) {
          b.m_flags = b.m_flags and Body.Companion.e_islandFlag.inv()
        }
      }
      seed = seed.m_next
    }
    broadphaseTimer.reset()
    // Synchronize fixtures, check for out of range bodies.
    var b = m_bodyList
    while (b != null) {

      // If a body was not in an island then it did not move.
      if (b.m_flags and Body.Companion.e_islandFlag == 0) {
        b = b.m_next
        continue
      }
      if (b.m_type == BodyType.STATIC) {
        b = b.m_next
        continue
      }

      // Update fixtures (for broad-phase).
      b.synchronizeFixtures()
      b = b.m_next
    }

    // Look for new contacts.
    m_contactManager.findNewContacts()
    m_profile.broadphase = broadphaseTimer.milliseconds
  }

  private val toiIsland = Island()
  private val toiInput = TimeOfImpact.TOIInput()
  private val toiOutput = TimeOfImpact.TOIOutput()
  private val subStep = TimeStep()
  private val tempBodies = arrayOfNulls<Body>(2)
  private val backup1 = Sweep()
  private val backup2 = Sweep()
  private fun solveTOI(step: TimeStep) {
    val island = toiIsland
    island.initialize(
      2 * Settings.maxTOIContacts,
      Settings.maxTOIContacts,
      0,
      m_contactManager.m_contactListener
    )
    if (m_stepComplete) {
      var b = m_bodyList
      while (b != null) {
        b.m_flags = b.m_flags and Body.Companion.e_islandFlag.inv()
        b.m_sweep.alpha0 = 0.0f
        b = b.m_next
      }
      var c = m_contactManager.m_contactList
      while (c != null) {

        // Invalidate TOI
        c.m_flags = c.m_flags and (Contact.TOI_FLAG or Contact.ISLAND_FLAG).inv()
        c.m_toiCount = 0f
        c.m_toi = 1.0f
        c = c.m_next
      }
    }

    // Find TOI events and solve them.
    while (true) {

      // Find the first TOI.
      var minContact: Contact? = null
      var minAlpha = 1.0f
      var c = m_contactManager.m_contactList
      while (c != null) {

        // Is this contact disabled?
        if (c.isEnabled() == false) {
          c = c.m_next
          continue
        }

        // Prevent excessive sub-stepping.
        if (c.m_toiCount > Settings.maxSubSteps) {
          c = c.m_next
          continue
        }
        @Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER") var alpha = 1.0f
        if (c.m_flags and Contact.TOI_FLAG != 0) {
          // This contact has a valid cached TOI.
          alpha = c.m_toi
        } else {
          val fA = c.m_fixtureA!!
          val fB = c.m_fixtureB!!

          // Is there a sensor?
          if (fA.isSensor() || fB.isSensor()) {
            c = c.m_next
            continue
          }
          val bA = fA.m_body!!
          val bB = fB.m_body!!
          val typeA = bA.m_type
          val typeB = bB.m_type
          // assert is not supported in KMP.
          // assert(typeA == BodyType.DYNAMIC || typeB == BodyType.DYNAMIC)
          val activeA = bA.isAwake() && typeA != BodyType.STATIC
          val activeB = bB.isAwake() && typeB != BodyType.STATIC

          // Is at least one body active (awake and dynamic or kinematic)?
          if (activeA == false && activeB == false) {
            c = c.m_next
            continue
          }
          val collideA = bA.isBullet() || typeA != BodyType.DYNAMIC
          val collideB = bB.isBullet() || typeB != BodyType.DYNAMIC

          // Are these two non-bullet dynamic bodies?
          if (collideA == false && collideB == false) {
            c = c.m_next
            continue
          }

          // Compute the TOI for this contact.
          // Put the sweeps onto the same time interval.
          var alpha0 = bA.m_sweep.alpha0
          if (bA.m_sweep.alpha0 < bB.m_sweep.alpha0) {
            alpha0 = bB.m_sweep.alpha0
            bA.m_sweep.advance(alpha0)
          } else if (bB.m_sweep.alpha0 < bA.m_sweep.alpha0) {
            alpha0 = bA.m_sweep.alpha0
            bB.m_sweep.advance(alpha0)
          }
          // assert is not supported in KMP.
          // assert(alpha0 < 1.0f)
          val indexA = c.m_indexA
          val indexB = c.m_indexB

          // Compute the time of impact in interval [0, minTOI]
          val input = toiInput
          input.proxyA.set(fA.getShape(), indexA)
          input.proxyB.set(fB.getShape(), indexB)
          input.sweepA.set(bA.m_sweep)
          input.sweepB.set(bB.m_sweep)
          input.tMax = 1.0f
          pool.getTimeOfImpact().timeOfImpact(toiOutput, input)

          // Beta is the fraction of the remaining portion of the .
          val beta = toiOutput.t
          alpha =
            if (toiOutput.state == TimeOfImpact.TOIOutputState.TOUCHING) {
              MathUtils.min(alpha0 + (1.0f - alpha0) * beta, 1.0f)
            } else {
              1.0f
            }
          c.m_toi = alpha
          c.m_flags = c.m_flags or Contact.TOI_FLAG
        }
        if (alpha < minAlpha) {
          // This is the minimum TOI found so far.
          minContact = c
          minAlpha = alpha
        }
        c = c.m_next
      }
      if (minContact == null || 1.0f - 10.0f * Settings.EPSILON < minAlpha) {
        // No more TOI events. Done!
        m_stepComplete = true
        break
      }

      // Advance the bodies to the TOI.
      val fA = minContact.m_fixtureA!!
      val fB = minContact.m_fixtureB!!
      val bA = fA.m_body!!
      val bB = fB.m_body!!
      backup1.set(bA.m_sweep)
      backup2.set(bB.m_sweep)
      bA.advance(minAlpha)
      bB.advance(minAlpha)

      // The TOI contact likely has some new contact points.
      minContact.update(m_contactManager.m_contactListener)
      minContact.m_flags = minContact.m_flags and Contact.TOI_FLAG.inv()
      ++minContact.m_toiCount

      // Is the contact solid?
      if (minContact.isEnabled() == false || minContact.isTouching() == false) {
        // Restore the sweeps.
        minContact.setEnabled(false)
        bA.m_sweep.set(backup1)
        bB.m_sweep.set(backup2)
        bA.synchronizeTransform()
        bB.synchronizeTransform()
        continue
      }
      bA.setAwake(true)
      bB.setAwake(true)

      // Build the island
      island.clear()
      island.add(bA)
      island.add(bB)
      island.add(minContact)
      bA.m_flags = bA.m_flags or Body.Companion.e_islandFlag
      bB.m_flags = bB.m_flags or Body.Companion.e_islandFlag
      minContact.m_flags = minContact.m_flags or Contact.ISLAND_FLAG

      // Get contacts on bodyA and bodyB.
      tempBodies[0] = bA
      tempBodies[1] = bB
      for (i in 0..1) {
        val body = tempBodies[i]!!
        if (body.m_type == BodyType.DYNAMIC) {
          var ce = body.m_contactList
          while (ce != null) {
            if (island.m_bodyCount == island.m_bodyCapacity) {
              break
            }
            if (island.m_contactCount == island.m_contactCapacity) {
              break
            }
            val contact = ce.contact!!

            // Has this contact already been added to the island?
            if (contact.m_flags and Contact.ISLAND_FLAG != 0) {
              ce = ce.next
              continue
            }

            // Only add static, kinematic, or bullet bodies.
            val other = ce.other!!
            if (
              other.m_type == BodyType.DYNAMIC &&
                body.isBullet() == false &&
                other.isBullet() == false
            ) {
              ce = ce.next
              continue
            }

            // Skip sensors.
            val sensorA = contact.m_fixtureA!!.m_isSensor
            val sensorB = contact.m_fixtureB!!.m_isSensor
            if (sensorA || sensorB) {
              ce = ce.next
              continue
            }

            // Tentatively advance the body to the TOI.
            backup1.set(other.m_sweep)
            if (other.m_flags and Body.Companion.e_islandFlag == 0) {
              other.advance(minAlpha)
            }

            // Update the contact points
            contact.update(m_contactManager.m_contactListener)

            // Was the contact disabled by the user?
            if (contact.isEnabled() == false) {
              other.m_sweep.set(backup1)
              other.synchronizeTransform()
              ce = ce.next
              continue
            }

            // Are there contact points?
            if (contact.isTouching() == false) {
              other.m_sweep.set(backup1)
              other.synchronizeTransform()
              ce = ce.next
              continue
            }

            // Add the contact to the island
            contact.m_flags = contact.m_flags or Contact.ISLAND_FLAG
            island.add(contact)

            // Has the other body already been added to the island?
            if (other.m_flags and Body.Companion.e_islandFlag != 0) {
              ce = ce.next
              continue
            }

            // Add the other body to the island.
            other.m_flags = other.m_flags or Body.Companion.e_islandFlag
            if (other.m_type != BodyType.STATIC) {
              other.setAwake(true)
            }
            island.add(other)
            ce = ce.next
          }
        }
      }
      subStep.dt = (1.0f - minAlpha) * step.dt
      subStep.inv_dt = 1.0f / subStep.dt
      subStep.dtRatio = 1.0f
      subStep.positionIterations = 20
      subStep.velocityIterations = step.velocityIterations
      subStep.warmStarting = false
      island.solveTOI(subStep, bA.m_islandIndex, bB.m_islandIndex)

      // Reset island flags and synchronize broad-phase proxies.
      for (i in 0 until island.m_bodyCount) {
        val body = island.m_bodies!![i]!!
        body.m_flags = body.m_flags and Body.Companion.e_islandFlag.inv()
        if (body.m_type != BodyType.DYNAMIC) {
          continue
        }
        body.synchronizeFixtures()

        // Invalidate all contact TOIs on this displaced body.
        var ce = body.m_contactList
        while (ce != null) {
          ce.contact!!.m_flags =
            ce.contact!!.m_flags and (Contact.TOI_FLAG or Contact.ISLAND_FLAG).inv()
          ce = ce.next
        }
      }

      // Commit fixture proxy movements to the broad-phase so that new contacts are created.
      // Also, some contacts can be destroyed.
      m_contactManager.findNewContacts()
      if (m_subStepping) {
        m_stepComplete = false
        break
      }
    }
  }

  private fun drawJoint(joint: Joint) {
    val bodyA = joint.m_bodyA
    val bodyB = joint.m_bodyB
    val xf1 = bodyA!!.m_xf
    val xf2 = bodyB!!.m_xf
    val x1 = xf1.p
    val x2 = xf2.p
    val p1 = pool.popVec2()
    val p2 = pool.popVec2()
    joint.getAnchorA(p1)
    joint.getAnchorB(p2)
    color.set(0.5f, 0.8f, 0.8f)
    when (joint.m_type) {
      JointType.DISTANCE -> m_debugDraw!!.drawSegment(p1, p2, color)
      JointType.PULLEY -> {
        val pulley = joint as PulleyJoint
        val s1 = pulley.m_groundAnchorA
        val s2 = pulley.m_groundAnchorB
        m_debugDraw!!.drawSegment(s1, p1, color)
        m_debugDraw!!.drawSegment(s2, p2, color)
        m_debugDraw!!.drawSegment(s1, s2, color)
      }
      JointType.CONSTANT_VOLUME,
      JointType.MOUSE -> {}
      else -> {
        m_debugDraw!!.drawSegment(x1, p1, color)
        m_debugDraw!!.drawSegment(p1, p2, color)
        m_debugDraw!!.drawSegment(x2, p2, color)
      }
    }
    pool.pushVec2(2)
  }

  private val liquidLength = .12f
  private var averageLinearVel = -1f
  private val liquidOffset = Vec2()
  private val circCenterMoved = Vec2()
  private val liquidColor = Color3f(.4f, .4f, 1f)
  private val center = Vec2()
  private val axis = Vec2()
  private val v1 = Vec2()
  private val v2 = Vec2()
  private val tlvertices = Vec2Array()

  private fun drawShape(fixture: Fixture, xf: Transform, color: Color3f) {
    @Suppress("REDUNDANT_ELSE_IN_WHEN")
    when (fixture.getType()) {
      ShapeType.CIRCLE -> {
        val circle = fixture.m_shape as CircleShape

        // Vec2 center = Mul(xf, circle.m_p);
        Transform.mulToOutUnsafe(xf, circle.m_p, center)
        val radius = circle.m_radius
        xf.q.getXAxis(axis)
        if (fixture.m_userData != null && fixture.m_userData == LIQUID_INT) {
          val b = fixture.m_body
          liquidOffset.set(b!!.m_linearVelocity)
          val linVelLength = b.m_linearVelocity.length()
          averageLinearVel =
            if (averageLinearVel == -1f) {
              linVelLength
            } else {
              .98f * averageLinearVel + .02f * linVelLength
            }
          liquidOffset.mulLocal(liquidLength / averageLinearVel / 2)
          circCenterMoved.set(center).addLocal(liquidOffset)
          center.subLocal(liquidOffset)
          m_debugDraw!!.drawSegment(center, circCenterMoved, liquidColor)
          return
        }
        m_debugDraw!!.drawSolidCircle(center, radius, axis, color)
      }
      ShapeType.POLYGON -> {
        val poly = fixture.m_shape as PolygonShape
        val vertexCount = poly.m_count
        // assert is not supported in KMP.
        // assert(vertexCount <= Settings.maxPolygonVertices)
        val vertices: Array<Vec2> = tlvertices[Settings.maxPolygonVertices]
        for (i: Int in 0 until vertexCount) {

          // vertices[i] = Mul(xf, poly.m_vertices[i]);
          Transform.mulToOutUnsafe(xf, poly.m_vertices[i], vertices[i])
        }
        m_debugDraw!!.drawSolidPolygon(vertices, vertexCount, color)
      }
      ShapeType.EDGE -> {
        val edge = fixture.m_shape as EdgeShape
        Transform.mulToOutUnsafe(xf, edge.m_vertex1, v1)
        Transform.mulToOutUnsafe(xf, edge.m_vertex2, v2)
        m_debugDraw!!.drawSegment(v1, v2, color)
      }
      ShapeType.CHAIN -> {
        val chain = fixture.m_shape as ChainShape
        val count = chain.m_count
        val vertices = chain.m_vertices
        Transform.mulToOutUnsafe(xf, vertices!![0], v1)
        for (i: Int in 1 until count) {
          Transform.mulToOutUnsafe(xf, vertices[i], v2)
          m_debugDraw!!.drawSegment(v1, v2, color)
          m_debugDraw!!.drawCircle(v1, 0.05f, color)
          v1.set(v2)
        }
      }
      else -> {}
    }
  }

  companion object {
    const val WORLD_POOL_SIZE = 100
    const val WORLD_POOL_CONTAINER_SIZE = 10
    const val NEW_FIXTURE = 0x0001
    const val LOCKED = 0x0002
    const val CLEAR_FORCES = 0x0004

    // NOTE this corresponds to the liquid test, so the debugdraw can draw
    // the liquid particles correctly. They should be the same.
    private const val LIQUID_INT = 1234598372
  }
}

internal class WorldQueryWrapper : TreeCallback {
  override fun treeCallback(proxyId: Int): Boolean {
    val proxy = broadPhase!!.getUserData(proxyId) as FixtureProxy?
    return callback!!.reportFixture(proxy!!.fixture!!)
  }

  var broadPhase: BroadPhase? = null
  var callback: QueryCallback? = null
}

internal class WorldRayCastWrapper : TreeRayCastCallback {
  // djm pooling
  private val output = RayCastOutput()
  private val temp = Vec2()
  private val point = Vec2()
  override fun raycastCallback(input: RayCastInput, nodeId: Int): Float {
    val userData = broadPhase!!.getUserData(nodeId)
    val proxy = userData as FixtureProxy?
    val fixture = proxy!!.fixture
    val index = proxy.childIndex
    val hit = fixture!!.raycast(output, input, index)
    if (hit) {
      val fraction = output.fraction
      // Vec2 point = (1.0f - fraction) * input.p1 + fraction * input.p2;
      temp.set(input.p2).mulLocal(fraction)
      point.set(input.p1).mulLocal(1 - fraction).addLocal(temp)
      return callback!!.reportFixture(fixture, point, output.normal, fraction)
    }
    return input.maxFraction
  }

  var broadPhase: BroadPhase? = null
  var callback: RayCastCallback? = null
}
