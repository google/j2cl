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
  private val vecs: OrderedStack<Vec2>
  private val vec3s: OrderedStack<Vec3>
  private val mats: OrderedStack<Mat22>
  private val mat33s: OrderedStack<Mat33>
  private val aabbs: OrderedStack<AABB>
  private val rots: OrderedStack<Rot>
  private val afloats = hashMapOf<Int, FloatArray>()
  private val aints = hashMapOf<Int, IntArray>()
  private val avecs = hashMapOf<Int, Array<Vec2>>()
  private val world: IWorldPool = this
  private val pcstack: MutableStack<Contact> =
    object : MutableStack<Contact>(Settings.CONTACT_STACK_INIT_SIZE) {
      override fun newInstance(): Contact {
        return PolygonContact(world)
      }
    }
  private val ccstack: MutableStack<Contact> =
    object : MutableStack<Contact>(Settings.CONTACT_STACK_INIT_SIZE) {
      override fun newInstance(): Contact {
        return CircleContact(world)
      }
    }
  private val cpstack: MutableStack<Contact> =
    object : MutableStack<Contact>(Settings.CONTACT_STACK_INIT_SIZE) {
      override fun newInstance(): Contact {
        return PolygonAndCircleContact(world)
      }
    }
  private val ecstack: MutableStack<Contact> =
    object : MutableStack<Contact>(Settings.CONTACT_STACK_INIT_SIZE) {
      override fun newInstance(): Contact {
        return EdgeAndCircleContact(world)
      }
    }
  private val epstack: MutableStack<Contact> =
    object : MutableStack<Contact>(Settings.CONTACT_STACK_INIT_SIZE) {
      override fun newInstance(): Contact {
        return EdgeAndPolygonContact(world)
      }
    }
  private val chcstack: MutableStack<Contact> =
    object : MutableStack<Contact>(Settings.CONTACT_STACK_INIT_SIZE) {
      override fun newInstance(): Contact {
        return ChainAndCircleContact(world)
      }
    }
  private val chpstack: MutableStack<Contact> =
    object : MutableStack<Contact>(Settings.CONTACT_STACK_INIT_SIZE) {
      override fun newInstance(): Contact {
        return ChainAndPolygonContact(world)
      }
    }

  private val collision: Collision
  private val timeOfImpact: TimeOfImpact
  private val distance: Distance

  init {
    vecs =
      object : OrderedStack<Vec2>(argSize, argContainerSize) {
        override fun newInstance(): Vec2 {
          return Vec2()
        }
      }
    vec3s =
      object : OrderedStack<Vec3>(argSize, argContainerSize) {
        override fun newInstance(): Vec3 {
          return Vec3()
        }
      }
    mats =
      object : OrderedStack<Mat22>(argSize, argContainerSize) {
        override fun newInstance(): Mat22 {
          return Mat22()
        }
      }
    aabbs =
      object : OrderedStack<AABB>(argSize, argContainerSize) {
        override fun newInstance(): AABB {
          return AABB()
        }
      }
    rots =
      object : OrderedStack<Rot>(argSize, argContainerSize) {
        override fun newInstance(): Rot {
          return Rot()
        }
      }
    mat33s =
      object : OrderedStack<Mat33>(argSize, argContainerSize) {
        override fun newInstance(): Mat33 {
          return Mat33()
        }
      }
    distance = Distance()
    collision = Collision(this)
    timeOfImpact = TimeOfImpact(this)
  }

  override fun getPolyContactStack(): IDynamicStack<Contact> {
    return pcstack
  }

  override fun getCircleContactStack(): IDynamicStack<Contact> {
    return ccstack
  }

  override fun getPolyCircleContactStack(): IDynamicStack<Contact> {
    return cpstack
  }

  override fun getEdgeCircleContactStack(): IDynamicStack<Contact> {
    return ecstack
  }

  override fun getEdgePolyContactStack(): IDynamicStack<Contact> {
    return epstack
  }

  override fun getChainCircleContactStack(): IDynamicStack<Contact> {
    return chcstack
  }

  override fun getChainPolyContactStack(): IDynamicStack<Contact> {
    return chpstack
  }

  override fun popVec2(): Vec2 {
    return vecs.pop()
  }

  override fun popVec2(num: Int): Array<Vec2> {
    return vecs.pop(num)
  }

  override fun pushVec2(num: Int) {
    vecs.push(num)
  }

  override fun popVec3(): Vec3 {
    return vec3s.pop()
  }

  override fun popVec3(num: Int): Array<Vec3> {
    return vec3s.pop(num)
  }

  override fun pushVec3(num: Int) {
    vec3s.push(num)
  }

  override fun popMat22(): Mat22 {
    return mats.pop()
  }

  override fun popMat22(num: Int): Array<Mat22> {
    return mats.pop(num)
  }

  override fun pushMat22(num: Int) {
    mats.push(num)
  }

  override fun popMat33(): Mat33 {
    return mat33s.pop()
  }

  override fun pushMat33(num: Int) {
    mat33s.push(num)
  }

  override fun popAABB(): AABB {
    return aabbs.pop()
  }

  override fun popAABB(num: Int): Array<AABB> {
    return aabbs.pop(num)
  }

  override fun pushAABB(num: Int) {
    aabbs.push(num)
  }

  override fun popRot(): Rot {
    return rots.pop()
  }

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

  override fun getCollision(): Collision {
    return collision
  }

  override fun getTimeOfImpact(): TimeOfImpact {
    return timeOfImpact
  }

  override fun getDistance(): Distance {
    return distance
  }
}
