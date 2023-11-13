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
  var flags: Int = CLEAR_FORCES

  var contactManager: ContactManager = ContactManager(this, broadPhaseStrategy)
  var bodyList: Body? = null
    private set

  var jointList: Joint? = null
    private set

  var bodyCount = 0
    private set

  var jointCount = 0
    private set

  var gravity = Vec2().apply { set(gravity) }
    set(value) {
      field.set(value)
    }

  var allowSleep = true
    set(value) {
      if (value == field) {
        return
      }
      field = value
      if (!field) {
        var b = bodyList
        while (b != null) {
          b.setAwake(true)
          b = b.next
        }
      }
    }

  // private Body m_groundBody;
  var destructionListener: DestructionListener? = null
  var debugDraw: DebugDraw? = null

  // these are for debugging the solver
  var warmStarting = true
  var continuousPhysics = true
  var subStepping = false
  val profile: Profile = Profile()
  val contactList: Contact
    get() = contactManager.contactList!!

  val proxyCount: Int
    get() = contactManager.broadPhase.proxyCount

  val contactCount: Int
    get() = contactManager.contactCount

  val treeHeight: Int
    get() = contactManager.broadPhase.getTreeHeight()

  val treeBalance: Int
    get() = contactManager.broadPhase.getTreeBalance()

  val treeQuality: Float
    get() = contactManager.broadPhase.getTreeQuality()

  val isLocked: Boolean
    get() = (flags and LOCKED) == LOCKED

  private var stepComplete = true
  private var invDt0: Float = 0f
  private val contactStacks =
    Array(ShapeType.values().size) { arrayOfNulls<ContactRegister>(ShapeType.values().size) }

  // djm pooling
  private val step = TimeStep()
  private val stepTimer = Timer()
  private val tempTimer = Timer()

  private val color = Color3f()
  private val xf = Transform()
  private val cA = Vec2()
  private val cB = Vec2()
  private val avs = Vec2Array()

  private val wqwrapper = WorldQueryWrapper()
  private val wrcwrapper = WorldRayCastWrapper()
  private val input = RayCastInput()

  private val island = Island()
  private var stack = arrayOfNulls<Body>(10) // TODO djm find a good initial stack number;
  private val islandProfile = Profile()
  private val broadphaseTimer = Timer()

  private val toiIsland = Island()
  private val toiInput = TimeOfImpact.TOIInput()
  private val toiOutput = TimeOfImpact.TOIOutput()
  private val subStep = TimeStep()
  private val tempBodies = arrayOfNulls<Body>(2)
  private val backup1 = Sweep()
  private val backup2 = Sweep()

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
    initializeRegisters()
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
    addType(pool.circleContactStack, ShapeType.CIRCLE, ShapeType.CIRCLE)
    addType(pool.polyCircleContactStack, ShapeType.POLYGON, ShapeType.CIRCLE)
    addType(pool.polyContactStack, ShapeType.POLYGON, ShapeType.POLYGON)
    addType(pool.edgeCircleContactStack, ShapeType.EDGE, ShapeType.CIRCLE)
    addType(pool.edgePolyContactStack, ShapeType.EDGE, ShapeType.POLYGON)
    addType(pool.chainCircleContactStack, ShapeType.CHAIN, ShapeType.CIRCLE)
    addType(pool.chainPolyContactStack, ShapeType.CHAIN, ShapeType.POLYGON)
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
    val fixtureA = contact.fixtureA
    val fixtureB = contact.fixtureB
    if (contact.manifold.pointCount > 0 && !fixtureA.isSensor && !fixtureB.isSensor) {
      fixtureA.body!!.setAwake(true)
      fixtureB.body!!.setAwake(true)
    }
    val type1 = fixtureA.getType()
    val type2 = fixtureB.getType()
    val creator = contactStacks[type1.ordinal][type2.ordinal]!!.creator!!
    creator.push(contact)
  }

  /**
   * Register a contact filter to provide specific control over collision. Otherwise the default
   * filter is used (_defaultFilter). The listener is owned by you and must remain in scope.
   *
   * @param filter
   */
  fun setContactFilter(filter: ContactFilter) {
    contactManager.contactFilter = filter
  }

  /**
   * Register a contact event listener. The listener is owned by you and must remain in scope.
   *
   * @param listener
   */
  fun setContactListener(listener: ContactListener) {
    contactManager.contactListener = listener
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
    if (isLocked) {
      return null
    }
    // TODO djm pooling
    val b = Body(def, this)

    // add to world doubly linked list
    b.prev = null
    b.next = bodyList
    if (bodyList != null) {
      bodyList!!.prev = b
    }
    bodyList = b
    ++bodyCount
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
    if (isLocked) {
      return
    }

    // Delete the attached joints.
    var je = body.jointList
    while (je != null) {
      val je0 = je
      je = je.next
      if (destructionListener != null) {
        destructionListener!!.sayGoodbye(je0.joint!!)
      }
      destroyJoint(je0.joint!!)
      body.jointList = je
    }
    body.jointList = null

    // Delete the attached contacts.
    var ce = body.contactList
    while (ce != null) {
      val ce0 = ce
      ce = ce.next
      contactManager.destroy(ce0.contact!!)
    }
    body.contactList = null
    var f = body.fixtureList
    while (f != null) {
      val f0 = f
      f = f.next
      if (destructionListener != null) {
        destructionListener!!.sayGoodbye(f0)
      }
      f0.destroyProxies(contactManager.broadPhase)
      f0.destroy()
      // TODO djm recycle fixtures (here or in that destroy method)
      body.fixtureList = f
      body.fixtureCount -= 1
    }
    body.fixtureList = null
    body.fixtureCount = 0

    // Remove world body list.
    if (body.prev != null) {
      body.prev!!.next = body.next
    }
    if (body.next != null) {
      body.next!!.prev = body.prev
    }
    if (body === bodyList) {
      bodyList = body.next
    }
    --bodyCount
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
    if (isLocked) {
      return null
    }
    val j = Joint.create(this, def)

    // Connect to the world list.
    j!!.prev = null
    j.next = jointList
    if (jointList != null) {
      jointList!!.prev = j
    }
    jointList = j
    ++jointCount

    // Connect to the bodies' doubly linked lists.
    j.edgeA.joint = j
    j.edgeA.other = j.bodyB
    j.edgeA.prev = null
    j.edgeA.next = j.bodyA.jointList
    if (j.bodyA.jointList != null) {
      j.bodyA.jointList!!.prev = j.edgeA
    }
    j.bodyA.jointList = j.edgeA
    j.edgeB.joint = j
    j.edgeB.other = j.bodyA
    j.edgeB.prev = null
    j.edgeB.next = j.bodyB.jointList
    if (j.bodyB.jointList != null) {
      j.bodyB.jointList!!.prev = j.edgeB
    }
    j.bodyB.jointList = j.edgeB
    val bodyA = def.bodyA
    val bodyB = def.bodyB

    // If the joint prevents collisions, then flag any contacts for filtering.
    if (!def.collideConnected) {
      var edge = bodyB.contactList
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
    if (isLocked) {
      return
    }
    val collideConnected = j.collideConnected

    // Remove from the doubly linked list.
    if (j.prev != null) {
      j.prev!!.next = j.next
    }
    if (j.next != null) {
      j.next!!.prev = j.prev
    }
    if (j === jointList) {
      jointList = j.next
    }

    // Disconnect from island graph.
    val bodyA = j.bodyA
    val bodyB = j.bodyB

    // Wake up connected bodies.
    bodyA.setAwake(true)
    bodyB.setAwake(true)

    // Remove from body 1.
    if (j.edgeA.prev != null) {
      j.edgeA.prev!!.next = j.edgeA.next
    }
    if (j.edgeA.next != null) {
      j.edgeA.next!!.prev = j.edgeA.prev
    }
    if (j.edgeA === bodyA.jointList) {
      bodyA.jointList = j.edgeA.next
    }
    j.edgeA.prev = null
    j.edgeA.next = null

    // Remove from body 2
    if (j.edgeB.prev != null) {
      j.edgeB.prev!!.next = j.edgeB.next
    }
    if (j.edgeB.next != null) {
      j.edgeB.next!!.prev = j.edgeB.prev
    }
    if (j.edgeB === bodyB.jointList) {
      bodyB.jointList = j.edgeB.next
    }
    j.edgeB.prev = null
    j.edgeB.next = null
    Joint.destroy(j)
    // assert is not supported in KMP.
    // assert(jointCount > 0)
    --jointCount

    // If the joint prevents collisions, then flag any contacts for filtering.
    if (!collideConnected) {
      var edge = bodyB.contactList
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
    if (flags and NEW_FIXTURE == NEW_FIXTURE) {
      // log.debug("There's a new fixture, lets look for new contacts");
      contactManager.findNewContacts()
      flags = flags and NEW_FIXTURE.inv()
    }
    flags = flags or LOCKED
    step.dt = dt
    step.velocityIterations = velocityIterations
    step.positionIterations = positionIterations
    if (dt > 0.0f) {
      step.inv_dt = 1.0f / dt
    } else {
      step.inv_dt = 0.0f
    }
    step.dtRatio = invDt0 * dt
    step.warmStarting = warmStarting

    // Update contacts. This is where some contacts are destroyed.
    tempTimer.reset()
    contactManager.collide()
    profile.collide = tempTimer.milliseconds

    // Integrate velocities, solve velocity constraints, and integrate positions.
    if (stepComplete && step.dt > 0.0f) {
      tempTimer.reset()
      solve(step)
      profile.solve = tempTimer.milliseconds
    }

    // Handle TOI events.
    if (continuousPhysics && step.dt > 0.0f) {
      tempTimer.reset()
      solveTOI(step)
      profile.solveTOI = tempTimer.milliseconds
    }
    if (step.dt > 0.0f) {
      invDt0 = step.inv_dt
    }
    if (flags and CLEAR_FORCES == CLEAR_FORCES) {
      clearForces()
    }
    flags = flags and LOCKED.inv()
    // log.debug("ending step");
    profile.step = stepTimer.milliseconds
  }

  /**
   * Call this after you are done with time steps to clear the forces. You normally call this after
   * each call to Step, unless you are performing sub-steps. By default, forces will be
   * automatically cleared, so you don't need to call this function.
   *
   * @see setAutoClearForces
   */
  fun clearForces() {
    var body = bodyList
    while (body != null) {
      body.force.setZero()
      body.torque = 0.0f
      body = body.next
    }
  }

  /** Call this to draw shapes and other debug draw data. */
  fun drawDebugData() {
    if (debugDraw == null) {
      return
    }
    val flags: Int = debugDraw!!.flags
    if (flags and DebugDraw.E_SHAPE_BIT == DebugDraw.E_SHAPE_BIT) {
      var b = bodyList
      while (b != null) {
        xf.set(b.xf)
        var f = b.fixtureList
        while (f != null) {
          if (!b.isActive) {
            color.set(0.5f, 0.5f, 0.3f)
            drawShape(f, xf, color)
          } else if (b.type == BodyType.STATIC) {
            color.set(0.5f, 0.9f, 0.3f)
            drawShape(f, xf, color)
          } else if (b.type == BodyType.KINEMATIC) {
            color.set(0.5f, 0.5f, 0.9f)
            drawShape(f, xf, color)
          } else if (!b.isAwake) {
            color.set(0.5f, 0.5f, 0.5f)
            drawShape(f, xf, color)
          } else {
            color.set(0.9f, 0.7f, 0.7f)
            drawShape(f, xf, color)
          }
          f = f.next
        }
        b = b.next
      }
    }
    if (flags and DebugDraw.E_JOINT_BIT == DebugDraw.E_JOINT_BIT) {
      var j = jointList
      while (j != null) {
        drawJoint(j)
        j = j.next
      }
    }
    if (flags and DebugDraw.E_PAIR_BIT == DebugDraw.E_PAIR_BIT) {
      color.set(0.3f, 0.9f, 0.9f)
      var c = contactManager.contactList
      while (c != null) {
        val fixtureA = c.fixtureA
        val fixtureB = c.fixtureB
        fixtureA.getAABB(c.indexA)!!.getCenterToOut(cA)
        fixtureB.getAABB(c.indexB)!!.getCenterToOut(cB)
        debugDraw!!.drawSegment(cA, cB, color)
        c = c.next
      }
    }
    if (flags and DebugDraw.E_AABB_BIT == DebugDraw.E_AABB_BIT) {
      color.set(0.9f, 0.3f, 0.9f)
      var b = bodyList
      while (b != null) {
        if (!b.isActive) {
          b = b.next
          continue
        }
        var f = b.fixtureList
        while (f != null) {
          for (i in 0 until f.proxyCount) {
            val proxy = f.proxies!![i]
            val aabb = contactManager.broadPhase.getFatAABB(proxy.proxyId)
            val vs: Array<Vec2> = avs[4]
            vs[0].set(aabb.lowerBound.x, aabb.lowerBound.y)
            vs[1].set(aabb.upperBound.x, aabb.lowerBound.y)
            vs[2].set(aabb.upperBound.x, aabb.upperBound.y)
            vs[3].set(aabb.lowerBound.x, aabb.upperBound.y)
            debugDraw!!.drawPolygon(vs, 4, color)
          }
          f = f.next
        }
        b = b.next
      }
    }
    if (flags and DebugDraw.E_CENTER_OF_MASS_BIT == DebugDraw.E_CENTER_OF_MASS_BIT) {
      var b = bodyList
      while (b != null) {
        xf.set(b.xf)
        xf.p.set(b.sweep.c)
        debugDraw!!.drawTransform(xf)
        b = b.next
      }
    }
    if (flags and DebugDraw.E_DYNAMIC_TREE_BIT == DebugDraw.E_DYNAMIC_TREE_BIT) {
      contactManager.broadPhase.drawTree(debugDraw!!)
    }
  }

  /**
   * Query the world for all fixtures that potentially overlap the provided AABB.
   *
   * @param callback a user implemented callback class.
   * @param aabb the query box.
   */
  fun queryAABB(callback: QueryCallback, aabb: AABB) {
    wqwrapper.broadPhase = contactManager.broadPhase
    wqwrapper.callback = callback
    contactManager.broadPhase.query(wqwrapper, aabb)
  }

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
    wrcwrapper.broadPhase = contactManager.broadPhase
    wrcwrapper.callback = callback
    input.maxFraction = 1.0f
    input.p1.set(point1)
    input.p2.set(point2)
    contactManager.broadPhase.raycast(wrcwrapper, input)
  }

  /**
   * Set flag to control automatic clearing of forces after each time step.
   *
   * @param flag
   */
  fun setAutoClearForces(flag: Boolean) {
    flags =
      if (flag) {
        flags or CLEAR_FORCES
      } else {
        flags and CLEAR_FORCES.inv()
      }
  }

  /**
   * Get the flag that controls automatic clearing of forces after each time step.
   *
   * @return
   */
  fun getAutoClearForces(): Boolean = flags and CLEAR_FORCES == CLEAR_FORCES

  private fun solve(step: TimeStep) {
    profile.solveInit = 0f
    profile.solveVelocity = 0f
    profile.solvePosition = 0f

    // Size the island for the worst case.
    island.initialize(
      bodyCount,
      contactManager.contactCount,
      jointCount,
      contactManager.contactListener
    )

    // Clear all the island flags.
    var body: Body? = bodyList
    while (body != null) {
      body.flags = body.flags and Body.Companion.IS_LAND_FLAG.inv()
      body = body.next
    }

    var c = contactManager.contactList
    while (c != null) {
      c.flags = c.flags and Contact.ISLAND_FLAG.inv()
      c = c.next
    }
    var j = jointList
    while (j != null) {
      j.islandFlag = false
      j = j.next
    }

    // Build and simulate all awake islands.
    val stackSize = bodyCount
    if (stack.size < stackSize) {
      stack = arrayOfNulls(stackSize)
    }
    var seed = bodyList
    while (seed != null) {
      if (seed.flags and Body.Companion.IS_LAND_FLAG == Body.Companion.IS_LAND_FLAG) {
        seed = seed.next
        continue
      }
      if (!seed.isAwake || !seed.isActive) {
        seed = seed.next
        continue
      }

      // The seed can be dynamic or kinematic.
      if (seed.type == BodyType.STATIC) {
        seed = seed.next
        continue
      }

      // Reset island and stack.
      island.clear()
      var stackCount = 0
      stack[stackCount++] = seed
      seed.flags = seed.flags or Body.Companion.IS_LAND_FLAG

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
        if (b.type == BodyType.STATIC) {
          continue
        }

        // Search all contacts connected to this body.
        var ce = b.contactList
        while (ce != null) {
          val contact = ce.contact!!

          // Has this contact already been added to an island?
          if (contact.flags and Contact.ISLAND_FLAG == Contact.ISLAND_FLAG) {
            ce = ce.next
            continue
          }

          // Is this contact solid and touching?
          if (!contact.isEnabled || !contact.isTouching) {
            ce = ce.next
            continue
          }

          // Skip sensors.
          val sensorA = contact.fixtureA.isSensor
          val sensorB = contact.fixtureB.isSensor
          if (sensorA || sensorB) {
            ce = ce.next
            continue
          }
          island.add(contact)
          contact.flags = contact.flags or Contact.ISLAND_FLAG
          val other = ce.other

          // Was the other body already added to this island?
          if (other!!.flags and Body.Companion.IS_LAND_FLAG == Body.Companion.IS_LAND_FLAG) {
            ce = ce.next
            continue
          }
          // assert is not supported in KMP.
          // assert(stackCount < stackSize)
          stack[stackCount++] = other
          other.flags = other.flags or Body.Companion.IS_LAND_FLAG
          ce = ce.next
        }

        // Search all joints connect to this body.
        var je = b.jointList
        while (je != null) {
          if (je.joint!!.islandFlag) {
            je = je.next
            continue
          }
          val other = je.other

          // Don't simulate joints connected to inactive bodies.
          if (!other!!.isActive) {
            je = je.next
            continue
          }
          island.add(je.joint!!)
          je.joint!!.islandFlag = true
          if (other.flags and Body.Companion.IS_LAND_FLAG == Body.Companion.IS_LAND_FLAG) {
            je = je.next
            continue
          }
          // assert is not supported in KMP.
          // assert(stackCount < stackSize)
          stack[stackCount++] = other
          other.flags = other.flags or Body.Companion.IS_LAND_FLAG
          je = je.next
        }
      }
      island.solve(islandProfile, step, gravity, allowSleep)
      profile.solveInit += islandProfile.solveInit
      profile.solveVelocity += islandProfile.solveVelocity
      profile.solvePosition += islandProfile.solvePosition

      // Post solve cleanup.
      for (i in 0 until island.bodyCount) {
        // Allow static bodies to participate in other islands.
        val b = island.bodies[i]!!
        if (b.type == BodyType.STATIC) {
          b.flags = b.flags and Body.Companion.IS_LAND_FLAG.inv()
        }
      }
      seed = seed.next
    }
    broadphaseTimer.reset()
    // Synchronize fixtures, check for out of range bodies.
    var b = bodyList
    while (b != null) {

      // If a body was not in an island then it did not move.
      if (b.flags and Body.Companion.IS_LAND_FLAG == 0) {
        b = b.next
        continue
      }
      if (b.type == BodyType.STATIC) {
        b = b.next
        continue
      }

      // Update fixtures (for broad-phase).
      b.synchronizeFixtures()
      b = b.next
    }

    // Look for new contacts.
    contactManager.findNewContacts()
    profile.broadphase = broadphaseTimer.milliseconds
  }

  private fun solveTOI(step: TimeStep) {
    val island = toiIsland
    island.initialize(
      2 * Settings.MAX_TOI_CONTACTS,
      Settings.MAX_TOI_CONTACTS,
      0,
      contactManager.contactListener
    )
    if (stepComplete) {
      var b = bodyList
      while (b != null) {
        b.flags = b.flags and Body.Companion.IS_LAND_FLAG.inv()
        b.sweep.alpha0 = 0.0f
        b = b.next
      }
      var c = contactManager.contactList
      while (c != null) {

        // Invalidate TOI
        c.flags = c.flags and (Contact.TOI_FLAG or Contact.ISLAND_FLAG).inv()
        c.toiCount = 0f
        c.toi = 1.0f
        c = c.next
      }
    }

    // Find TOI events and solve them.
    while (true) {

      // Find the first TOI.
      var minContact: Contact? = null
      var minAlpha = 1.0f
      var c = contactManager.contactList
      while (c != null) {

        // Is this contact disabled?
        if (!c.isEnabled) {
          c = c.next
          continue
        }

        // Prevent excessive sub-stepping.
        if (c.toiCount > Settings.MAX_SUB_STEPS) {
          c = c.next
          continue
        }
        @Suppress("VARIABLE_WITH_REDUNDANT_INITIALIZER") var alpha = 1.0f
        if (c.flags and Contact.TOI_FLAG != 0) {
          // This contact has a valid cached TOI.
          alpha = c.toi
        } else {
          val fA = c.fixtureA
          val fB = c.fixtureB

          // Is there a sensor?
          if (fA.isSensor || fB.isSensor) {
            c = c.next
            continue
          }
          val bA = fA.body!!
          val bB = fB.body!!
          val typeA = bA.type
          val typeB = bB.type
          // assert is not supported in KMP.
          // assert(typeA == BodyType.DYNAMIC || typeB == BodyType.DYNAMIC)
          val activeA = bA.isAwake && typeA != BodyType.STATIC
          val activeB = bB.isAwake && typeB != BodyType.STATIC

          // Is at least one body active (awake and dynamic or kinematic)?
          if (!activeA && !activeB) {
            c = c.next
            continue
          }
          val collideA = bA.isBullet || typeA != BodyType.DYNAMIC
          val collideB = bB.isBullet || typeB != BodyType.DYNAMIC

          // Are these two non-bullet dynamic bodies?
          if (!collideA && !collideB) {
            c = c.next
            continue
          }

          // Compute the TOI for this contact.
          // Put the sweeps onto the same time interval.
          var alpha0 = bA.sweep.alpha0
          if (bA.sweep.alpha0 < bB.sweep.alpha0) {
            alpha0 = bB.sweep.alpha0
            bA.sweep.advance(alpha0)
          } else if (bB.sweep.alpha0 < bA.sweep.alpha0) {
            alpha0 = bA.sweep.alpha0
            bB.sweep.advance(alpha0)
          }
          // assert is not supported in KMP.
          // assert(alpha0 < 1.0f)
          val indexA = c.indexA
          val indexB = c.indexB

          // Compute the time of impact in interval [0, minTOI]
          val input = toiInput
          input.proxyA.set(fA.shape!!, indexA)
          input.proxyB.set(fB.shape!!, indexB)
          input.sweepA.set(bA.sweep)
          input.sweepB.set(bB.sweep)
          input.tMax = 1.0f
          pool.timeOfImpact.timeOfImpact(toiOutput, input)

          // Beta is the fraction of the remaining portion of the .
          val beta = toiOutput.t
          alpha =
            if (toiOutput.state == TimeOfImpact.TOIOutputState.TOUCHING) {
              MathUtils.min(alpha0 + (1.0f - alpha0) * beta, 1.0f)
            } else {
              1.0f
            }
          c.toi = alpha
          c.flags = c.flags or Contact.TOI_FLAG
        }
        if (alpha < minAlpha) {
          // This is the minimum TOI found so far.
          minContact = c
          minAlpha = alpha
        }
        c = c.next
      }
      if (minContact == null || 1.0f - 10.0f * Settings.EPSILON < minAlpha) {
        // No more TOI events. Done!
        stepComplete = true
        break
      }

      // Advance the bodies to the TOI.
      val fA = minContact.fixtureA
      val fB = minContact.fixtureB
      val bA = fA.body!!
      val bB = fB.body!!
      backup1.set(bA.sweep)
      backup2.set(bB.sweep)
      bA.advance(minAlpha)
      bB.advance(minAlpha)

      // The TOI contact likely has some new contact points.
      minContact.update(contactManager.contactListener)
      minContact.flags = minContact.flags and Contact.TOI_FLAG.inv()
      ++minContact.toiCount

      // Is the contact solid?
      if (!minContact.isEnabled || !minContact.isTouching) {
        // Restore the sweeps.
        minContact.setEnabled(false)
        bA.sweep.set(backup1)
        bB.sweep.set(backup2)
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
      bA.flags = bA.flags or Body.Companion.IS_LAND_FLAG
      bB.flags = bB.flags or Body.Companion.IS_LAND_FLAG
      minContact.flags = minContact.flags or Contact.ISLAND_FLAG

      // Get contacts on bodyA and bodyB.
      tempBodies[0] = bA
      tempBodies[1] = bB
      for (i in 0..1) {
        val body = tempBodies[i]!!
        if (body.type == BodyType.DYNAMIC) {
          var ce = body.contactList
          while (ce != null) {
            if (island.bodyCount == island.bodyCapacity) {
              break
            }
            if (island.contactCount == island.contactCapacity) {
              break
            }
            val contact = ce.contact!!

            // Has this contact already been added to the island?
            if (contact.flags and Contact.ISLAND_FLAG != 0) {
              ce = ce.next
              continue
            }

            // Only add static, kinematic, or bullet bodies.
            val other = ce.other!!
            if (other.type == BodyType.DYNAMIC && !body.isBullet && !other.isBullet) {
              ce = ce.next
              continue
            }

            // Skip sensors.
            val sensorA = contact.fixtureA.isSensor
            val sensorB = contact.fixtureB.isSensor
            if (sensorA || sensorB) {
              ce = ce.next
              continue
            }

            // Tentatively advance the body to the TOI.
            backup1.set(other.sweep)
            if (other.flags and Body.Companion.IS_LAND_FLAG == 0) {
              other.advance(minAlpha)
            }

            // Update the contact points
            contact.update(contactManager.contactListener)

            // Was the contact disabled by the user?
            if (!contact.isEnabled) {
              other.sweep.set(backup1)
              other.synchronizeTransform()
              ce = ce.next
              continue
            }

            // Are there contact points?
            if (!contact.isTouching) {
              other.sweep.set(backup1)
              other.synchronizeTransform()
              ce = ce.next
              continue
            }

            // Add the contact to the island
            contact.flags = contact.flags or Contact.ISLAND_FLAG
            island.add(contact)

            // Has the other body already been added to the island?
            if (other.flags and Body.Companion.IS_LAND_FLAG != 0) {
              ce = ce.next
              continue
            }

            // Add the other body to the island.
            other.flags = other.flags or Body.Companion.IS_LAND_FLAG
            if (other.type != BodyType.STATIC) {
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
      island.solveTOI(subStep, bA.islandIndex, bB.islandIndex)

      // Reset island flags and synchronize broad-phase proxies.
      for (i in 0 until island.bodyCount) {
        val body = island.bodies[i]!!
        body.flags = body.flags and Body.Companion.IS_LAND_FLAG.inv()
        if (body.type != BodyType.DYNAMIC) {
          continue
        }
        body.synchronizeFixtures()

        // Invalidate all contact TOIs on this displaced body.
        var ce = body.contactList
        while (ce != null) {
          ce.contact!!.flags =
            ce.contact!!.flags and (Contact.TOI_FLAG or Contact.ISLAND_FLAG).inv()
          ce = ce.next
        }
      }

      // Commit fixture proxy movements to the broad-phase so that new contacts are created.
      // Also, some contacts can be destroyed.
      contactManager.findNewContacts()
      if (subStepping) {
        stepComplete = false
        break
      }
    }
  }

  private fun drawJoint(joint: Joint) {
    val bodyA = joint.bodyA
    val bodyB = joint.bodyB
    val xf1 = bodyA.xf
    val xf2 = bodyB.xf
    val x1 = xf1.p
    val x2 = xf2.p
    val p1 = pool.popVec2()
    val p2 = pool.popVec2()
    joint.getAnchorA(p1)
    joint.getAnchorB(p2)
    color.set(0.5f, 0.8f, 0.8f)
    val nonNullDebugDrew = debugDraw!!
    when (joint.type) {
      JointType.DISTANCE -> nonNullDebugDrew.drawSegment(p1, p2, color)
      JointType.PULLEY -> {
        val pulley = joint as PulleyJoint
        val s1 = pulley.groundAnchorA
        val s2 = pulley.groundAnchorB
        nonNullDebugDrew.drawSegment(s1, p1, color)
        nonNullDebugDrew.drawSegment(s2, p2, color)
        nonNullDebugDrew.drawSegment(s1, s2, color)
      }
      JointType.CONSTANT_VOLUME,
      JointType.MOUSE -> {}
      else -> {
        nonNullDebugDrew.drawSegment(x1, p1, color)
        nonNullDebugDrew.drawSegment(p1, p2, color)
        nonNullDebugDrew.drawSegment(x2, p2, color)
      }
    }
    pool.pushVec2(2)
  }

  private fun drawShape(fixture: Fixture, xf: Transform, color: Color3f) {
    val nonNullDebugDraw = debugDraw!!
    when (fixture.getType()) {
      ShapeType.CIRCLE -> {
        val circle = fixture.shape as CircleShape

        // Vec2 center = Mul(xf, circle.m_p);
        Transform.mulToOutUnsafe(xf, circle.p, center)
        val radius = circle.radius
        xf.q.getXAxis(axis)
        if (fixture.userData != null && fixture.userData == LIQUID_INT) {
          val b = fixture.body
          liquidOffset.set(b!!.linearVelocity)
          val linVelLength = b.linearVelocity.length()
          averageLinearVel =
            if (averageLinearVel == -1f) {
              linVelLength
            } else {
              .98f * averageLinearVel + .02f * linVelLength
            }
          liquidOffset.mulLocal(liquidLength / averageLinearVel / 2)
          circCenterMoved.set(center).addLocal(liquidOffset)
          center.subLocal(liquidOffset)
          nonNullDebugDraw.drawSegment(center, circCenterMoved, liquidColor)
          return
        }
        debugDraw!!.drawSolidCircle(center, radius, axis, color)
      }
      ShapeType.POLYGON -> {
        val poly = fixture.shape as PolygonShape
        val vertexCount = poly.count
        // assert is not supported in KMP.
        // assert(vertexCount <= Settings.maxPolygonVertices)
        val vertices: Array<Vec2> = tlvertices[Settings.MAX_POLYGON_VERTICES]
        for (i: Int in 0 until vertexCount) {

          // vertices[i] = Mul(xf, poly.m_vertices[i]);
          Transform.mulToOutUnsafe(xf, poly.vertices[i], vertices[i])
        }
        nonNullDebugDraw.drawSolidPolygon(vertices, vertexCount, color)
      }
      ShapeType.EDGE -> {
        val edge = fixture.shape as EdgeShape
        Transform.mulToOutUnsafe(xf, edge.vertex1, v1)
        Transform.mulToOutUnsafe(xf, edge.vertex2, v2)
        nonNullDebugDraw.drawSegment(v1, v2, color)
      }
      ShapeType.CHAIN -> {
        val chain = fixture.shape as ChainShape
        val count = chain.count
        val vertices = chain.vertices
        Transform.mulToOutUnsafe(xf, vertices!![0], v1)
        for (i: Int in 1 until count) {
          Transform.mulToOutUnsafe(xf, vertices[i], v2)
          nonNullDebugDraw.drawSegment(v1, v2, color)
          nonNullDebugDraw.drawCircle(v1, 0.05f, color)
          v1.set(v2)
        }
      }
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
  var broadPhase: BroadPhase? = null
  var callback: QueryCallback? = null

  override fun treeCallback(proxyId: Int): Boolean {
    val proxy = broadPhase!!.getUserData(proxyId) as FixtureProxy?
    return callback!!.reportFixture(proxy!!.fixture!!)
  }
}

internal class WorldRayCastWrapper : TreeRayCastCallback {
  var broadPhase: BroadPhase? = null
  var callback: RayCastCallback? = null

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
}
