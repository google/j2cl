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
/** Created at 3:26:14 AM Jan 11, 2011 */
package org.jbox2d.pooling.normal

import org.jbox2d.collision.AABB
import org.jbox2d.collision.Collision
import org.jbox2d.collision.Distance
import org.jbox2d.collision.TimeOfImpact
import org.jbox2d.common.Mat22
import org.jbox2d.common.Mat33
import org.jbox2d.common.Rot
import org.jbox2d.common.Settings
import org.jbox2d.common.Vec2
import org.jbox2d.common.Vec3
import org.jbox2d.dynamics.contacts.ChainAndCircleContact
import org.jbox2d.dynamics.contacts.ChainAndPolygonContact
import org.jbox2d.dynamics.contacts.CircleContact
import org.jbox2d.dynamics.contacts.Contact
import org.jbox2d.dynamics.contacts.EdgeAndCircleContact
import org.jbox2d.dynamics.contacts.EdgeAndPolygonContact
import org.jbox2d.dynamics.contacts.PolygonAndCircleContact
import org.jbox2d.dynamics.contacts.PolygonContact
import org.jbox2d.pooling.IDynamicStack
import org.jbox2d.pooling.IWorldPool

/**
 * Provides object pooling for all objects used in the engine. Objects retrieved from here should
 * only be used temporarily, and then pushed back (with the exception of arrays).
 *
 * @author Daniel Murphy
 */
class DefaultWorldPool(private val argSize: Int, private val argContainerSize: Int) : IWorldPool {
  // This has to be first, since it is used in other properties' initialization.
  private val world: IWorldPool = this

  override val polyContactStack: IDynamicStack<Contact> =
    object : MutableStack<Contact>(Settings.CONTACT_STACK_INIT_SIZE) {
      override fun newInstance(): Contact {
        return PolygonContact(world)
      }
    }
  override val circleContactStack: IDynamicStack<Contact> =
    object : MutableStack<Contact>(Settings.CONTACT_STACK_INIT_SIZE) {
      override fun newInstance(): Contact {
        return CircleContact(world)
      }
    }
  override val polyCircleContactStack: IDynamicStack<Contact> =
    object : MutableStack<Contact>(Settings.CONTACT_STACK_INIT_SIZE) {
      override fun newInstance(): Contact {
        return PolygonAndCircleContact(world)
      }
    }
  override val edgeCircleContactStack: IDynamicStack<Contact> =
    object : MutableStack<Contact>(Settings.CONTACT_STACK_INIT_SIZE) {
      override fun newInstance(): Contact {
        return EdgeAndCircleContact(world)
      }
    }
  override val edgePolyContactStack: IDynamicStack<Contact> =
    object : MutableStack<Contact>(Settings.CONTACT_STACK_INIT_SIZE) {
      override fun newInstance(): Contact {
        return EdgeAndPolygonContact(world)
      }
    }
  override val chainCircleContactStack: IDynamicStack<Contact> =
    object : MutableStack<Contact>(Settings.CONTACT_STACK_INIT_SIZE) {
      override fun newInstance(): Contact {
        return ChainAndCircleContact(world)
      }
    }
  override val chainPolyContactStack: IDynamicStack<Contact> =
    object : MutableStack<Contact>(Settings.CONTACT_STACK_INIT_SIZE) {
      override fun newInstance(): Contact {
        return ChainAndPolygonContact(world)
      }
    }
  override val collision: Collision = Collision(this)
  override val timeOfImpact: TimeOfImpact = TimeOfImpact(this)
  override val distance: Distance = Distance()

  private val vecs: OrderedStack<Vec2> =
    object : OrderedStack<Vec2>(argSize, argContainerSize) {
      override fun newInstance(): Vec2 {
        return Vec2()
      }
    }
  private val vec3s: OrderedStack<Vec3> =
    object : OrderedStack<Vec3>(argSize, argContainerSize) {
      override fun newInstance(): Vec3 {
        return Vec3()
      }
    }
  private val mats: OrderedStack<Mat22> =
    object : OrderedStack<Mat22>(argSize, argContainerSize) {
      override fun newInstance(): Mat22 {
        return Mat22()
      }
    }
  private val mat33s: OrderedStack<Mat33> =
    object : OrderedStack<Mat33>(argSize, argContainerSize) {
      override fun newInstance(): Mat33 {
        return Mat33()
      }
    }
  private val aabbs: OrderedStack<AABB> =
    object : OrderedStack<AABB>(argSize, argContainerSize) {
      override fun newInstance(): AABB {
        return AABB()
      }
    }
  private val rots: OrderedStack<Rot> =
    object : OrderedStack<Rot>(argSize, argContainerSize) {
      override fun newInstance(): Rot {
        return Rot()
      }
    }
  private val afloats = mutableMapOf<Int, FloatArray>()
  private val aints = mutableMapOf<Int, IntArray>()
  private val avecs = mutableMapOf<Int, Array<Vec2>>()

  override fun popVec2(): Vec2 = vecs.pop()

  override fun popVec2(num: Int): Array<Vec2> = vecs.pop(num)

  override fun pushVec2(num: Int) {
    vecs.push(num)
  }

  override fun popVec3(): Vec3 = vec3s.pop()

  override fun popVec3(num: Int): Array<Vec3> = vec3s.pop(num)

  override fun pushVec3(num: Int) {
    vec3s.push(num)
  }

  override fun popMat22(): Mat22 = mats.pop()

  override fun popMat22(num: Int): Array<Mat22> = mats.pop(num)

  override fun pushMat22(num: Int) {
    mats.push(num)
  }

  override fun popMat33(): Mat33 = mat33s.pop()

  override fun pushMat33(num: Int) {
    mat33s.push(num)
  }

  override fun popAABB(): AABB = aabbs.pop()

  override fun popAABB(num: Int): Array<AABB> = aabbs.pop(num)

  override fun pushAABB(num: Int) {
    aabbs.push(num)
  }

  override fun popRot(): Rot = rots.pop()

  override fun pushRot(num: Int) {
    rots.push(num)
  }

  override fun getFloatArray(argLength: Int): FloatArray {
    if (!afloats.containsKey(argLength)) {
      afloats[argLength] = FloatArray(argLength)
    }
    // assert is not supported in KMP.
    // assert(afloats[argLength]!!.size == argLength) { "Array not built with correct length" }
    return afloats[argLength]!!
  }

  override fun getIntArray(argLength: Int): IntArray {
    if (!aints.containsKey(argLength)) {
      aints[argLength] = IntArray(argLength)
    }
    // assert is not supported in KMP.
    // assert(aints[argLength]!!.size == argLength) { "Array not built with correct length" }
    return aints[argLength]!!
  }

  override fun getVec2Array(argLength: Int): Array<Vec2> {
    if (!avecs.containsKey(argLength)) {
      val ray = Array<Vec2>(argLength) { Vec2() }
      avecs[argLength] = ray
    }
    // assert is not supported in KMP.
    // assert(avecs[argLength]!!.size == argLength) { "Array not built with correct length" }
    return avecs[argLength]!!
  }
}
